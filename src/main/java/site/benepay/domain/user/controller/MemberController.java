package site.benepay.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import site.benepay.domain.user.dto.ChangePasswordRequestDto;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.dto.VerifyPasswordRequestDto;
import site.benepay.domain.user.service.UserService;

import javax.validation.Valid;

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

    @DeleteMapping
    public ResponseEntity<Void> withdraw(@RequestParam(defaultValue = "false") boolean confirmed) {
        userService.withdraw(currentUserId(), confirmed);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
