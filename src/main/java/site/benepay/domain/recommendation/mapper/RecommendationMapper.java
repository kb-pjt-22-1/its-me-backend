package site.benepay.domain.recommendation.mapper;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

public interface RecommendationMapper {

	RecommendationMerchantVO findMerchantForRecommendation(
		@Param("merchantId") Long merchantId
	);
}
