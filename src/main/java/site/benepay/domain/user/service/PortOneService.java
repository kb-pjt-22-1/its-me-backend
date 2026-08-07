package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.PortOneVerifyRequestDto;
import site.benepay.domain.user.dto.PortOneVerifyResponseDto;

public interface PortOneService {

	PortOneVerifyResponseDto verify(PortOneVerifyRequestDto request);
}
