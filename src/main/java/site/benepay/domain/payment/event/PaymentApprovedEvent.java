package site.benepay.domain.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 승인 완료 시 발행되는 이벤트이다.
 *
 * <p>record는 값 전달을 위한 불변 객체를 간결하게 정의하는 Java 문법으로,
 * 생성자, 필드 접근 메서드, equals(), hashCode(), toString()을 자동으로 제공한다.
 *
 * <p>결제 도메인에서 승인된 결제 정보를 전달하면
 * 카드 도메인에서 해당 이벤트를 수신하여 월별 카드 실적을 갱신한다.
 *
 * @param paymentId 결제 ID
 * @param userCardId 결제에 사용된 사용자 보유 카드 ID
 * @param performanceAmount 카드 실적에 반영할 금액
 * @param approvedAt 결제 승인 일시
 */

public record PaymentApprovedEvent(
	Long paymentId,
	Long userCardId,
	BigDecimal performanceAmount,
	LocalDateTime approvedAt
) {
}
