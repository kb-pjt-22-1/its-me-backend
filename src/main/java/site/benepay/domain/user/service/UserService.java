package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.ChangePasswordRequestDto;
import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;
import site.benepay.domain.user.dto.VerifyPasswordRequestDto;
import site.benepay.domain.user.dto.VerifyPinRequestDto;

public interface UserService {

	UserResponseDto signUp(SignUpRequestDto request);

	UserResponseDto getMyProfile(Long userId);

	UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request);

	void verifyPassword(Long userId, VerifyPasswordRequestDto request);

	void changePassword(Long userId, ChangePasswordRequestDto request);

	void registerPin(Long userId, RegisterPinRequestDto request);

	void updateOrDeletePin(Long userId, UpdateDeletePinRequestDto request);

	void verifyPin(Long userId, VerifyPinRequestDto request);

	void withdraw(Long userId, boolean confirmed);
}
