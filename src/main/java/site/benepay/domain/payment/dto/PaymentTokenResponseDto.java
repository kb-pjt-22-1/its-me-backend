package site.benepay.domain.payment.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.payment.service.PaymentTokenStore;
import site.benepay.domain.payment.vo.PaymentTokenVO;

@Getter
@Builder
public class PaymentTokenResponseDto {

	private String paymentTokenId;
	// 바코드에 실제로 표시/스캔되는 값. 매 발급마다 새로 생성되는 paymentTokenId를 쓴다 -
	// 카드 고정 토큰(user_cards.payment_token)을 그대로 노출하면 매번 같은 값이 떠서
	// 재사용(리플레이) 위험이 생기고, 실제 동적 바코드 결제 방식과도 다르다.
	private String tokenValue;
	private String paymentMethod;
	private String status;
	private LocalDateTime issuedAt;
	private LocalDateTime expiresAt;

	public static PaymentTokenResponseDto from(PaymentTokenVO token) {
		LocalDateTime issuedAt = LocalDateTime.parse(token.getIssuedAt());

		return PaymentTokenResponseDto.builder()
			.paymentTokenId(token.getPaymentTokenId())
			.tokenValue(token.getPaymentTokenId())
			.paymentMethod(token.getPaymentMethod())
			.status(token.getStatus())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(PaymentTokenStore.TTL))
			.build();
	}
}