package site.benepay.auth.domain.user.controller;

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

import site.benepay.auth.domain.user.dto.RegisterPinRequestDto;
import site.benepay.auth.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.auth.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.auth.domain.user.dto.UserResponseDto;
import site.benepay.auth.domain.user.service.UserService;

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
