package site.benepay.domain.recommendation.engine;

import java.util.List;
import java.util.Locale;

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

	/**
	 * 이 혜택이 주어진 매장에 실제로 적용되는지. categoryCodes는 업종 대분류일 뿐이라(예:
	 * "5812 음식점"), MERCHANT_BRAND처럼 특정 매장에만 한정된 혜택은 categoryCodes 매칭만으론
	 * 부족하다 - 반드시 이 메서드까지 같이 확인해야 한다(BenefitServiceImpl.matchesMerchant와
	 * 같은 판정을 여기 한 곳으로 모았다 - 그 클래스는 이 메서드로 위임한다).
	 *
	 * <p>merchantName이 null이면(비교 대상 매장이 특정되지 않은 경우, 예: 지갑 전체 기준
	 * "어느 카테고리가 제일 유리한가" 계산) 매장 제한 혜택도 그냥 통과시킨다 - 그 호출부는
	 * 애초에 특정 매장을 판정하는 게 아니라서 걸러낼 매장 자체가 없다.</p>
	 */
	public boolean matchesMerchant(String merchantName) {
		if (!isMerchantLimited() || merchantName == null) {
			return true;
		}

		String normalized = normalize(merchantName);
		if (normalized.isBlank()) {
			return false;
		}

		for (String targetMerchant : merchantNames) {
			String normalizedTarget = normalize(targetMerchant);
			if (normalized.contains(normalizedTarget) || normalizedTarget.contains(normalized)) {
				return true;
			}
		}
		return false;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.replace(" ", "").toLowerCase(Locale.KOREAN);
	}
}
