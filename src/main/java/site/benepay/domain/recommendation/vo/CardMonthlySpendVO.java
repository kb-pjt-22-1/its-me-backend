package site.benepay.domain.recommendation.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * card_monthly_status 한 행 - 카드 한 장의 특정 달 총 실적. 모드 2(실적 채우기)의
 * dailyRate/cv/hits 계산에 쓰는 월별 이력이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CardMonthlySpendVO {

	private Long userCardId;
	private String targetYearMonth;
	private Long totalSpendingAmount;
}
