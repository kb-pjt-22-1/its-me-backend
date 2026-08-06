package site.benepay.domain.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class PaymentVO {

	private Long paymentId;
	@NonNull
	private Long merchantId;
	@NonNull
	private Long userCardId;
    @NonNull
	private LocalDateTime paymentTime;
    @NonNull
	private BigDecimal originalAmount;
	@NonNull
	private BigDecimal discountAmount;
	@NonNull
	private BigDecimal finalAmount;
	// payment_status: PENDING, APPROVED, CANCELED, PAYMENT_FAILED (common_codes 그룹 PAYMENT_STATUS)
	@NonNull
	private String paymentStatus;
	// payment_method: BARCODE, QR (common_codes 그룹 PAYMENT_METHOD)
	@NonNull
	private String paymentMethod;
}
