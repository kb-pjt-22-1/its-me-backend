package site.benepay.domain.user.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.domain.user.dto.ChangePasswordRequestDto;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.dto.VerifyPasswordRequestDto;
import site.benepay.domain.user.dto.VerifyPinRequestDto;
import site.benepay.domain.user.service.UserService;

@RestController
@RequestMapping("/api/users/me")
public class MemberController {

	private final UserService userService;

	public MemberController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<UserResponseDto> getMyProfile() {
		return ResponseEntity.ok(userService.getMyProfile(currentUserId()));
	}

	@PutMapping
	public ResponseEntity<UserResponseDto> updateMyProfile(@Valid @RequestBody UpdateProfileRequestDto request) {
		return ResponseEntity.ok(userService.updateProfile(currentUserId(), request));
	}

	@PostMapping("/verify-password")
	public ResponseEntity<Void> verifyPassword(@Valid @RequestBody VerifyPasswordRequestDto request) {
		userService.verifyPassword(currentUserId(), request);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
		userService.changePassword(currentUserId(), request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/pin")
	public ResponseEntity<Void> registerPin(@Valid @RequestBody RegisterPinRequestDto request) {
		userService.registerPin(currentUserId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PutMapping("/pin")
	public ResponseEntity<Void> updateOrDeletePin(@Valid @RequestBody UpdateDeletePinRequestDto request) {
		userService.updateOrDeletePin(currentUserId(), request);
		return ResponseEntity.noContent().build();
	}

	// 결제 화면에서 간편 비밀번호 인증 게이트로 쓴다 - verify-password와 같은 용도, 대상만 PIN.
	@PostMapping("/verify-pin")
	public ResponseEntity<Void> verifyPin(@Valid @RequestBody VerifyPinRequestDto request) {
		userService.verifyPin(currentUserId(), request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	public ResponseEntity<Void> withdraw(@RequestParam(defaultValue = "false") boolean confirmed) {
		userService.withdraw(currentUserId(), confirmed);
		return ResponseEntity.noContent().build();
	}

	private Long currentUserId() {
		return (Long)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
}
