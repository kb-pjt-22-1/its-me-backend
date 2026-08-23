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
	private Long merchantId;
	// 브랜드 없이 등록된 매장도 있어(개인 매장 등) nullable. 화면에 표시되지 않고, 지도
	// "혜택순" 정렬의 결제 이력 기반 가중치 계산(매장>브랜드>카테고리)에 프론트에서 내부적으로만 쓴다.
	private Long brandId;
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
			.merchantId(payment.getMerchantId())
			.brandId(payment.getBrandId())
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
