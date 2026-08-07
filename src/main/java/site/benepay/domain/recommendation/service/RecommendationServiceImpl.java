package site.benepay.domain.recommendation.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.recommendation.dto.CardBenefitComparisonResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

	private static final String TARGET_YEAR_MONTH = "202607";

	private final RecommendationMapper recommendationMapper;

	@Override
	public MerchantCardRecommendationResponseDto getCardRecommendations(
		Long userId,
		Long merchantId
	) {
		// 1. 사용자가 선택한 매장 조회
		RecommendationMerchantVO merchant =
			recommendationMapper.findMerchantForRecommendation(merchantId);

		if (merchant == null) {
			throw new IllegalArgumentException("존재하지 않는 매장입니다.");
		}

		// 2. 사용자의 추천 대상 보유 카드와 전월 실적 조회
		List<RecommendationCardCandidateVO> candidates =
			recommendationMapper.findRecommendationCardCandidates(
				userId,
				TARGET_YEAR_MONTH
			);

		/*
		 * 다음 단계에서 candidates의 benefitsInfo를 분석해
		 * CardBenefitComparisonResponseDto 목록으로 변환한다.
		 */
		List<CardBenefitComparisonResponseDto> cards =
			Collections.emptyList();

		// 3. 선택 매장 정보와 카드 비교 결과 반환
		return MerchantCardRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.merchantName(merchant.getMerchantName())
			.categoryCode(merchant.getCategoryCode())
			.brandId(merchant.getBrandId())
			.cards(cards)
			.build();
	}
}