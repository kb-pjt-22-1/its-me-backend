package site.benepay.domain.user.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.benepay.auth.security.jwt.JwtProperties;
import site.benepay.common.event.UserSignedUpEvent;
import site.benepay.common.exception.AccountLockedException;
import site.benepay.common.exception.DuplicateUserException;
import site.benepay.common.exception.InvalidCredentialsException;
import site.benepay.common.exception.InvalidTokenException;
import site.benepay.common.exception.PinAlreadyRegisteredException;
import site.benepay.common.exception.UserNotFoundException;
import site.benepay.common.exception.WithdrawalNotConfirmedException;
import site.benepay.common.util.RedisKeys;
import site.benepay.domain.user.dto.ChangePasswordRequestDto;
import site.benepay.domain.user.dto.LoginResponseDto;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.TokenPairDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateFcmTokenRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.dto.VerifyPasswordRequestDto;
import site.benepay.domain.user.dto.VerifyPinRequestDto;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.validator.PinValidator;
import site.benepay.domain.user.vo.Role;
import site.benepay.domain.user.vo.User;

@Service
public class UserServiceImpl implements UserService {

	// DB 커넥션의 serverTimezone(application.properties의 db.url)과 맞춘다 - 여기서 채우는
	// createdAt이 DEFAULT CURRENT_TIMESTAMP가 실제로 넣었을 값과 어긋나면 안 된다.
	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final RedisLockoutService redisLockoutService;
	private final TokenService tokenService;
	private final SignupVerificationStore signupVerificationStore;
	private final ApplicationEventPublisher eventPublisher;
	private final JwtProperties jwtProperties;

