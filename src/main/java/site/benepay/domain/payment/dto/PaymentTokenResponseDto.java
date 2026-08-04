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
	// 바코드/QR에 실릴 값. 지금은 paymentTokenId를 그대로 쓴다 (실 카드 토큰은 서버 밖으로 안 나감).
	private String tokenValue;
	private Long userCardId;
	private String status;
	private LocalDateTime issuedAt;
	private LocalDateTime expiresAt;

	public static PaymentTokenResponseDto from(PaymentTokenVO token) {
		LocalDateTime issuedAt = LocalDateTime.parse(token.getIssuedAt());

		return PaymentTokenResponseDto.builder()
			.paymentTokenId(token.getPaymentTokenId())
			.tokenValue(token.getPaymentTokenId())
			.userCardId(token.getUserCardId())
			.status(token.getStatus())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(PaymentTokenStore.TTL))
			.build();
	}
}
