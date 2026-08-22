package site.benepay.domain.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import site.benepay.common.crypto.Encryptor;
import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.KbCustomerNotFoundException;
import site.benepay.common.exception.VerificationCodeInvalidException;
import site.benepay.common.util.RedisKeys;
import site.benepay.common.util.Sha256Util;
import site.benepay.domain.user.dto.SignupIdentityConfirmRequestDto;
import site.benepay.domain.user.dto.SignupIdentityConfirmResponseDto;
import site.benepay.domain.user.dto.SignupIdentityRequestDto;
import site.benepay.domain.user.dto.SignupIdentityRequestResponseDto;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.integration.kbcard.client.KbCardClient;
import site.benepay.integration.kbcard.dto.KbCustomerVerifyResponseDto;

@Slf4j
@Service
public class SignupIdentityServiceImpl implements SignupIdentityService {

	private static final int CODE_LENGTH = 6;
	private static final Duration CODE_TTL = Duration.ofMinutes(3);
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);
	// 아직 userId가 없는 단계라 login/password처럼 계정 단위가 아니라 전화번호 단위로 잠근다.
	private static final Duration REQUEST_LOCK_DURATION = Duration.ofMinutes(10);
	private static final Duration CONFIRM_LOCK_DURATION = Duration.ofMinutes(30);

	private final UserMapper userMapper;
	private final Encryptor encryptor;
	private final SignupVerificationStore signupVerificationStore;
	private final RedisLockoutService redisLockoutService;
	private final StringRedisTemplate redisTemplate;
	private final KbCardClient kbCardClient;
	private final String diHashSalt;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SecureRandom secureRandom = new SecureRandom();

	public SignupIdentityServiceImpl(UserMapper userMapper, Encryptor encryptor,
		SignupVerificationStore signupVerificationStore, RedisLockoutService redisLockoutService,
		StringRedisTemplate redisTemplate, KbCardClient kbCardClient,
		@Value("${security.di-hash-salt}") String diHashSalt) {
		this.userMapper = userMapper;
		this.encryptor = encryptor;
		this.signupVerificationStore = signupVerificationStore;
		this.redisLockoutService = redisLockoutService;
		this.redisTemplate = redisTemplate;
		this.kbCardClient = kbCardClient;
		this.diHashSalt = diHashSalt;
	}

	@Override
	public SignupIdentityRequestResponseDto requestVerification(SignupIdentityRequestDto request) {
		String phoneNumber = request.getPhoneNumber();
		String failureKey = RedisKeys.signupIdentityFailure(phoneNumber);
		String lockKey = RedisKeys.signupIdentityLock(phoneNumber);

		if (redisLockoutService.isLocked(lockKey)) {
			throw new AccountLockedException("verification requests for this phone number are temporarily locked");
		}

		// ciHash = SHA-256(name+birthDate+phoneNumber)가 정본이다 - KB Mock Server의 카드
		// 시드 데이터도 같은 조합으로 ci_hash를 채우도록 맞추기로 했으므로, 카드 자동연동
		// (KbCardClient.findCardsByCiHash) 매칭 키로도 그대로 쓸 수 있다.
		String ciHash = buildCiHash(request.getName(), request.getBirthDate(), phoneNumber);
		if (userMapper.existsByCiHash(ciHash)) {
			redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, MAX_ATTEMPTS, FAILURE_WINDOW,
				REQUEST_LOCK_DURATION);
			throw new DuplicateUserException("identity already registered");
		}

		KbCustomerVerifyResponseDto kbCustomer = kbCardClient.verifyCustomer(ciHash);
		if (!kbCustomer.isRegistered()) {
			// (테스트용) 이 프로젝트는 실제 본인인증기관/카드사 연동이 없는 목데이터 전용
			// 서비스라, KB Mock Server가 등록된 회원을 못 찾으면 그 자리에서 새 고객+카드를
			// 만들어 회원가입을 계속 진행한다 - 누구나 임의의 이름/생년월일/휴대폰번호로
			// 회원가입부터 카드 자동연동까지 끝까지 테스트할 수 있어야 하기 때문이다.
			kbCustomer = kbCardClient.registerCustomer(ciHash);
			if (!kbCustomer.isRegistered()) {
				redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, MAX_ATTEMPTS, FAILURE_WINDOW,
					REQUEST_LOCK_DURATION);
				throw new KbCustomerNotFoundException("KB에 등록된 회원이 아닙니다.");
			}
		}

		redisLockoutService.clearFailuresAndLock(failureKey, lockKey);

		String code = generateCode();
		PendingOtp pending = new PendingOtp(code, request.getName(), request.getBirthDate(), phoneNumber, ciHash,
			kbCustomer.getCustomerReferenceId());
		savePendingOtp(phoneNumber, pending);

		log.info("Signup verification code issued for phoneNumber={}", phoneNumber);

		// (테스트용) 실제 SMS 게이트웨이가 없는 목데이터 전용 서비스라, 인증번호를 응답에
		// 그대로 실어 준다 - 그래야 실제 문자 없이도 회원가입 2단계(인증 확인)를 테스트할 수 있다.
		return new SignupIdentityRequestResponseDto(code);
	}

	@Override
	public SignupIdentityConfirmResponseDto confirmVerification(SignupIdentityConfirmRequestDto request) {
		String phoneNumber = request.getPhoneNumber();
		String failureKey = RedisKeys.signupOtpFailure(phoneNumber);
		String lockKey = RedisKeys.signupOtpLock(phoneNumber);

		if (redisLockoutService.isLocked(lockKey)) {
			throw new AccountLockedException("verification code confirmation is temporarily locked");
		}

		PendingOtp pending = loadPendingOtp(phoneNumber)
			.orElseThrow(() -> new VerificationCodeInvalidException("verification code is invalid or expired"));

		if (!pending.code.equals(request.getCode())) {
			redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, MAX_ATTEMPTS, FAILURE_WINDOW,
				CONFIRM_LOCK_DURATION);
			throw new VerificationCodeInvalidException("verification code is invalid or expired");
		}

		redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
		// 1회용 - 검증에 성공했든 다음 redeem에서 쓰든, 이 상태는 여기서 끝난다.
		redisTemplate.delete(RedisKeys.signupOtp(phoneNumber));

		// requestVerification에서 이미 계산해 둔 값을 그대로 쓴다 - 여기서 다시 계산하면
		// (이론적으로는 같은 값이 나와야 하지만) 검증 통과 시점과 계산 시점이 어긋날 여지를 만든다.
		String ciEncrypted = encryptor.encrypt(pending.name);
		// DI는 이제 PortOne impUid가 아니라 KB Mock Server가 내려준 실제 고객 식별자를 시드로
		// 쓴다 - PortOne 없이도 "이 사람과 KB 계정을 유일하게 잇는 값"이라는 DI의 의미가 유지된다.
		String diHash = Sha256Util.hashWithSalt(pending.kbCustomerReferenceId, diHashSalt);

		SignupVerificationStore.VerifiedIdentity identity = new SignupVerificationStore.VerifiedIdentity(
			pending.name, pending.phoneNumber, pending.birthDate, pending.ciHash, ciEncrypted, diHash);
		String token = signupVerificationStore.issue(identity);

		return new SignupIdentityConfirmResponseDto(token);
	}

	// KB Mock Server의 카드 시드 데이터와 매칭돼야 하므로(카드 자동연동, KbCardClient.
	// findCardsByCiHash) 이 조합 공식은 임의로 바꾸면 안 된다 - name+birthDate+phoneNumber가 정본.
	private static String buildCiHash(String name, String birthDate, String phoneNumber) {
		return Sha256Util.hash(name + birthDate + phoneNumber);
	}

	private String generateCode() {
		int value = secureRandom.nextInt((int)Math.pow(10, CODE_LENGTH));
		return String.format("%0" + CODE_LENGTH + "d", value);
	}

	private void savePendingOtp(String phoneNumber, PendingOtp pending) {
		try {
			String json = objectMapper.writeValueAsString(pending);
			redisTemplate.opsForValue().set(RedisKeys.signupOtp(phoneNumber), json, CODE_TTL);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to serialize pending signup otp", e);
		}
	}

	private Optional<PendingOtp> loadPendingOtp(String phoneNumber) {
		String raw = redisTemplate.opsForValue().get(RedisKeys.signupOtp(phoneNumber));
		if (raw == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(raw, PendingOtp.class));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to deserialize pending signup otp", e);
		}
	}

	/** 인증번호 발송~검증 사이(최대 CODE_TTL)만 Redis에 살아있는 임시 상태. */
	static class PendingOtp {
		public String code;
		public String name;
		public String birthDate;
		public String phoneNumber;
		public String ciHash;
		public String kbCustomerReferenceId;

		public PendingOtp() {
		}

		public PendingOtp(String code, String name, String birthDate, String phoneNumber, String ciHash,
			String kbCustomerReferenceId) {
			this.code = code;
			this.name = name;
			this.birthDate = birthDate;
			this.phoneNumber = phoneNumber;
			this.ciHash = ciHash;
			this.kbCustomerReferenceId = kbCustomerReferenceId;
		}
	}
}
