package site.benepay.domain.recommendation.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.dto.CardBenefitComparisonResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.BenefitEngine;
import site.benepay.domain.recommendation.engine.BenefitJsonParser;
import site.benepay.domain.recommendation.engine.Mode3Result;
import site.benepay.domain.recommendation.engine.PerformanceTier;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

	// 이번 달 확정 이득(now)과 다음 달 확률적 기대 이득(future)을 1:1로 합산한다
	// (CsvProcessing category_search.py의 evaluate_priority 기본값과 동일).
	private static final double PRIORITY_BETA = 1.0;
	// DB 커넥션의 serverTimezone(application.properties의 db.url)과 맞춘다 - UserServiceImpl과 동일 규약.
	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

	private final RecommendationMapper recommendationMapper;
	private final MerchantCategoryService merchantCategoryService;
	private final ObjectMapper objectMapper;
	private final RecommendationParamsLoader recommendationParamsLoader;

	// TODO: 다른 팀원의 카드 추천 알고리즘 구현이 완료되면 이 클래스에 주입한다.

	@Override
	public MerchantCardRecommendationResponseDto getCardRecommendations(
		Long userId,
		Long merchantId
	) {
		validateUserId(userId);

		RecommendationMerchantVO merchant =
			recommendationMapper.findMerchantForRecommendation(merchantId);

		if (merchant == null) {
			throw new MerchantNotFoundException(
				"존재하지 않는 매장입니다."
			);
		}

		/*
		 * TODO: 다른 팀원의 카드 추천 알고리즘 연결 위치
		 *
		 * 팀원의 추천 알고리즘 구현이 완료되면
		 * 아래와 같은 흐름으로 교체한다.
		 *
		 * List<CardBenefitComparisonResponseDto> cards =
		 *     cardRecommendationAlgorithm.recommend(
		 *         heldCards,
		 *         merchant
		 *     );
		 *
		 * 현재는 추천 알고리즘이 연결되지 않았으므로
		 * 카드 비교 결과를 빈 목록으로 반환한다.
		 */
		List<CardBenefitComparisonResponseDto> cards =
			Collections.emptyList();

		return MerchantCardRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.merchantName(merchant.getMerchantName())
			.categoryCode(merchant.getCategoryCode())
			.brandId(merchant.getBrandId())
			.cards(cards)
			.build();
	}

	@Override
	public List<NearbyMerchantRecommendationResponseDto> recommendMerchants(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<MerchantResponseDto> merchants
	) {
		validateUserId(userId);

		if (merchants.isEmpty()) {
			return Collections.emptyList();
		}

		// 카테고리 코드를 응답용 카테고리 이름 + typicalPaymentAmount 조회 키로 쓰기 위한 Map이다.
		Map<String, String> categoryNames = merchantCategoryService.getCategoryList().stream()
			.collect(Collectors.toMap(
				MerchantCategoryResponseDto::getCategoryCode,
				MerchantCategoryResponseDto::getCategoryName,
				(first, ignored) -> first
			));

		// 지갑 전체 여력(P_흐름 기준) - 매장마다 다시 합산하지 않도록 한 번만 계산해 재사용한다.
		Map<String, Long> walletSpendHistory = aggregateWalletSpendHistory(heldCards);

		// benefitsInfo JSON 파싱은 카드당 비용이 있으므로, 매장 수만큼 반복하지 않도록
		// 카드별로 한 번만 파싱해 재사용한다(카드 M장 x 매장 N개가 아니라 M번만 파싱).
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers = heldCards.stream()
			.collect(Collectors.toMap(
				candidate -> candidate,
				candidate -> BenefitJsonParser.parse(candidate.getBenefitsInfo(), objectMapper)
			));

		return merchants.stream()
			.map(merchant -> toOptimalCardRecommendation(merchant, heldCards, categoryNames, walletSpendHistory,
				parsedTiers))
			.toList();
	}

	/**
	 * 매장 하나에 대해 사용자 보유 카드 중 모드 3(우선순위) 기준 최적 카드를 고른다. 필터링해서
	 * 매장을 걸러내지 않고 전달받은 매장 전부를 그대로 반환하되, 그 최적 카드의 total(이번 달
	 * 확정 + beta × 다음 달 기대)이 0보다 큰 매장만 benefitAvailable=true로 표시하고
	 * recommendedCardName/benefitSummary를 채운다 - 카테고리가 추천 분석 대상(16개 대분류)
	 * 밖이거나 실질적 이득이 없으면 benefitAvailable=false로 나머지 필드는 비워 둔다. 전달받은
	 * 매장 리스트를 카테고리로 미리 좁히지 않고 전부 평가하므로, 검색 카테고리 밖이라 혜택받을
	 * 수 있는 다른 매장을 놓치는 문제가 없다. 위치 기반 조회라 거리 개념이 없어 distanceMeters는
	 * 항상 null이다.
	 */
	private NearbyMerchantRecommendationResponseDto toOptimalCardRecommendation(
		MerchantResponseDto merchant,
		List<RecommendationCardCandidateVO> heldCards,
		Map<String, String> categoryNames,
		Map<String, Long> walletSpendHistory,
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers
	) {
		String categoryName = categoryNames.get(merchant.getCategoryCode());
		Map.Entry<RecommendationCardCandidateVO, Mode3Result> best = categoryName == null
			? null
			: findBestCard(heldCards, merchant.getCategoryCode(), categoryName, walletSpendHistory, parsedTiers);

		boolean benefitAvailable = best != null && best.getValue().total() > 0;

		return NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.merchantName(merchant.getMerchantName())
			.categoryName(categoryName)
			.latitude(merchant.getLatitude())
			.longitude(merchant.getLongitude())
			.distanceMeters(null)
			.benefitAvailable(benefitAvailable)
			.benefitSummary(benefitAvailable ? best.getValue().note() : null)
			.recommendedCardName(benefitAvailable ? best.getKey().getCardName() : null)
			.build();
	}

	private Map.Entry<RecommendationCardCandidateVO, Mode3Result> findBestCard(
		List<RecommendationCardCandidateVO> heldCards,
		String categoryCode,
		String categoryName,
		Map<String, Long> walletSpendHistory,
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers
	) {
		Long typicalAmount = recommendationParamsLoader.params().typicalPaymentAmount().get(categoryName);
		if (typicalAmount == null || heldCards.isEmpty()) {
			return null;
		}

		return heldCards.stream()
			.map(candidate -> Map.entry(
				candidate,
				scorePriority(candidate, categoryCode, categoryName, typicalAmount, walletSpendHistory,
					parsedTiers.get(candidate))
			))
			.max(Comparator.comparingDouble(e -> e.getValue().total()))
			.orElse(null);
	}

	/**
	 * 카드 한 장에 대해 모드 3을 평가한다. prevMonthSpend(activeTier 기준)는 이 카드
	 * spendHistory의 가장 최신 달 값이고, currentMonthSpend(baselineTier/nextTier 기준)는
	 * 진행 중인 이번 달 누적이다.
	 */
	private Mode3Result scorePriority(
		RecommendationCardCandidateVO candidate,
		String categoryCode,
		String categoryName,
		long typicalAmount,
		Map<String, Long> walletSpendHistory,
		List<PerformanceTier> tiers
	) {
		Map<String, Long> spendHistory =
			candidate.getSpendHistory() == null ? Collections.emptyMap() : candidate.getSpendHistory();
		long prevMonthSpend = spendHistory.isEmpty() ? 0L : spendHistory.get(Collections.max(spendHistory.keySet()));
		long currentMonthSpend = candidate.getCurrentMonthSpend() == null ? 0L : candidate.getCurrentMonthSpend();

		return BenefitEngine.evaluatePriority(
			tiers, prevMonthSpend, currentMonthSpend, categoryCode, categoryName, typicalAmount,
			spendHistory, walletSpendHistory, recommendationParamsLoader.params(), LocalDate.now(APP_ZONE),
			PRIORITY_BETA
		);
	}

	/**
	 * 보유 카드 전체의 spendHistory를 월별로 합산해 "지갑 전체" 과거 실적 이력을 만든다.
	 * 모드 3의 P_흐름(다음 구간 도달 확률)이 카드 습관이 아니라 지갑 전체 여력을 기준으로
	 * 삼기 때문에 필요하다.
	 */
	private Map<String, Long> aggregateWalletSpendHistory(List<RecommendationCardCandidateVO> heldCards) {
		Map<String, Long> wallet = new HashMap<>();
		for (RecommendationCardCandidateVO card : heldCards) {
			Map<String, Long> history = card.getSpendHistory();
			if (history == null) {
				continue;
			}
			for (Map.Entry<String, Long> entry : history.entrySet()) {
				wallet.merge(entry.getKey(), entry.getValue() == null ? 0L : entry.getValue(), Long::sum);
			}
		}
		return wallet;
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException(
				"로그인 사용자 정보가 필요합니다."
			);
		}
	}
}
