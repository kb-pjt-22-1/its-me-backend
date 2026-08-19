package site.benepay.domain.user.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.common.util.TokenExtractor;
import site.benepay.domain.user.dto.DevLoginRequestDto;
import site.benepay.domain.user.dto.LoginRequestDto;
import site.benepay.domain.user.dto.LoginResponseDto;
import site.benepay.domain.user.dto.RefreshRequestDto;
import site.benepay.domain.user.dto.RefreshResponseDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.SignupIdentityConfirmRequestDto;
import site.benepay.domain.user.dto.SignupIdentityConfirmResponseDto;
import site.benepay.domain.user.dto.SignupIdentityRequestDto;
import site.benepay.domain.user.dto.SignupIdentityRequestResponseDto;
import site.benepay.domain.user.service.AuthService;
import site.benepay.domain.user.service.SignupIdentityService;
import site.benepay.domain.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;
	private final AuthService authService;
	private final SignupIdentityService signupIdentityService;

	public AuthController(UserService userService, AuthService authService,
		SignupIdentityService signupIdentityService) {
		this.userService = userService;
		this.authService = authService;
		this.signupIdentityService = signupIdentityService;
	}

	/**
	 * 회원가입 1단계: 휴대폰 본인인증 - 인증번호 발송. 이름+생년월일+휴대폰번호로 내부 중복
	 * 가입 여부와 KB Mock Server 실명 회원 여부를 확인한 뒤에만 인증번호를 발급한다.
	 */
	@PostMapping("/signup/identity")
	public ResponseEntity<SignupIdentityRequestResponseDto> requestSignupIdentity(
		@Valid @RequestBody SignupIdentityRequestDto request) {
		return ResponseEntity.ok(signupIdentityService.requestVerification(request));
	}

	/**
	 * 회원가입 1단계: 인증번호 검증. 성공하면 검증된 신원 정보를 가리키는 1회용
	 * verificationToken을 돌려준다 - /signup 호출 시 그대로 다시 보내면 된다.
	 */
	@PostMapping("/signup/identity/confirm")
	public ResponseEntity<SignupIdentityConfirmResponseDto> confirmSignupIdentity(
		@Valid @RequestBody SignupIdentityConfirmRequestDto request) {
		return ResponseEntity.ok(signupIdentityService.confirmVerification(request));
	}

	/**
	 * 회원가입 2~3단계(아이디/비밀번호/PIN) 최종 제출. 성공 시 토큰을 즉시 발급한다(자동
	 * 로그인) - 프론트는 재로그인 없이 바로 홈으로 이동하면 된다. 4단계(KB 카드 자동 연동)는
	 * UserService.signUp() 내부에서 발행하는 이벤트로 백그라운드에서 이어서 처리된다.
	 */
	@PostMapping("/signup")
	public ResponseEntity<LoginResponseDto> signUp(@Valid @RequestBody SignUpRequestDto request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.signUp(request));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/**
	 * 개발용 자동 로그인. dev-login.enabled가 꺼져 있으면 404를 돌려준다.
	 * 프론트의 '개발자 로그인' 버튼이 컴퓨터별로 다른 slot을 보내야 한다.
	 */
	@PostMapping("/dev-login")
	public ResponseEntity<LoginResponseDto> devLogin(@RequestBody(required = false) DevLoginRequestDto request) {
		return ResponseEntity.ok(authService.devLogin(
			request == null ? new DevLoginRequestDto() : request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<RefreshResponseDto> refresh(@Valid @RequestBody RefreshRequestDto request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
		Long userId = (Long)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String accessToken = TokenExtractor.extractBearerToken(servletRequest);
		authService.logout(accessToken, userId);
		return ResponseEntity.noContent().build();
	}
}
