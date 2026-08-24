package site.benepay.domain.recommendation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.recommendation.vo.RecommendationBenefitUsageVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

public interface RecommendationMapper {

	RecommendationMerchantVO findMerchantForRecommendation(
		@Param("merchantId") Long merchantId
	);

	/**
	 * 사용자가 보유한 카드 전체의 이번 달 혜택별 소진액/횟수(card_benefit_monthly_usage).
	 * 매장 N개를 평가하는 동안 반복 조회하지 않도록 recommendMerchants에서 한 번만 호출한다.
	 */
	List<RecommendationBenefitUsageVO> findMonthlyUsageByUserId(
		@Param("userId") Long userId,
		@Param("targetYearMonth") String targetYearMonth
	);

	/**
	 * 사용자가 보유한 카드 전체의 올해 혜택별 누적 횟수(월별 집계를 연 단위로 SUM).
	 * annualCountLimit 헤드룸 계산에 쓴다.
	 */
	List<RecommendationBenefitUsageVO> findAnnualCountByUserId(
		@Param("userId") Long userId,
		@Param("targetYear") Integer targetYear
	);
}
