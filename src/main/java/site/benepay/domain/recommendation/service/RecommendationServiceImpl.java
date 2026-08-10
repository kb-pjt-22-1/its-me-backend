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
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.dto.CardBenefitComparisonResponseDto;
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
	public List<NearbyMerchantRecommendationResponseDto> recommendMerchants(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<NearbyMerchantResponseDto> merchants
	) {
		validateUserId(userId);

		if (heldCards.isEmpty() || merchants.isEmpty()) {
			return Collections.emptyList();
		}

		// 카테고리 코드를 응답용 카테고리 이름 + typicalPaymentAmount 조회 키로 쓰기 위한 Map이다.
		Map<String, String> categoryNames = merchantCategoryService.getCategoryList().stream()
			.collect(Collectors.toMap(
				MerchantCategoryResponseDto::getCategoryCode,
				MerchantCategoryResponseDto::getCategoryName,
				(first, ignored) -> first
			));

		return merchants.stream()
			.map(merchant -> toOptimalCardRecommendation(merchant, heldCards, categoryNames))
			.flatMap(Optional::stream)
			.collect(Collectors.toList());
	}

	/**
	 * 매장 하나에 대해 사용자 보유 카드 중 최적 카드를 고르고(scoreAndRank 재사용), 그 카드가
	 * 이 매장 카테고리에서 지금 당장(즉시할인) 혜택을 주는 경우에만 결과에 포함시킨다. 카테고리가
	 * 추천 분석 대상(16개 대분류) 밖이면 이 매장은 건너뛴다. 전달받은 매장 리스트를 카테고리로
	 * 미리 좁히지 않고 전부 평가하므로, 검색 카테고리 밖이라 혜택받을 수 있는 다른 매장을 놓치는
	 * 문제가 없다. 위치 기반 조회라 거리 개념이 없어 distanceMeters는 항상 null이다.
	 */
	private Optional<NearbyMerchantRecommendationResponseDto> toOptimalCardRecommendation(
		NearbyMerchantResponseDto merchant,
		List<RecommendationCardCandidateVO> heldCards,
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
			scoreAndRank(heldCards, merchant.getCategoryCode(), categoryName, typicalAmount);

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

	/**
	 * 카드 후보 전체를 이 카테고리 기준으로 평가해 상태(즉시할인 -> 조건부할인 -> ...) -> 할인율
	 * 내림차순으로 정렬한다.
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

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException(
				"로그인 사용자 정보가 필요합니다."
			);
		}
	}
}