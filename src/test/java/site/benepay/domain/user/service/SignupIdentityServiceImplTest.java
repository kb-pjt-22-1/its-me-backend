package site.benepay.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import site.benepay.common.crypto.Encryptor;
import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.KbCustomerNotFoundException;
import site.benepay.common.exception.VerificationCodeInvalidException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.SignupIdentityConfirmRequestDto;
import site.benepay.domain.user.dto.SignupIdentityConfirmResponseDto;
import site.benepay.domain.user.dto.SignupIdentityRequestDto;
import site.benepay.domain.user.dto.SignupIdentityRequestResponseDto;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.integration.kbcard.client.KbCardClient;
import site.benepay.integration.kbcard.dto.KbCustomerVerifyResponseDto;

@ExtendWith(MockitoExtension.class)
class SignupIdentityServiceImplTest {

	private static final String DI_HASH_SALT = "unit-test-salt";
	private static final String NAME = "홍길동";
	private static final String BIRTH_DATE = "19900101";
	private static final String PHONE_NUMBER = "010-1111-2222";
	// ciHash = SHA-256(name+birthDate+phoneNumber)가 정본이다 - KB Mock Server의 카드 시드
	// 데이터도 같은 조합으로 ci_hash를 채우기로 했으므로, 이 조합 공식 자체가 테스트 대상이다.
	private static final String EXPECTED_CI_HASH =
		site.benepay.common.util.Sha256Util.hash(NAME + BIRTH_DATE + PHONE_NUMBER);

	@Mock
	private UserMapper userMapper;

	@Mock
	private Encryptor encryptor;

	@Mock
	private SignupVerificationStore signupVerificationStore;

	@Mock
	private RedisLockoutService redisLockoutService;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private KbCardClient kbCardClient;

