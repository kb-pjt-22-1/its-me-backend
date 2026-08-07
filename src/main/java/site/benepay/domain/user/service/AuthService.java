package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.DevLoginRequestDto;
import site.benepay.domain.user.dto.LoginRequestDto;
import site.benepay.domain.user.dto.LoginResponseDto;
import site.benepay.domain.user.dto.RefreshRequestDto;
import site.benepay.domain.user.dto.RefreshResponseDto;

public interface AuthService {

	LoginResponseDto login(LoginRequestDto request);

	RefreshResponseDto refresh(RefreshRequestDto request);

	void logout(String accessToken, Long userId);

	/**
	 * 비밀번호 확인 없이 미리 심어 둔 개발 계정으로 토큰을 발급한다.
	 * dev-login.enabled가 켜져 있을 때만 동작한다.
	 */
	LoginResponseDto devLogin(DevLoginRequestDto request);
}
