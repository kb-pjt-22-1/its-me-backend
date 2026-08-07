package site.benepay.domain.payment.service;

import java.math.BigDecimal;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;

public interface PaymentTokenService {

	PaymentTokenResponseDto issueToken(Long userId, Long userCardId);

	PaymentTokenResponseDto getTokenStatus(String paymentTokenId);

	// "결제완료하기" 버튼 → 결제 이벤트 생성 (payments 테이블에 실제 행을 만든다).
	PaymentHistoryResponseDto completeToken(String paymentTokenId, Long merchantId,
		BigDecimal originalAmount, BigDecimal discountAmount);
}