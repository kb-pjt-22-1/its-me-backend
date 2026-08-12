package site.benepay.domain.benefit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 이번 달 카테고리별 혜택 합계 read model. 월간 리포트(#47/#50) 전용이며,
 * 연회비 본전(DailyBenefitAmountVO)과는 별개 조회다.
 */
@Getter
@Setter
public class MonthlyCategoryBenefitVO {

	private String categoryCode;
	private String categoryName;
	private Long categoryAmount;
}