	public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
		RedisLockoutService redisLockoutService, TokenService tokenService,
		SignupVerificationStore signupVerificationStore, ApplicationEventPublisher eventPublisher,
		JwtProperties jwtProperties) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.redisLockoutService = redisLockoutService;
		this.tokenService = tokenService;
		this.signupVerificationStore = signupVerificationStore;
		this.eventPublisher = eventPublisher;
		this.jwtProperties = jwtProperties;
	}

	@Override
	@Transactional
	public LoginResponseDto signUp(SignUpRequestDto request) {
		if (userMapper.existsByLoginId(request.getLoginId())) {
			throw new DuplicateUserException("login id already in use: " + request.getLoginId());
		}

		SignupVerificationStore.VerifiedIdentity identity = signupVerificationStore
			.redeem(request.getVerificationToken())
			.orElseThrow(() -> new InvalidTokenException("identity verification token is invalid or expired"));

		// 휴대폰 본인인증 시점에 이미 한 번 걸렀지만, 그 사이 다른 요청이 같은 DI로 먼저
		// 가입했을 수 있어 여기서 한 번 더 확인한다. 최종 방어선은 어차피 users.di UNIQUE다.
		if (userMapper.existsByDiHash(identity.diHash)) {
			throw new DuplicateUserException("identity already registered");
		}
		// ci_hash도 di와 마찬가지로 users 테이블에 UNIQUE라 최종 방어선은 DB가 지키지만,
		// 인증은 됐는데 이미 가입된 사람인 경우를 여기서 먼저 걸러 더 명확한 예외로 알려준다.
		if (userMapper.existsByCiHash(identity.ciHash)) {
			throw new DuplicateUserException("identity already registered");
		}

		PinValidator.validate(request.getPin());

		User user = User.builder()
			.loginId(request.getLoginId())
			.loginPasswordHash(passwordEncoder.encode(request.getPassword()))
			.pinHash(passwordEncoder.encode(request.getPin()))
			.name(identity.name)
			.phoneNumber(identity.phoneNumber)
			.birthDate(identity.birthDate)
			.role(Role.USER)
			.di(identity.diHash)
			.ciHash(identity.ciHash)
			.ciEncrypted(identity.ciEncrypted)
			// 컬럼에 DEFAULT CURRENT_TIMESTAMP가 붙었지만, insert 후 재조회하지 않고
			// 이 객체를 그대로 응답으로 내보내므로 값을 직접 채워 둔다.
			.createdAt(LocalDateTime.now(APP_ZONE))
			.deleted(false)
			.fcmToken(request.getFcmToken())
			.build();

		userMapper.insert(user);

		// 이 시점에는 userId가 발급됐지만 아직 회원가입 트랜잭션 안이다.
		// 실제 카드 동기화는 UserSignedUpCardSyncHandler의 AFTER_COMMIT에서 실행된다.
		// 따라서 KB Mock Server가 중단돼도 users INSERT는 정상적으로 COMMIT된다.
		eventPublisher.publishEvent(
			new UserSignedUpEvent(user.getUserId(), user.getCiHash())
		);

		// 가입 성공 시 바로 토큰을 발급한다(자동 로그인) - 프론트가 재로그인 없이 홈으로
		// 이동할 수 있게 하기 위함. login과 동일한 발급 로직(TokenService.issueTokenPair)이다.
		TokenPairDto tokens = tokenService.issueTokenPair(user);
		return LoginResponseDto.of(user, tokens, jwtProperties.getAccessTokenExpirationMillis() / 1000);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDto getMyProfile(Long userId) {
		User user = findActiveUser(userId);
		return UserResponseDto.from(user);
	}

	@Override
	@Transactional
	public UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request) {
		findActiveUser(userId);
		userMapper.updateProfile(userId, request.getPhoneNumber());
		// getMyProfile()을 this로 직접 부르면 프록시를 안 거쳐서 그 메서드의 @Transactional이
		// 적용되지 않는다(Spring AOP self-invocation 문제) - 여기서는 이미 이 메서드 자체가
		// 트랜잭션 안이라 결과는 같지만, 아예 self-invocation이 없도록 조회를 직접 한다.
		User updatedUser = findActiveUser(userId);
		return UserResponseDto.from(updatedUser);
	}

	@Override
	@Transactional
	public void updateFcmToken(Long userId, UpdateFcmTokenRequestDto request) {
		findActiveUser(userId);
		userMapper.updateFcmToken(userId, request.getFcmToken());
	}

	@Override
	@Transactional(readOnly = true)
	public void verifyPassword(Long userId, VerifyPasswordRequestDto request) {
		// 실패하면 verifyCurrentPasswordOrThrow가 예외를 던진다 - 성공은 반환값 자체가 증거라
		// 여기서 더 할 일이 없다. 개인정보 수정 페이지 진입 전 재인증 게이트 용도.
		verifyCurrentPasswordOrThrow(userId, request.getPassword());
	}

	@Override
	@Transactional
	public void changePassword(Long userId, ChangePasswordRequestDto request) {
		verifyCurrentPasswordOrThrow(userId, request.getCurrentPassword());

		userMapper.updatePasswordHash(userId, passwordEncoder.encode(request.getNewPassword()));
		// 비밀번호가 바뀌면 이전 비밀번호로 발급된 세션은 더 이상 유효할 이유가 없다 -
		// 다른 곳에서 이미 로그인돼 있던 사람이 그대로 남아있게 두지 않는다.
		tokenService.revokeRefreshToken(userId);
	}

	// verifyPassword(재인증 게이트)와 changePassword가 "현재 비밀번호가 맞는지" 확인하는
	// 로직과 잠금 카운터를 그대로 공유한다 - 둘 다 막으려는 위협이 같다(비밀번호 무차별 대입).
	private User verifyCurrentPasswordOrThrow(Long userId, String currentPassword) {
		String failureKey = RedisKeys.passwordFailure(userId);
		String lockKey = RedisKeys.passwordLock(userId);

		if (redisLockoutService.isLocked(lockKey)) {
			throw new AccountLockedException("password verification is temporarily locked");
		}

		User user = findActiveUser(userId);
		if (!passwordEncoder.matches(currentPassword, user.getLoginPasswordHash())) {
			redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, 5, Duration.ofMinutes(10),
				Duration.ofMinutes(30));
			throw new InvalidCredentialsException("current password is incorrect");
		}
		redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
		return user;
	}

	@Override
	@Transactional
	public void registerPin(Long userId, RegisterPinRequestDto request) {
		User user = findActiveUser(userId);
		if (user.getPinHash() != null) {
			throw new PinAlreadyRegisteredException("PIN already registered; use the update endpoint instead");
		}
		PinValidator.validate(request.getPin());
		userMapper.updatePinHash(userId, passwordEncoder.encode(request.getPin()));
	}

	@Override
	@Transactional
	public void updateOrDeletePin(Long userId, UpdateDeletePinRequestDto request) {
		verifyCurrentPinOrThrow(userId, request.getCurrentPin());

		if (request.getNewPin() == null) {
			userMapper.updatePinHash(userId, null);
		} else {
			PinValidator.validate(request.getNewPin());
			userMapper.updatePinHash(userId, passwordEncoder.encode(request.getNewPin()));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void verifyPin(Long userId, VerifyPinRequestDto request) {
		// 결제 화면에서 결제 수단을 노출하기 전에 거치는 게이트. 성공은 반환값 자체가
		// 증거라(예외 없이 끝남) 여기서 더 할 일이 없다 - verifyPassword와 동일한 패턴.
		verifyCurrentPinOrThrow(userId, request.getPin());
	}

	// updateOrDeletePin(PIN 변경 전 현재 PIN 확인)과 verifyPin(결제 전 PIN 확인)이 "PIN이
	// 맞는지" 확인하는 로직과 잠금 카운터를 그대로 공유한다 - 둘 다 막으려는 위협이 같다
	// (PIN 무차별 대입). verifyPassword/changePassword가 같은 이유로 카운터를 공유하는 것과 동일하다.
	private void verifyCurrentPinOrThrow(Long userId, String currentPin) {
		String failureKey = RedisKeys.pinFailure(userId);
		String lockKey = RedisKeys.pinLock(userId);

		if (redisLockoutService.isLocked(lockKey)) {
			throw new AccountLockedException("PIN verification is temporarily locked");
		}

		User user = findActiveUser(userId);
		if (user.getPinHash() == null || !passwordEncoder.matches(currentPin, user.getPinHash())) {
			redisLockoutService.recordFailureAndMaybeLock(failureKey, lockKey, 5, Duration.ofMinutes(10),
				Duration.ofSeconds(30));
			throw new InvalidCredentialsException("PIN is incorrect");
		}
		redisLockoutService.clearFailuresAndLock(failureKey, lockKey);
	}

	@Override
	@Transactional
	public void withdraw(Long userId, boolean confirmed) {
		if (!confirmed) {
			throw new WithdrawalNotConfirmedException("withdrawal confirmation flag is required");
		}
		findActiveUser(userId);
		userMapper.softDeleteAndAnonymize(userId);
		tokenService.revokeRefreshToken(userId);
	}

	private User findActiveUser(Long userId) {
		return userMapper.findByUserId(userId)
			.orElseThrow(() -> new UserNotFoundException("user not found"));
	}
}
