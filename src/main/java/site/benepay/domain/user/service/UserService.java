package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.RegisterPinRequestDto;
import site.benepay.domain.user.dto.SignUpRequestDto;
import site.benepay.domain.user.dto.UpdateDeletePinRequestDto;
import site.benepay.domain.user.dto.UpdateProfileRequestDto;
import site.benepay.domain.user.dto.UserResponseDto;

public interface UserService {

    UserResponseDto signUp(SignUpRequestDto request);

    UserResponseDto getMyProfile(Long userId);

    UserResponseDto updateProfile(Long userId, UpdateProfileRequestDto request);

    void registerPin(Long userId, RegisterPinRequestDto request);

    void updateOrDeletePin(Long userId, UpdateDeletePinRequestDto request);

    void withdraw(Long userId, boolean confirmed);
}
