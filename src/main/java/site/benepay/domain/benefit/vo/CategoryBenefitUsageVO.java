package site.benepay.domain.benefit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 카드 한 장이 이번 달 특정 카테고리에서 실제로 소진한 혜택 금액/횟수.
 *
 * <p>payments에는 결제 1건이 어느 혜택(serviceName)을 적용받았는지 기록하는 컬럼이 없어
 * (db/2026-08-07_card_benefit_monthly_usage.sql 참고, 쓰기 경로 미구현), 혜택 단위가 아니라
 * 카드+카테고리 단위로만 집계할 수 있다 - 같은 카드가 같은 카테고리에 혜택을 2개 이상 가지면
 * 두 혜택 모두 이 값을 그대로 대응시킨다(한도 소진량이 실제보다 커 보일 수 있음).</p>
 */
@Getter
@Setter
public class CategoryBenefitUsageVO {

	private Long userCardId;
	private String categoryCode;
	private Long usedAmount;
	private Integer usedCount;
}
