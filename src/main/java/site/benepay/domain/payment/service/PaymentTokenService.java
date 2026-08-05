package site.benepay.domain.payment.service;

import java.math.BigDecimal;

import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;

public interface PaymentTokenService {

	PaymentTokenResponseDto issueToken(Long userId, Long userCardId, Long merchantId,
		BigDecimal originalAmount, BigDecimal discountAmount);

	PaymentTokenResponseDto getTokenStatus(String paymentTokenId);

	// 명세서엔 없지만, 바코드를 "결제완료"시켜 실제 payments 행을 만드는 다리 역할.
	PaymentResponseDto completeToken(String paymentTokenId);
}
