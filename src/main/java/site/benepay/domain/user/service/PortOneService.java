package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.PortOneVerifyResponseDto;

public interface PortOneService {

    PortOneVerifyResponseDto verify(String impUid);
}
