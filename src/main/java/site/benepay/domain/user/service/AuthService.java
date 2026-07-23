package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.LoginRequestDto;
import site.benepay.domain.user.dto.LoginResponseDto;
import site.benepay.domain.user.dto.RefreshRequestDto;
import site.benepay.domain.user.dto.RefreshResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    RefreshResponseDto refresh(RefreshRequestDto request);

    void logout(String accessToken, Long userId);
}
