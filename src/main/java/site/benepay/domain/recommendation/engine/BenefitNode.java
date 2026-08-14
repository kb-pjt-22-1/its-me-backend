package site.benepay.domain.recommendation.engine;

import java.util.List;

/**
 * cards.benefits_info JSON의 performanceTiers[].benefits[] 원소 하나.
 * CsvProcessing/benefits.py의 Benefit dataclass를 그대로 옮긴 것 - 필드 이름/의미는
 * README.md "9. 혜택 계산 엔진" 절 참고.
 *
 * <p>업종 판정은 categoryCodes로만 한다 - benefitsInfo의 categoryName은 여러 코드를 묶은
 * 표시용 라벨이라 기준으로 쓰면 안 된다(같은 README).</p>
 */
public record BenefitNode(
	String serviceName,
	String benefitType,
	List<String> categoryCodes,
	String discountMethod,
	double discountRate,
	long discountAmount,
	long weekdayDiscountPerLiter,
	long weekendDiscountPerLiter,
	long minimumPaymentAmount,
	Long maximumEligiblePerTransaction,
	Long maximumDiscountPerTransaction,
	Long monthlyDiscountLimit,
	Long monthlyEligibleLimit,
	Integer monthlyCountLimit,
	Integer annualCountLimit,
	List<String> merchantNames,
	boolean integratedLimitExcluded,
	String merchantScope
) {

	public boolean isOverseas() {
		return "OVERSEAS".equals(merchantScope);
	}

	/**
	 * 이 혜택이 주어진 업종 코드를 커버하는지. benefitType이 ALL_MERCHANTS이고
	 * categoryCodes가 비어 있으면 전 가맹점 혜택이라 모든 업종을 커버한다.
	 */
	public boolean coversCategory(String categoryCode) {
		if ("ALL_MERCHANTS".equals(benefitType) && categoryCodes.isEmpty()) {
			return true;
		}
		return categoryCodes.contains(categoryCode);
	}

	/**
	 * 특정 매장에서만 되는 혜택인지 - representativeMerchantNames는 업종 전체를
	 * 대표하는 예시일 뿐 제한이 아니라 여기 포함하지 않는다(benefits.py와 동일).
	 */
	public boolean isMerchantLimited() {
		return !merchantNames.isEmpty();
	}

	public String merchantNote() {
		return isMerchantLimited() ? String.join("·", merchantNames) + " 한정" : "";
	}
}
