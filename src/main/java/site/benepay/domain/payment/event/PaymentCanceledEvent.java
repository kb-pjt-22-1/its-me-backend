package site.benepay.domain.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소(APPROVED -> CANCELED) 시 발행하는 이벤트. card 도메인의
 * CardPerformanceEventHandler가 userCardId/approvedAt/performanceAmount를 받아서
 * card_monthly_status에 집계된 카드 실적을 차감한다.
 *
 * <p>categoryCode/discountAmount는 benefit 도메인이 카테고리별 혜택 사용액을
 * 차감할 때 사용할 수 있도록 함께 전달한다. 해당 benefit 리스너는 아직 구현되지 않았다.
 *
 * <p>approvedAt은 취소 시각이 아니라 "원래 결제가 승인됐던 시각"이다. 실적/혜택 사용액은
 * 승인된 달의 집계 테이블에 더해졌으므로, 취소도 같은 달에서 빼야 한다 (취소 시점의 달이 아님).
 */
public record PaymentCanceledEvent(
	Long paymentId,
	Long userCardId,
	String categoryCode,
	LocalDateTime approvedAt,
	BigDecimal performanceAmount,
	BigDecimal discountAmount
) {
}
