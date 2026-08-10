package site.benepay.domain.recommendation.engine;

/**
 * 모드 2(실적 채우기) 상태 분류. CsvProcessing category_search.py의 BUILD_STATES 순서 그대로
 * (표시 우선순위이기도 하다).
 */
public enum BuildStatus {

	TIER_UPGRADABLE("구간상향가능"),
	HARD_TO_REACH("도달어려움"),
	TOP_TIER_SECURED("최고구간확보"),
	NO_BENEFIT("혜택없음");

	private final String label;

	BuildStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