	private SignupIdentityServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new SignupIdentityServiceImpl(userMapper, encryptor, signupVerificationStore, redisLockoutService,
			redisTemplate, kbCardClient, DI_HASH_SALT, false);
	}

	private SignupIdentityRequestDto identityRequest() {
		return new SignupIdentityRequestDto(NAME, BIRTH_DATE, PHONE_NUMBER);
	}

	private KbCustomerVerifyResponseDto registeredKbCustomer() {
		KbCustomerVerifyResponseDto dto = new KbCustomerVerifyResponseDto();
		dto.setRegistered(true);
		dto.setCustomerReferenceId("kb-customer-001");
		return dto;
	}

	// ---- requestVerification ----

	@Test
	void requestVerificationFailsFastWhenLockedWithoutTouchingTheDatabaseOrKb() {
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(true);

		assertThatThrownBy(() -> service.requestVerification(identityRequest()))
			.isInstanceOf(AccountLockedException.class);

		verify(userMapper, never()).existsByCiHash(any());
		verify(kbCardClient, never()).verifyCustomer(any());
	}

	@Test
	void requestVerificationRejectsWhenAlreadyRegisteredInternallyAndNeverCallsKb() {
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(true);

		assertThatThrownBy(() -> service.requestVerification(identityRequest()))
			.isInstanceOf(DuplicateUserException.class);

		verify(kbCardClient, never()).verifyCustomer(any());
		verify(redisLockoutService).recordFailureAndMaybeLock(
			RedisKeys.signupIdentityFailure(PHONE_NUMBER), RedisKeys.signupIdentityLock(PHONE_NUMBER), 5,
			Duration.ofMinutes(10), Duration.ofMinutes(10));
	}

	@Test
	void requestVerificationRejectsWhenNotFoundAsAKbCustomer() {
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		KbCustomerVerifyResponseDto notRegistered = new KbCustomerVerifyResponseDto();
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(notRegistered);

		assertThatThrownBy(() -> service.requestVerification(identityRequest()))
			.isInstanceOf(KbCustomerNotFoundException.class);

		verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
		// dev-login이 꺼져 있으면(운영 환경 기본값) Mock Server에 새 고객을 만들어달라고
		// 요청하지 않는다 - 이 편의 기능은 devLoginEnabled일 때만 켜진다.
		verify(kbCardClient, never()).registerCustomer(any());
	}

	// (테스트용) 실제 본인인증기관 연동이 없는 로컬/테스트 환경에서는, 미리 심어둔 시드
	// 신원이 아니어도 회원가입을 끝까지 테스트할 수 있어야 한다 - Mock Server에 등록된
	// 회원이 아니면 devLoginEnabled일 때만 그 자리에서 새 고객을 만들어 계속 진행한다.
	@Test
	void requestVerificationAutoRegistersUnknownKbCustomerWhenDevLoginIsEnabled() {
		SignupIdentityServiceImpl devService = new SignupIdentityServiceImpl(userMapper, encryptor,
			signupVerificationStore, redisLockoutService, redisTemplate, kbCardClient, DI_HASH_SALT, true);
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(new KbCustomerVerifyResponseDto());
		when(kbCardClient.registerCustomer(EXPECTED_CI_HASH)).thenReturn(registeredKbCustomer());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		SignupIdentityRequestResponseDto response = devService.requestVerification(identityRequest());

		assertThat(response).isNotNull();
		verify(kbCardClient).registerCustomer(EXPECTED_CI_HASH);
		verify(redisLockoutService).clearFailuresAndLock(
			RedisKeys.signupIdentityFailure(PHONE_NUMBER), RedisKeys.signupIdentityLock(PHONE_NUMBER));
	}

	@Test
	void requestVerificationStillRejectsWhenDevLoginEnabledButMockServerCannotRegisterEither() {
		SignupIdentityServiceImpl devService = new SignupIdentityServiceImpl(userMapper, encryptor,
			signupVerificationStore, redisLockoutService, redisTemplate, kbCardClient, DI_HASH_SALT, true);
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(new KbCustomerVerifyResponseDto());
		when(kbCardClient.registerCustomer(EXPECTED_CI_HASH)).thenReturn(new KbCustomerVerifyResponseDto());

		assertThatThrownBy(() -> devService.requestVerification(identityRequest()))
			.isInstanceOf(KbCustomerNotFoundException.class);
	}

	@Test
	void requestVerificationSavesTheCodeWithATtlAndClearsFailuresOnSuccess() {
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(registeredKbCustomer());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		service.requestVerification(identityRequest());

		verify(redisLockoutService).clearFailuresAndLock(
			RedisKeys.signupIdentityFailure(PHONE_NUMBER), RedisKeys.signupIdentityLock(PHONE_NUMBER));
		verify(valueOperations).set(eq(RedisKeys.signupOtp(PHONE_NUMBER)), anyString(), eq(Duration.ofMinutes(3)));
	}

	@Test
	void requestVerificationNeverExposesTheCodeWhenDevLoginIsDisabled() {
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(registeredKbCustomer());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		SignupIdentityRequestResponseDto response = service.requestVerification(identityRequest());

		assertThat(response.getDevVerificationCode()).isNull();
	}

	@Test
	void requestVerificationExposesTheCodeWhenDevLoginIsEnabled() {
		SignupIdentityServiceImpl devService = new SignupIdentityServiceImpl(userMapper, encryptor,
			signupVerificationStore, redisLockoutService, redisTemplate, kbCardClient, DI_HASH_SALT, true);
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(registeredKbCustomer());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		SignupIdentityRequestResponseDto response = devService.requestVerification(identityRequest());

		assertThat(response.getDevVerificationCode()).matches("\\d{6}");
	}

	// ---- confirmVerification ----

	@Test
	void confirmVerificationFailsFastWhenLocked() {
		when(redisLockoutService.isLocked(RedisKeys.signupOtpLock(PHONE_NUMBER))).thenReturn(true);

		assertThatThrownBy(() -> service.confirmVerification(new SignupIdentityConfirmRequestDto(PHONE_NUMBER, "123456")))
			.isInstanceOf(AccountLockedException.class);
	}

	@Test
	void confirmVerificationThrowsWhenNoCodeWasEverRequested() {
		when(redisLockoutService.isLocked(RedisKeys.signupOtpLock(PHONE_NUMBER))).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(RedisKeys.signupOtp(PHONE_NUMBER))).thenReturn(null);

		assertThatThrownBy(() -> service.confirmVerification(new SignupIdentityConfirmRequestDto(PHONE_NUMBER, "123456")))
			.isInstanceOf(VerificationCodeInvalidException.class);
	}

	@Test
	void confirmVerificationWithWrongCodeRecordsFailureAndNeverIssuesAToken() {
		String issuedCode = requestAndCaptureCode();

		assertThatThrownBy(() -> service.confirmVerification(
			new SignupIdentityConfirmRequestDto(PHONE_NUMBER, wrongCode(issuedCode))))
			.isInstanceOf(VerificationCodeInvalidException.class);

		verify(redisLockoutService).recordFailureAndMaybeLock(
			RedisKeys.signupOtpFailure(PHONE_NUMBER), RedisKeys.signupOtpLock(PHONE_NUMBER), 5,
			Duration.ofMinutes(10), Duration.ofMinutes(30));
		verify(signupVerificationStore, never()).issue(any());
	}

	@Test
	void confirmVerificationWithCorrectCodeIssuesAVerificationTokenAndDeletesTheCode() {
		String issuedCode = requestAndCaptureCode();
		when(encryptor.encrypt(NAME)).thenReturn("encrypted-ci");
		when(signupVerificationStore.issue(any())).thenReturn("verification-token");

		SignupIdentityConfirmResponseDto response = service.confirmVerification(
			new SignupIdentityConfirmRequestDto(PHONE_NUMBER, issuedCode));

		assertThat(response.getVerificationToken()).isEqualTo("verification-token");
		verify(redisLockoutService).clearFailuresAndLock(
			RedisKeys.signupOtpFailure(PHONE_NUMBER), RedisKeys.signupOtpLock(PHONE_NUMBER));
		verify(redisTemplate).delete(RedisKeys.signupOtp(PHONE_NUMBER));
	}

	@Test
	void confirmVerificationBuildsTheVerifiedIdentityFromThePendingRequest() {
		String issuedCode = requestAndCaptureCode();
		when(encryptor.encrypt(NAME)).thenReturn("encrypted-ci");
		when(signupVerificationStore.issue(any())).thenReturn("verification-token");

		service.confirmVerification(new SignupIdentityConfirmRequestDto(PHONE_NUMBER, issuedCode));

		ArgumentCaptor<SignupVerificationStore.VerifiedIdentity> captor =
			ArgumentCaptor.forClass(SignupVerificationStore.VerifiedIdentity.class);
		verify(signupVerificationStore).issue(captor.capture());

		SignupVerificationStore.VerifiedIdentity identity = captor.getValue();
		assertThat(identity.name).isEqualTo(NAME);
		assertThat(identity.phoneNumber).isEqualTo(PHONE_NUMBER);
		assertThat(identity.birthDate).isEqualTo(BIRTH_DATE);
		// ciHash는 requestVerification에서 이미 계산해 둔 값(name+birthDate+phoneNumber 조합)을
		// confirm 시점에 그대로 재사용해야 한다 - 카드 자동연동 매칭 키가 이 값과 일치해야 한다.
		assertThat(identity.ciHash).isEqualTo(EXPECTED_CI_HASH);
		assertThat(identity.ciEncrypted).isEqualTo("encrypted-ci");
		// diHash는 이제 impUid가 아니라 KB Mock Server가 내려준 고객 식별자를 시드로 만든다.
		assertThat(identity.diHash)
			.isEqualTo(site.benepay.common.util.Sha256Util.hashWithSalt("kb-customer-001", DI_HASH_SALT));
	}

	/**
	 * requestVerification()을 실행해 Redis에 저장될 JSON을 가로챈 뒤, 같은 Mock 세션 안에서
	 * confirmVerification()이 그 값을 그대로 읽도록 연결해 준다(실제 Redis get/set 왕복을
	 * SignupVerificationStoreTest.capturedTokenAfterIssuing와 동일한 방식으로 흉내낸다).
	 * 반환값은 실제로 저장된 6자리 인증번호 - 테스트가 이걸 몰라도 되게 하려고 캡처해서 돌려준다.
	 */
	private String requestAndCaptureCode() {
		when(redisLockoutService.isLocked(RedisKeys.signupIdentityLock(PHONE_NUMBER))).thenReturn(false);
		when(userMapper.existsByCiHash(EXPECTED_CI_HASH)).thenReturn(false);
		when(kbCardClient.verifyCustomer(EXPECTED_CI_HASH)).thenReturn(registeredKbCustomer());
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		String[] savedJson = new String[1];
		doAnswer(invocation -> {
			savedJson[0] = invocation.getArgument(1);
			return null;
		}).when(valueOperations).set(eq(RedisKeys.signupOtp(PHONE_NUMBER)), anyString(), any(Duration.class));

		SignupIdentityServiceImpl devService = new SignupIdentityServiceImpl(userMapper, encryptor,
			signupVerificationStore, redisLockoutService, redisTemplate, kbCardClient, DI_HASH_SALT, true);
		SignupIdentityRequestResponseDto response = devService.requestVerification(identityRequest());
		when(valueOperations.get(RedisKeys.signupOtp(PHONE_NUMBER))).thenReturn(savedJson[0]);

		when(redisLockoutService.isLocked(RedisKeys.signupOtpLock(PHONE_NUMBER))).thenReturn(false);

		return response.getDevVerificationCode();
	}

	private String wrongCode(String issuedCode) {
		int asNumber = Integer.parseInt(issuedCode);
		int wrong = (asNumber + 1) % 1_000_000;
		return String.format("%06d", wrong);
	}
}
