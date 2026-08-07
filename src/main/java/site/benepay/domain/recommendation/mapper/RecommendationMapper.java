package site.benepay.domain.recommendation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

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
}