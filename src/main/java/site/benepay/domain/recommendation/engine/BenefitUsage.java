package site.benepay.domain.recommendation.engine;

/**
 * 카드 한 장의 혜택 하나(serviceName)에 대한 이번 달/올해 소진 현황.
 *
 * <p>지금은 이 값을 채워 넣는 결제 처리 로직이 없어(스키마에 추적 테이블만 준비해 둠)
 * 항상 {@link #NONE}(전액 미사용)으로 계산한다 - Python 콜드스타트 데모와 동일한 근사.
 * 결제 처리 기능이 생기면 RecommendationMapper의 사용량 조회 결과로 채우면 된다.</p>
 */
public record BenefitUsage(long usedAmount, int usedCountMonth, int usedCountYear) {

	public static final BenefitUsage NONE = new BenefitUsage(0L, 0, 0);
}
