package site.benepay.domain.recommendation.service;

import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.CategoryNotFoundException;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.CardBenefitComparisonResponseDto;
import site.benepay.domain.recommendation.dto.CardBenefitScoreDto;
import site.benepay.domain.recommendation.dto.CategoryCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.BenefitEngine;
import site.benepay.domain.recommendation.engine.BenefitJsonParser;
import site.benepay.domain.recommendation.engine.BenefitStatus;
import site.benepay.domain.recommendation.engine.BenefitUsage;
import site.benepay.domain.recommendation.engine.Mode1Result;
import site.benepay.domain.recommendation.engine.PerformanceTier;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.BenefitUsageVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMM");

	private final RecommendationMapper recommendationMapper;
	private final MerchantService merchantService;
	private final MerchantCategoryService merchantCategoryService;
	private final ObjectMapper objectMapper;
	private final RecommendationParamsLoader recommendationParamsLoader;

	/*
	 * TODO: 다른 팀원의 카드 추천 알고리즘 구현이 완료되면 이 클래스에 주입한다.
	 *
	 * 예시:
	 * private final CardBenefitRecommendationAlgorithm cardRecommendationAlgorithm;
	 */

	@Override
	public List<NearbyMerchantRecommendationResponseDto> getNearbyMerchants(
		Long userId,
		double swLat,
		double swLng,
		double neLat,
		double neLng
	) {
		validateUserId(userId);

		/*
		 * MerchantService에서 지도 화면 bounds 안의 매장 후보를 조회한다(bounds 자체 검증은
		 * MerchantService.getMerchantsWithinBounds가 한다). MerchantController의 API를 HTTP로
		 * 호출하는 것이 아니라, 같은 백엔드 안에 있는 MerchantService 메서드를 직접 호출한다.
		 */
		List<NearbyMerchantResponseDto> merchants =
			merchantService.getMerchantsWithinBounds(swLat, swLng, neLat, neLng);

		if (merchants.isEmpty()) {
			return Collections.emptyList();
		}

		List<RecommendationCardCandidateVO> candidates =
			recommendationMapper.findRecommendationCardCandidates(userId, getPreviousYearMonth());

		if (candidates.isEmpty()) {
			return Collections.emptyList();
		}

		// 카테고리 코드를 응답용 카테고리 이름 + typicalPaymentAmount 조회 키로 쓰기 위한 Map이다.
		Map<String, String> categoryNames =
			merchantCategoryService.getCategoryList().stream()
				.collect(Collectors.toMap(
					MerchantCategoryResponseDto::getCategoryCode,
					MerchantCategoryResponseDto::getCategoryName,
					(first, ignored) -> first
				));

		return merchants.stream()
			.map(merchant -> toOptimalCardRecommendation(merchant, candidates, categoryNames))
			.flatMap(Optional::stream)
			.collect(Collectors.toList());
	}

	/**
	 * 매장 하나에 대해 사용자 보유 카드 중 최적 카드를 고르고(scoreAndRank 재사용), 그 카드가
	 * 이 매장 카테고리에서 지금 당장(즉시할인) 혜택을 주는 경우에만 결과에 포함시킨다. 카테고리가
	 * 추천 분석 대상(16개 대분류) 밖이면 이 매장은 건너뛴다. bounds 조회라 거리 개념이 없어
	 * distanceMeters는 항상 null이다.
	 */
	private Optional<NearbyMerchantRecommendationResponseDto> toOptimalCardRecommendation(
		NearbyMerchantResponseDto merchant,
		List<RecommendationCardCandidateVO> candidates,
		Map<String, String> categoryNames
	) {
		String categoryName = categoryNames.get(merchant.getCategoryCode());
		if (categoryName == null) {
			return Optional.empty();
		}

		Long typicalAmount = recommendationParamsLoader.params().typicalPaymentAmount().get(categoryName);
		if (typicalAmount == null) {
			return Optional.empty();
		}

		List<Map.Entry<RecommendationCardCandidateVO, Mode1Result>> ranked =
			scoreAndRank(candidates, merchant.getCategoryCode(), categoryName, typicalAmount);

		if (ranked.isEmpty()) {
			return Optional.empty();
		}

		Map.Entry<RecommendationCardCandidateVO, Mode1Result> best = ranked.get(0);
		if (best.getValue().status() != BenefitStatus.IMMEDIATE_DISCOUNT) {
			return Optional.empty();
		}

		return Optional.of(
			NearbyMerchantRecommendationResponseDto.builder()
				.merchantId(merchant.getMerchantId())
				.merchantName(merchant.getMerchantName())
				.categoryName(categoryName)
				.latitude(merchant.getLatitude() == null ? null : merchant.getLatitude().doubleValue())
				.longitude(merchant.getLongitude() == null ? null : merchant.getLongitude().doubleValue())
				.distanceMeters(null)
				.benefitSummary(best.getValue().note())
				.recommendedCardName(best.getKey().getCardName())
				.build()
		);
	}

	@Override
	public MerchantCardRecommendationResponseDto getCardRecommendations(
		Long userId,
		Long merchantId
	) {
		validateUserId(userId);

		RecommendationMerchantVO merchant =
			recommendationMapper.findMerchantForRecommendation(merchantId);

		if (merchant == null) {
			throw new IllegalArgumentException(
				"존재하지 않는 매장입니다."
			);
		}

		/*
		 * TODO: 다른 팀원의 카드 추천 알고리즘 연결 위치
		 *
		 * 팀원의 추천 알고리즘 구현이 완료되면
		 * 아래와 같은 흐름으로 교체한다.
		 *
		 * List<RecommendationCardCandidateVO> candidates =
		 *     recommendationMapper.findRecommendationCardCandidates(
		 *         userId,
		 *         getPreviousYearMonth()
		 *     );
		 *
		 * List<CardBenefitComparisonResponseDto> cards =
		 *     cardRecommendationAlgorithm.recommend(
		 *         candidates,
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
	public CategoryCardRecommendationResponseDto getCardRecommendationsByCategory(
		Long userId,
		String categoryName
	) {
		validateUserId(userId);

		String categoryCode = resolveCategoryCode(categoryName);
		Long typicalAmount = recommendationParamsLoader.params().typicalPaymentAmount().get(categoryName);
		if (typicalAmount == null) {
			throw new CategoryNotFoundException(
				"'" + categoryName + "'는 추천 분석 대상 대분류가 아닙니다."
			);
		}

		List<RecommendationCardCandidateVO> candidates =
			recommendationMapper.findRecommendationCardCandidates(userId, getPreviousYearMonth());

		List<CardBenefitScoreDto> cards = scoreAndRank(candidates, categoryCode, categoryName, typicalAmount).stream()
			.map(e -> toDto(e.getKey(), e.getValue()))
			.collect(Collectors.toList());

		return CategoryCardRecommendationResponseDto.builder()
			.categoryName(categoryName)
			.typicalPaymentAmount(typicalAmount)
			.cards(cards)
			.build();
	}

	/**
	 * 카드 후보 전체를 이 카테고리 기준으로 평가해 상태(즉시할인 -> 조건부할인 -> ...) -> 할인율
	 * 내림차순으로 정렬한다. getCardRecommendationsByCategory와 getNearbyMerchants(최적 카드 선정)가
	 * 공유하는 랭킹 로직이다.
	 */
	private List<Map.Entry<RecommendationCardCandidateVO, Mode1Result>> scoreAndRank(
		List<RecommendationCardCandidateVO> candidates,
		String categoryCode,
		String categoryName,
		long typicalAmount
	) {
		return candidates.stream()
			.map(candidate -> Map.entry(candidate, scoreCandidate(candidate, categoryCode, categoryName, typicalAmount)))
			.sorted(Comparator
				.<Map.Entry<RecommendationCardCandidateVO, Mode1Result>>comparingInt(e -> e.getValue().status().ordinal())
				.thenComparing(e -> -e.getValue().rate()))
			.collect(Collectors.toList());
	}

	/**
	 * 카드 한 장에 대해 모드 1을 평가한다.
	 */
	private Mode1Result scoreCandidate(
		RecommendationCardCandidateVO candidate,
		String categoryCode,
		String categoryName,
		long typicalAmount
	) {
		List<PerformanceTier> tiers = BenefitJsonParser.parse(candidate.getBenefitsInfo(), objectMapper);
		long prevMonthSpend = candidate.getTotalSpendingAmount() == null ? 0L : candidate.getTotalSpendingAmount();
		Map<String, BenefitUsage> usage = loadBenefitUsage(candidate.getUserCardId());

		return BenefitEngine.evaluateNow(
			tiers, prevMonthSpend, categoryCode, categoryName, typicalAmount, usage, recommendationParamsLoader.params()
		);
	}

	private CardBenefitScoreDto toDto(RecommendationCardCandidateVO candidate, Mode1Result result) {
		return CardBenefitScoreDto.builder()
			.userCardId(candidate.getUserCardId())
			.cardName(candidate.getCardName())
			.cardImageUrl(candidate.getCardImageUrl())
			.status(result.status().label())
			.discountRate(result.rate())
			.nominalDiscountRate(result.nominalRate())
			.capped(result.capped())
			.discountAmount(result.discount())
			.evaluatedAmount(result.amount())
			.note(result.note())
			.build();
	}

	/**
	 * 이 카드의 올해치 혜택 소진 현황을 조회해 이번 달(월 한도/월 횟수)·올해 누적(연 횟수)
	 * 사용량으로 접는다. 지금은 결제 처리 기능이 없어 findYearlyBenefitUsage가 항상 빈
	 * 리스트를 돌려주므로, 결과적으로 항상 "미사용"으로 계산된다(db/2026-08-07_card_benefit_monthly_usage.sql 참고).
	 */
	private Map<String, BenefitUsage> loadBenefitUsage(Long userCardId) {
		int year = Year.now().getValue();
		String currentYearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);

		List<BenefitUsageVO> rows = recommendationMapper.findYearlyBenefitUsage(userCardId, year);
		if (rows.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, Long> monthlyAmount = new HashMap<>();
		Map<String, Integer> monthlyCount = new HashMap<>();
		Map<String, Integer> yearlyCount = new HashMap<>();
		for (BenefitUsageVO row : rows) {
			yearlyCount.merge(row.getServiceName(), row.getUsedCount() == null ? 0 : row.getUsedCount(), Integer::sum);
			if (currentYearMonth.equals(row.getTargetYearMonth())) {
				monthlyAmount.put(row.getServiceName(), row.getUsedAmount() == null ? 0L : row.getUsedAmount());
				monthlyCount.put(row.getServiceName(), row.getUsedCount() == null ? 0 : row.getUsedCount());
			}
		}

		Map<String, BenefitUsage> usage = new HashMap<>();
		for (String serviceName : yearlyCount.keySet()) {
			usage.put(serviceName, new BenefitUsage(
				monthlyAmount.getOrDefault(serviceName, 0L),
				monthlyCount.getOrDefault(serviceName, 0),
				yearlyCount.get(serviceName)
			));
		}
		return usage;
	}

	private String resolveCategoryCode(String categoryName) {
		return merchantCategoryService.getCategoryList().stream()
			.filter(category -> category.getCategoryName().equals(categoryName))
			.map(MerchantCategoryResponseDto::getCategoryCode)
			.findFirst()
			.orElseThrow(() -> new CategoryNotFoundException(
				"존재하지 않는 카테고리입니다: " + categoryName
			));
	}

	/**
	 * 전월 실적 조회에 사용할 연월을 yyyyMM 형태로 반환한다.
	 */
	private String getPreviousYearMonth() {
		return YearMonth.now()
			.minusMonths(1)
			.format(YEAR_MONTH_FORMATTER);
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException(
				"로그인 사용자 정보가 필요합니다."
			);
		}
	}
}