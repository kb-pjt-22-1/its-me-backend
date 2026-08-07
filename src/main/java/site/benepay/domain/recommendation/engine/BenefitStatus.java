package site.benepay.domain.recommendation.engine;

/**
 * 모드 1(즉시 할인) 상태 분류. README.md "6. 상태 분류" 순서 그대로
 * (표시 우선순위이기도 하다 - 이 순서대로 그룹핑해서 보여준다).
 */
public enum BenefitStatus {

	IMMEDIATE_DISCOUNT("즉시할인"),
	CONDITIONAL_DISCOUNT("조건부할인"),
	LIMIT_EXHAUSTED("한도소진"),
	COUNT_EXHAUSTED("횟수소진"),
	PERFORMANCE_INSUFFICIENT("실적부족"),
	NO_BENEFIT("혜택없음");

	private final String label;

	BenefitStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
