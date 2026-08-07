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

import com.fasterxml.jackson.databind.JsonNode;
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
		double latitude,
		double longitude,
		double radiusMeters
	) {
		validateUserId(userId);

		if (radiusMeters <= 0) {
			throw new IllegalArgumentException(
				"radiusMeters는 0보다 커야 합니다."
			);
		}

		String targetYearMonth = getPreviousYearMonth();

		// 사용자의 활성 상태이면서 추천이 허용된 보유 카드를 조회한다.
		List<RecommendationCardCandidateVO> candidates =
			recommendationMapper.findRecommendationCardCandidates(
				userId,
				targetYearMonth
			);

		if (candidates.isEmpty()) {
			return Collections.emptyList();
		}

		// 카테고리 코드를 응답용 카테고리 이름으로 변환하기 위한 Map이다.
		Map<String, String> categoryNames =
			merchantCategoryService.getCategoryList().stream()
				.collect(Collectors.toMap(
					MerchantCategoryResponseDto::getCategoryCode,
					MerchantCategoryResponseDto::getCategoryName,
					(first, ignored) -> first
				));

		/*
		 * MerchantService에서 반경 내 매장 후보를 조회한다.
		 *
		 * MerchantController의 API를 HTTP로 호출하는 것이 아니라,
		 * 같은 백엔드 안에 있는 MerchantService 메서드를 직접 호출한다.
		 *
		 * 이후 RecommendationService에서 사용자의 카드 혜택을
		 * 받을 수 있는 매장만 다시 추려낸다.
		 */
		return merchantService.getNearbyMerchants(
				latitude,
				longitude,
				radiusMeters
			).stream()
			.map(merchant -> toNearbyRecommendation(
				merchant,
				candidates,
				categoryNames
			))
			.flatMap(Optional::stream)
			.collect(Collectors.toList());
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

		List<CardBenefitScoreDto> cards = candidates.stream()
			.map(candidate -> Map.entry(candidate, scoreCandidate(candidate, categoryCode, categoryName, typicalAmount)))
			// NOW_STATES 순서(즉시할인 -> 조건부할인 -> ... 열거 순서와 동일) -> 그룹 내 할인율 내림차순
			.sorted(Comparator
				.<Map.Entry<RecommendationCardCandidateVO, Mode1Result>>comparingInt(e -> e.getValue().status().ordinal())
				.thenComparing(e -> -e.getValue().rate()))
			.map(e -> toDto(e.getKey(), e.getValue()))
			.collect(Collectors.toList());

		return CategoryCardRecommendationResponseDto.builder()
			.categoryName(categoryName)
			.typicalPaymentAmount(typicalAmount)
			.cards(cards)
			.build();
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
	 * 주변 매장 하나가 사용자의 카드 혜택 대상인지 확인하고
	 * 혜택 대상이라면 주변 추천 매장 응답으로 변환한다.
	 */
	private Optional<NearbyMerchantRecommendationResponseDto> toNearbyRecommendation(
		NearbyMerchantResponseDto nearbyMerchant,
		List<RecommendationCardCandidateVO> candidates,
		Map<String, String> categoryNames
	) {
		RecommendationMerchantVO merchant =
			toRecommendationMerchant(nearbyMerchant);

		/*
		 * 여기서는 최적 카드를 고르지 않는다.
		 *
		 * 해당 매장에서 적용 가능한 카드 혜택이
		 * 하나라도 존재하는지만 확인한다.
		 */
		Optional<ApplicableBenefit> applicableBenefit =
			candidates.stream()
				.map(candidate ->
					findApplicableBenefit(candidate, merchant)
				)
				.flatMap(Optional::stream)
				.findFirst();

		// 모든 보유 카드를 확인했지만 적용 가능한 혜택이 없다면 제외한다.
		if (applicableBenefit.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(
			NearbyMerchantRecommendationResponseDto.builder()
				.merchantId(nearbyMerchant.getMerchantId())
				.merchantName(nearbyMerchant.getMerchantName())
				.categoryName(categoryNames.getOrDefault(
					nearbyMerchant.getCategoryCode(),
					nearbyMerchant.getCategoryCode()
				))
				.latitude(
					nearbyMerchant.getLatitude() == null
						? null
						: nearbyMerchant.getLatitude().doubleValue()
				)
				.longitude(
					nearbyMerchant.getLongitude() == null
						? null
						: nearbyMerchant.getLongitude().doubleValue()
				)
				.distanceMeters(nearbyMerchant.getDistanceMeters())
				.benefitSummary(
					applicableBenefit.get().description()
				)

				/*
				 * TODO: 다른 팀원의 추천 알고리즘 연결 후
				 * 이 매장의 추천 카드명을 설정한다.
				 *
				 * 현재는 최적 카드를 직접 선택하지 않으므로
				 * null로 반환한다.
				 */
				.recommendedCardName(null)
				.build()
		);
	}

	/**
	 * 카드의 혜택 JSON에서 현재 매장에 적용되는 혜택을 찾는다.
	 */
	private Optional<ApplicableBenefit> findApplicableBenefit(
		RecommendationCardCandidateVO card,
		RecommendationMerchantVO merchant
	) {
		if (
			card.getBenefitsInfo() == null
				|| card.getBenefitsInfo().isBlank()
		) {
			return Optional.empty();
		}

		try {
			JsonNode performanceTiers = objectMapper
				.readTree(card.getBenefitsInfo())
				.path("performanceTiers");

			if (!performanceTiers.isArray()) {
				return Optional.empty();
			}

			long spending =
				card.getTotalSpendingAmount() == null
					? 0L
					: card.getTotalSpendingAmount();

			for (JsonNode tier : performanceTiers) {
				if (!isPerformanceTierApplicable(tier, spending)) {
					continue;
				}

				for (JsonNode benefit : tier.path("benefits")) {
					if (matchesMerchant(benefit, merchant)) {
						return Optional.of(
							new ApplicableBenefit(
								getBenefitDescription(benefit)
							)
						);
					}
				}
			}
		} catch (Exception exception) {
			/*
			 * 카드 혜택 JSON 형식이 잘못된 카드는
			 * 주변 혜택 매장 판정에서 제외한다.
			 */
			return Optional.empty();
		}

		return Optional.empty();
	}

	/**
	 * 사용자의 전월 실적이 카드 혜택 구간에 포함되는지 확인한다.
	 *
	 * minimumSpending <= spending
	 * spending < maximumSpending
	 */
	private boolean isPerformanceTierApplicable(
		JsonNode tier,
		long spending
	) {
		long minimumSpending =
			tier.path("minimumSpending").asLong(0L);

		JsonNode maximumSpendingNode =
			tier.get("maximumSpending");

		boolean belowMaximum =
			maximumSpendingNode == null
				|| maximumSpendingNode.isNull()
				|| spending < maximumSpendingNode.asLong();

		return minimumSpending <= spending && belowMaximum;
	}

	/**
	 * 카드 혜택 조건이 현재 매장의 업종 또는 브랜드 조건과
	 * 일치하는지 확인한다.
	 */
	private boolean matchesMerchant(
		JsonNode benefit,
		RecommendationMerchantVO merchant
	) {
		// 모든 가맹점 대상 혜택이면 바로 적용한다.
		if (
			"ALL_MERCHANTS".equals(
				benefit.path("benefitType").asText()
			)
		) {
			return true;
		}

		boolean hasCategoryCondition =
			benefit.hasNonNull("categoryCode")
				|| hasArrayValues(
				benefit.path("categoryCodes")
			);

		boolean categoryMatched =
			textEquals(
				benefit.get("categoryCode"),
				merchant.getCategoryCode()
			)
				|| arrayContainsText(
				benefit.path("categoryCodes"),
				merchant.getCategoryCode()
			);

		boolean hasMerchantCondition =
			benefit.hasNonNull("brandId")
				|| hasArrayValues(
				benefit.path("brandIds")
			)
				|| hasArrayValues(
				benefit.path("merchantNames")
			)
				|| hasArrayValues(
				benefit.path("merchants")
			);

		boolean merchantMatched =
			numberEquals(
				benefit.get("brandId"),
				merchant.getBrandId()
			)
				|| arrayContainsNumber(
				benefit.path("brandIds"),
				merchant.getBrandId()
			)
				|| arrayContainsMerchantName(
				benefit.path("merchantNames"),
				merchant.getMerchantName()
			)
				|| arrayContainsMerchantName(
				benefit.path("merchants"),
				merchant.getMerchantName()
			);

		/*
		 * 업종 조건도 없고 매장 조건도 없는 혜택은
		 * 현재 매장에 적용되는 혜택이라고 판단하지 않는다.
		 */
		if (!hasCategoryCondition && !hasMerchantCondition) {
			return false;
		}

		/*
		 * 업종 조건이 있으면 업종이 일치해야 하고,
		 * 매장 조건이 있으면 매장도 일치해야 한다.
		 */
		return (!hasCategoryCondition || categoryMatched)
			&& (!hasMerchantCondition || merchantMatched);
	}

	/**
	 * Merchant 도메인의 주변 매장 DTO를
	 * Recommendation 도메인에서 사용하는 VO로 변환한다.
	 */
	private RecommendationMerchantVO toRecommendationMerchant(
		NearbyMerchantResponseDto nearbyMerchant
	) {
		RecommendationMerchantVO merchant =
			new RecommendationMerchantVO();

		merchant.setMerchantId(
			nearbyMerchant.getMerchantId()
		);
		merchant.setMerchantName(
			nearbyMerchant.getMerchantName()
		);
		merchant.setCategoryCode(
			nearbyMerchant.getCategoryCode()
		);
		merchant.setBrandId(
			nearbyMerchant.getBrandId()
		);

		return merchant;
	}

	private boolean hasArrayValues(JsonNode node) {
		return node.isArray() && !node.isEmpty();
	}

	private boolean textEquals(
		JsonNode node,
		String value
	) {
		return node != null
			&& !node.isNull()
			&& value != null
			&& value.equals(node.asText());
	}

	private boolean arrayContainsText(
		JsonNode arrayNode,
		String value
	) {
		if (!arrayNode.isArray() || value == null) {
			return false;
		}

		for (JsonNode node : arrayNode) {
			if (value.equals(node.asText())) {
				return true;
			}
		}

		return false;
	}

	private boolean numberEquals(
		JsonNode node,
		Long value
	) {
		return node != null
			&& !node.isNull()
			&& value != null
			&& value.equals(node.asLong());
	}

	private boolean arrayContainsNumber(
		JsonNode arrayNode,
		Long value
	) {
		if (!arrayNode.isArray() || value == null) {
			return false;
		}

		for (JsonNode node : arrayNode) {
			if (value.equals(node.asLong())) {
				return true;
			}
		}

		return false;
	}

	private boolean arrayContainsMerchantName(
		JsonNode arrayNode,
		String merchantName
	) {
		if (!arrayNode.isArray() || merchantName == null) {
			return false;
		}

		for (JsonNode node : arrayNode) {
			String benefitMerchantName = node.asText();

			if (
				!benefitMerchantName.isBlank()
					&& merchantName.contains(
					benefitMerchantName
				)
			) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 카드 혜택 설명을 주변 매장 응답의 혜택 요약으로 사용한다.
	 */
	private String getBenefitDescription(JsonNode benefit) {
		String description =
			benefit.path("description").asText();

		if (!description.isBlank()) {
			return description;
		}

		String categoryName =
			benefit.path("categoryName").asText("카드");

		return categoryName + " 혜택";
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

	/**
	 * 주변 매장 필터링 과정에서 발견한 적용 가능 혜택이다.
	 *
	 * 최적 카드나 추천 점수를 나타내는 객체가 아니다.
	 */
	private record ApplicableBenefit(
		String description
	) {
	}
}