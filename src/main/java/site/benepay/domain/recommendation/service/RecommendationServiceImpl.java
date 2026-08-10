package site.benepay.domain.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;

@Service
public class RecommendationServiceImpl implements RecommendationService {

	/*
	 * TODO: 카드 혜택 기반 추천 알고리즘이 별도 브랜치(recommendation 도메인)에서 병합되면
	 * 사용자 보유 카드로 지금 당장 혜택을 주는 매장만 recommended=true로 표시하도록 교체한다.
	 * 지금은 알고리즘이 없어 후보 매장을 그대로 반환한다(전부 recommended=false).
	 */
	@Override
	public List<MerchantRecommendationResponseDto> markRecommendedMerchants(
		Long userId, List<MerchantRecommendationResponseDto> candidates) {
		return candidates;
	}
}
