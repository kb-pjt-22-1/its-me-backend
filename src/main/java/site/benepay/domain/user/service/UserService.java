package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.ChangePasswordRequestDto;
import site.benepay.domain.user.dto.LoginResponseDto;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateFcmTokenRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.dto.VerifyPasswordRequestDto;
import site.benepay.domain.user.dto.VerifyPinRequestDto;

public interface UserService {

	// 가입 성공 시 토큰까지 즉시 발급한다(자동 로그인) - 프론트가 재로그인 없이 바로 홈으로
	// 이동할 수 있게 하기 위함. login/devLogin과 동일한 응답 모양을 그대로 재사용한다.
	LoginResponseDto signUp(SignUpRequestDto request);

	UserResponseDto getMyProfile(Long userId);

	UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request);

	// 로그인마다(또는 FCM 토큰 갱신 시마다) 호출된다 - 기기 목록이 아니라 값 하나라, 가장
	// 최근에 호출한 기기가 이전 값을 덮어쓴다.
	void updateFcmToken(Long userId, UpdateFcmTokenRequestDto request);

	void verifyPassword(Long userId, VerifyPasswordRequestDto request);

	void changePassword(Long userId, ChangePasswordRequestDto request);

	void registerPin(Long userId, RegisterPinRequestDto request);

	void updateOrDeletePin(Long userId, UpdateDeletePinRequestDto request);

	void verifyPin(Long userId, VerifyPinRequestDto request);

	void withdraw(Long userId, boolean confirmed);
}
