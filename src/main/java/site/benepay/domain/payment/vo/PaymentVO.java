package site.benepay.domain.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentVO {
	private Long paymentId;
	private Long merchantId;
	private Long userCardId;

	private LocalDateTime paymentTime;

	private BigDecimal originalAmount;
	private BigDecimal discountAmount;
	private BigDecimal finalAmount;

	private String paymentStatus;
	private String paymentMethod;
}
