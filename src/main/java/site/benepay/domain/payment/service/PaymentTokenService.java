package site.benepay.domain.payment.service;

import site.benepay.domain.payment.dto.PaymentTokenResponseDto;

public interface PaymentTokenService {

	PaymentTokenResponseDto issueToken(Long userId, Long userCardId);

	PaymentTokenResponseDto getTokenStatus(String paymentTokenId);
}
