package site.benepay.domain.recommendation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.recommendation.vo.BenefitUsageVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

public interface RecommendationMapper {

	List<RecommendationCardCandidateVO> findRecommendationCardCandidates(
		@Param("userId") Long userId,
		@Param("targetYearMonth") String targetYearMonth
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