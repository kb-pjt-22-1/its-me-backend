package site.benepay.domain.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.payment.vo.PaymentVO;

@Getter
@Builder
public class PaymentResponseDto {

	private Long paymentId;
	private Long merchantId;
	private Long userCardId;

	private LocalDateTime paymentTime;

	private BigDecimal originalAmount;
	private BigDecimal discountAmount;
	private BigDecimal finalAmount;

	private String paymentStatus;
	private String paymentMethod;

	public static PaymentResponseDto from(PaymentVO payment) {
		return PaymentResponseDto.builder()
			.paymentId(payment.getPaymentId())
			.merchantId(payment.getMerchantId())
			.userCardId(payment.getUserCardId())
			.paymentTime(payment.getPaymentTime())
			.originalAmount(payment.getOriginalAmount())
			.discountAmount(payment.getDiscountAmount())
			.finalAmount(payment.getFinalAmount())
			.paymentStatus(payment.getPaymentStatus())
			.paymentMethod(payment.getPaymentMethod())
			.build();
	}
}
