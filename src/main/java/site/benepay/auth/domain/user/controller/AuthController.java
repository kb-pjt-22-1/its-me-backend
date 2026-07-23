package site.benepay.auth.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.benepay.auth.common.util.TokenExtractor;
import site.benepay.auth.domain.user.dto.*;
import site.benepay.auth.domain.user.service.AuthService;
import site.benepay.auth.domain.user.service.PortOneService;
import site.benepay.auth.domain.user.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final PortOneService portOneService;

    public AuthController(UserService userService, AuthService authService, PortOneService portOneService) {
        this.userService = userService;
        this.authService = authService;
        this.portOneService = portOneService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signUp(@Valid @RequestBody SignUpRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDto> refresh(@Valid @RequestBody RefreshRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String accessToken = TokenExtractor.extractBearerToken(servletRequest);
        authService.logout(accessToken, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/portone/verify")
    public ResponseEntity<PortOneVerifyResponseDto> verifyIdentity(@Valid @RequestBody PortOneVerifyRequestDto request) {
        return ResponseEntity.ok(portOneService.verify(request.getImpUid()));
    }
}
