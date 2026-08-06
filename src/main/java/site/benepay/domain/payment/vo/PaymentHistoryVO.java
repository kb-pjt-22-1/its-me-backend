package site.benepay.domain.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class PaymentHistoryVO {

	private Long paymentId;
	@NonNull
	private String merchantName;
	@NonNull
	private String cardName;
	// 카드 마스킹 표시용 (카드번호 자체가 아니라 user_cards.pan_last4)
	@NonNull
	private String panLast4;
    @NonNull
	private LocalDateTime paymentTime;
    @NonNull
	private BigDecimal originalAmount;
	@NonNull
	private BigDecimal discountAmount;
	@NonNull
	private BigDecimal finalAmount;
	@NonNull
	private String paymentStatus;
	@NonNull
	private String paymentMethod;
}
