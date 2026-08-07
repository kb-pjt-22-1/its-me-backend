package site.benepay.domain.recommendation.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * card_benefit_monthly_usage 한 행 - 카드 한 장의 혜택(serviceName) 하나가
 * 특정 달에 얼마나/몇 번 소진됐는지. 결제 처리 기능이 아직 없어 이 테이블은
 * 지금 항상 비어 있다(읽기 경로만 미리 만들어 둠) - db/2026-08-07_card_benefit_monthly_usage.sql 참고.
 */
@Getter
@Setter
@NoArgsConstructor
public class BenefitUsageVO {

	private String serviceName;
	private String targetYearMonth;
	private Long usedAmount;
	private Integer usedCount;
}
