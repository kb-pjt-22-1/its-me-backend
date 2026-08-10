package site.benepay.domain.recommendation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.recommendation.vo.BenefitUsageVO;
import site.benepay.domain.recommendation.vo.CardMonthlySpendVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

public interface RecommendationMapper {

	List<RecommendationCardCandidateVO> findRecommendationCardCandidates(
		@Param("userId") Long userId,
		@Param("targetYearMonth") String targetYearMonth
	);

	/**
	 * 모드 2(실적 채우기)용 - 이 유저의 활성 카드 전부에 대해 fromYearMonth부터 지금까지의
	 * 월별 실적을 한 번에 가져온다(카드마다 따로 조회하지 않도록). 서비스 계층에서
	 * userCardId별로 묶어 dailyRate/cv/hits 계산에 쓴다.
	 */
	List<CardMonthlySpendVO> findSpendHistoryForUser(
		@Param("userId") Long userId,
		@Param("fromYearMonth") String fromYearMonth
	);

	RecommendationMerchantVO findMerchantForRecommendation(
		@Param("merchantId") Long merchantId
	);

	/**
	 * 카드 한 장의 올해치 혜택 소진 현황을 전부 가져온다(혜택마다 매번 쿼리하지 않도록).
	 * 이번 달분은 월 한도/월 횟수 판정에, 연간 합계는 연 횟수 판정에 쓴다.
	 * db/2026-08-07_card_benefit_monthly_usage.sql이 its-me-infra에 반영되기 전까지는
	 * 이 테이블이 없어 항상 빈 리스트가 온다 - 결제 처리 기능이 생기면 채워진다.
	 */
	List<BenefitUsageVO> findYearlyBenefitUsage(
		@Param("userCardId") Long userCardId,
		@Param("targetYear") int targetYear
	);
}