package site.benepay.domain.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 승인(APPROVED) 시 발행하는 이벤트. card 도메인의 CardPerformanceEventHandler가
 * userCardId/approvedAt/performanceAmount를 받아서 card_monthly_status(카드 실적)를 갱신한다.
 *
 * <p>categoryCode/discountAmount/benefitServiceName은 benefit 도메인의
 * BenefitUsageEventHandler가 card_benefit_monthly_usage(혜택별 월/연 한도 소진액)를
 * 갱신할 때 쓴다 - 필드를 추가해도 기존 리스너(카드 쪽)는 안 쓰는 필드라 영향 없다.
 *
 * <p>performanceAmount는 카드 실적으로 인정할 금액이다. 원금(originalAmount)이 아니라
 * 실제로 결제된 finalAmount를 쓴다 - 할인 적용 후 실제로 청구된 금액이 "이용실적"이라고
 * 판단했다. 다른 기준(원금 기준)이 맞다면 여기만 바꾸면 된다.
 */
public record PaymentApprovedEvent(
	Long paymentId,
	Long userCardId,
	String categoryCode,
	LocalDateTime approvedAt,
	BigDecimal performanceAmount,
	BigDecimal discountAmount,
	String benefitServiceName
) {
}
