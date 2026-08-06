package site.benepay.domain.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.payment.vo.PaymentHistoryVO;

@Getter
@Builder
public class PaymentHistoryResponseDto {

	private Long paymentId;
	private String merchantName;
	private String cardName;
	// 화면 표시용 마스킹된 카드 정보. 실 카드번호는 애초에 안 갖고 있음 (user_cards.pan_last4만 사용).
	private String maskedCardNumber;

	private LocalDateTime paymentTime;

	private BigDecimal originalAmount;
	private BigDecimal discountAmount;
	private BigDecimal finalAmount;

	private String paymentStatus;
	private String paymentMethod;

	public static PaymentHistoryResponseDto from(PaymentHistoryVO payment) {
		return PaymentHistoryResponseDto.builder()
			.paymentId(payment.getPaymentId())
			.merchantName(payment.getMerchantName())
			.cardName(payment.getCardName())
			.maskedCardNumber("**** " + payment.getPanLast4())
			.paymentTime(payment.getPaymentTime())
			.originalAmount(payment.getOriginalAmount())
			.discountAmount(payment.getDiscountAmount())
			.finalAmount(payment.getFinalAmount())
			.paymentStatus(payment.getPaymentStatus())
			.paymentMethod(payment.getPaymentMethod())
			.build();
	}
}
