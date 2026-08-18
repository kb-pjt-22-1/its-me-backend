package site.benepay.domain.recommendation.engine;

/**
 * BenefitEngine.selectPaymentBenefit()이 결제 1건에 실제로 적용하기로 고른 혜택.
 * 적용할 혜택이 없으면(한도 소진, 최소결제금액 미달, 카테고리 혜택 없음 등) NONE -
 * serviceName=null, discountAmount=0.
 */
public record BenefitApplication(String serviceName, long discountAmount) {

	public static final BenefitApplication NONE = new BenefitApplication(null, 0L);
}
