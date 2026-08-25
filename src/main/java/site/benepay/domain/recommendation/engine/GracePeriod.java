package site.benepay.domain.recommendation.engine;

/**
 * cards.benefits_info JSON의 최상위 gracePeriod 객체. 신규 카드가 전월 실적이 없어도
 * 일정 기간 특정 구간의 혜택을 받을 수 있게 하는 규칙이다 - performanceTiers[]와 형제
 * 필드라 PerformanceTier에는 안 들어있다.
 */
public record GracePeriod(boolean available, boolean minimumSpendingRequired, String applicableBenefitNodeId) {

	public static final GracePeriod NONE = new GracePeriod(false, true, null);
}
