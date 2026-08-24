package site.benepay.domain.benefit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.benefit.vo.BenefitUsageVO;

/**
 * card_benefit_monthly_usage(혜택별 월/연 한도 소진액 집계) 전용 매퍼. 결제 승인/취소
 * 이벤트로 집계를 갱신하는 쓰기 경로(BenefitUsageEventHandler)와, 결제 시점/추천 시점에
 * "이미 얼마나 썼는지"를 읽는 경로 양쪽에서 쓴다.
 */
public interface BenefitUsageMapper {

	/**
	 * 결제 승인 시 소진액을 더한다. 같은 (user_card_id, benefit_service_name, target_year_month)
	 * 행이 없으면 새로 만들고, 있으면 used_amount/used_count를 누적한다.
	 */
	int upsertMonthlyUsage(
		@Param("userCardId") Long userCardId,
		@Param("benefitServiceName") String benefitServiceName,
		@Param("targetYear") Integer targetYear,
		@Param("targetYearMonth") String targetYearMonth,
		@Param("usedAmount") long usedAmount
	);

	/**
	 * 결제 취소 시 소진액을 뺀다(0 미만으로는 내려가지 않음). 집계 행이 없으면 0을 반환한다.
	 */
	int decrementMonthlyUsage(
		@Param("userCardId") Long userCardId,
		@Param("benefitServiceName") String benefitServiceName,
		@Param("targetYearMonth") String targetYearMonth,
		@Param("usedAmount") long usedAmount
	);

	/**
	 * 카드 한 장의 이번 달 혜택별 소진액/횟수. 결제 시점에 monthlyDiscountLimit/
	 * monthlyCountLimit 헤드룸을 계산할 때 쓴다.
	 */
	List<BenefitUsageVO> findMonthlyUsageByUserCardId(
		@Param("userCardId") Long userCardId,
		@Param("targetYearMonth") String targetYearMonth
	);

	/**
	 * 카드 한 장의 올해 혜택별 누적 횟수(월별 집계를 연 단위로 SUM). annualCountLimit
	 * 헤드룸을 계산할 때 쓴다.
	 */
	List<BenefitUsageVO> findAnnualCountByUserCardId(
		@Param("userCardId") Long userCardId,
		@Param("targetYear") Integer targetYear
	);
}
