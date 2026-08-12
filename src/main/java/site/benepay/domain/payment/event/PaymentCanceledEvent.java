package site.benepay.domain.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소(APPROVED -> CANCELED) 시 발행하는 이벤트. 아직 이걸 받는 리스너가
 * card/benefit 어느 도메인에도 없다 - 리스너가 없어도 이벤트 발행 자체는 안전하니
 * 미리 발행해두고, 나중에 리스너가 붙으면 그때부터 바로 동작한다.
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
