package site.benepay.domain.recommendation.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import site.benepay.domain.recommendation.dto.RecommendedCardResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.BenefitEngine;
import site.benepay.domain.recommendation.engine.BenefitJsonParser;
import site.benepay.domain.recommendation.engine.BenefitNode;
import site.benepay.domain.recommendation.engine.BenefitUsage;
import site.benepay.domain.recommendation.engine.Mode3Result;
import site.benepay.domain.recommendation.engine.PerformanceTier;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationBenefitUsageVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

	// 이번 달 확정 이득(now)과 다음 달 확률적 기대 이득(future)을 1:1로 합산한다
	// (CsvProcessing category_search.py의 evaluate_priority 기본값과 동일).
	private static final double PRIORITY_BETA = 1.0;
	// 매장 하나당 추천 카드는 총 기대 가치 상위 3장까지만 보여준다.
	private static final int TOP_CARD_LIMIT = 3;
	// "오늘의 카드 추천"에서 대표 카드 외에 "가까운 혜택 매장"을 몇 곳까지 더 보여줄지.
	private static final int TODAY_CARD_NEARBY_LIMIT = 2;
	// DB 커넥션의 serverTimezone(application.properties의 db.url)과 맞춘다 - UserServiceImpl과 동일 규약.
	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

	private final RecommendationMapper recommendationMapper;
	private final MerchantCategoryService merchantCategoryService;
	private final ObjectMapper objectMapper;
	private final RecommendationParamsLoader recommendationParamsLoader;

	@Override
	public MerchantCardRecommendationResponseDto getCardRecommendations(
		Long userId,
		Long merchantId,
		List<RecommendationCardCandidateVO> heldCards
	) {
		validateUserId(userId);

		RecommendationMerchantVO merchant =
			recommendationMapper.findMerchantForRecommendation(merchantId);

		if (merchant == null) {
			throw new MerchantNotFoundException(
				"존재하지 않는 매장입니다."
			);
		}

		List<CardBenefitComparisonResponseDto> cards =
			compareCardsForMerchant(userId, heldCards, merchant.getCategoryCode(), merchant.getMerchantName());

		return MerchantCardRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.merchantName(merchant.getMerchantName())
			.categoryCode(merchant.getCategoryCode())
			.brandId(merchant.getBrandId())
			.cards(cards)
			.build();
	}

	/**
	 * 보유 카드 전체를 이 매장의 카테고리 기준 모드 3으로 평가해, "왜 이 카드는 안 되는지"까지
	 * 볼 수 있게 total&lt;=0인 카드도 걸러내지 않고 전부 반환한다("오늘의 카드 추천"의
	 * findTopCards는 반대로 total&gt;0만 상위 3장 남긴다). 평가 대상 카드가 없거나 카테고리가
	 * 추천 분석 대상(16개 대분류) 밖이면 카드별로 benefitApplicable=false인 비교 결과를 반환한다.
	 */
	private List<CardBenefitComparisonResponseDto> compareCardsForMerchant(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		String categoryCode,
		String merchantName
	) {
		if (heldCards.isEmpty()) {
			return Collections.emptyList();
		}

		String categoryName = resolveCategoryName(categoryCode);
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers = heldCards.stream()
			.collect(Collectors.toMap(
				candidate -> candidate,
				candidate -> BenefitJsonParser.parse(candidate.getBenefitsInfo(), objectMapper)
			));

		if (categoryName == null) {
			return heldCards.stream()
				.map(candidate -> CardBenefitComparisonResponseDto.builder()
					.userCardId(candidate.getUserCardId())
					.cardName(candidate.getCardName())
					.cardImageUrl(candidate.getCardImageUrl())
					.benefitApplicable(false)
					.performanceMet(false)
					.reason("추천 분석 대상 카테고리가 아니에요")
					.recommended(false)
					.build())
				.toList();
		}

		Long typicalAmount = resolveTypicalAmount(heldCards, categoryCode, categoryName, parsedTiers);
		Map<String, Long> walletSpendHistory = aggregateWalletSpendHistory(heldCards);
		Map<Long, Map<String, BenefitUsage>> usageByCard = loadUsageByCard(userId);

		List<Map.Entry<RecommendationCardCandidateVO, Mode3Result>> evaluated = heldCards.stream()
			.map(candidate -> Map.entry(candidate, typicalAmount == null
				? new Mode3Result(0L, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0.0, "이 카테고리 통상 결제액 기준이 없어 비교할 수 없음", null)
				: scorePriority(candidate, categoryCode, merchantName, categoryName, typicalAmount,
					walletSpendHistory, parsedTiers.get(candidate),
					usageByCard.getOrDefault(candidate.getUserCardId(), Map.of()))))
			.toList();

		Long bestUserCardId = evaluated.stream()
			.filter(entry -> entry.getValue().total() > 0)
			.max(Comparator.comparingDouble(entry -> entry.getValue().total()))
			.map(entry -> entry.getKey().getUserCardId())
			.orElse(null);

		return evaluated.stream()
			.map(entry -> toComparison(entry.getKey(), entry.getValue(), categoryCode, merchantName,
				parsedTiers.get(entry.getKey()), bestUserCardId))
			.toList();
	}

	private String resolveCategoryName(String categoryCode) {
		return merchantCategoryService.getCategoryList().stream()
			.filter(category -> category.getCategoryCode().equals(categoryCode))
			.map(MerchantCategoryResponseDto::getCategoryName)
			.findFirst()
			.orElse(null);
	}

	private CardBenefitComparisonResponseDto toComparison(
		RecommendationCardCandidateVO candidate,
		Mode3Result result,
		String categoryCode,
		String merchantName,
		List<PerformanceTier> tiers,
		Long bestUserCardId
	) {
		boolean benefitApplicable =
			tiers.stream().anyMatch(tier -> !tier.benefitsForCategory(categoryCode, merchantName).isEmpty());
		boolean performanceMet = result.now() > 0;
		Long minimumPaymentAmount = tiers.stream()
			.flatMap(tier -> tier.benefitsForCategory(categoryCode, merchantName).stream())
			.map(BenefitNode::minimumPaymentAmount)
			.max(Long::compareTo)
			.orElse(null);

		// note()는 "이번 달 확정 0원 + 다음 달 기대 28원(성사확률 3% × 이득 +1,000원) = 총 28원"
		// 처럼 계산 근거를 전부 푸는 디버그용 문구라 UI에 그대로 노출하면 안 된다.
		// shortDescription()이 "카페 10% 할인 · 최대 1,000원"처럼 한 줄 노출용으로 이미
		// 정리돼 있는 값이라 이쪽을 쓴다 - performanceMet=false일 때도(실적 미달) 카드 자체엔
		// 이 카테고리 혜택이 있다는 사실은 그대로 보여준다(다음 구간 혜택으로 대신 채워짐).
		return CardBenefitComparisonResponseDto.builder()
			.userCardId(candidate.getUserCardId())
			.cardName(candidate.getCardName())
			.cardImageUrl(candidate.getCardImageUrl())
			.benefitDescription(result.shortDescription())
			.benefitApplicable(benefitApplicable)
			.performanceMet(performanceMet)
			.minimumPaymentAmount(minimumPaymentAmount)
			// benefitApplicable=false일 때만 reason을 쓴다 - 그때는 evaluatePriority가
			// hasAnywhere=false 분기를 타서 note()가 "이 카테고리 혜택 자체가 없음"처럼 항상
			// 짧고 깨끗하다(디버그 숫자 문구는 이 분기에서 절대 안 나온다). benefitApplicable=true면
			// benefitDescription이 이미 채워져 있어 reason은 화면에서 안 쓰인다.
			.reason(benefitApplicable ? null : result.note())
			.recommended(candidate.getUserCardId().equals(bestUserCardId))
			.build();
	}

	@Override
	public TodayCardRecommendationResponseDto getTodayCardRecommendation(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<MerchantResponseDto> nearbyMerchantCandidates
	) {
		validateUserId(userId);

		if (heldCards.isEmpty()) {
			return TodayCardRecommendationResponseDto.empty();
		}

		Map<String, String> categoryNames = merchantCategoryService.getCategoryList().stream()
			.collect(Collectors.toMap(
				MerchantCategoryResponseDto::getCategoryCode,
				MerchantCategoryResponseDto::getCategoryName,
				(first, ignored) -> first
			));

		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers = heldCards.stream()
			.collect(Collectors.toMap(
				candidate -> candidate,
				candidate -> BenefitJsonParser.parse(candidate.getBenefitsInfo(), objectMapper)
			));

		Map<String, Long> walletSpendHistory = aggregateWalletSpendHistory(heldCards);
		Map<Long, Map<String, BenefitUsage>> usageByCard = loadUsageByCard(userId);

		// 카테고리당 한 번만 계산해, 카드 x 카테고리 전체 조합을 평가하는 동안 재사용한다.
		Map<String, Long> typicalAmountByCategory = new HashMap<>();
		for (Map.Entry<String, String> category : categoryNames.entrySet()) {
			Long typicalAmount = resolveTypicalAmount(heldCards, category.getKey(), category.getValue(), parsedTiers);
			if (typicalAmount != null) {
				typicalAmountByCategory.put(category.getKey(), typicalAmount);
			}
		}

		WalletBestPick best = findWalletBestPick(heldCards, categoryNames, typicalAmountByCategory, walletSpendHistory,
			parsedTiers, usageByCard);

		if (best == null) {
			return TodayCardRecommendationResponseDto.empty();
		}

		List<PerformanceTier> bestCardTiers = parsedTiers.get(best.card());
		List<TodayCardRecommendationResponseDto.NearbyMerchant> nearby = nearbyMerchantCandidates.stream()
			.filter(merchant -> typicalAmountByCategory.containsKey(merchant.getCategoryCode()))
			.map(merchant -> Map.entry(merchant, scorePriority(
				best.card(), merchant.getCategoryCode(), merchant.getMerchantName(),
				categoryNames.get(merchant.getCategoryCode()),
				typicalAmountByCategory.get(merchant.getCategoryCode()), walletSpendHistory, bestCardTiers,
				usageByCard.getOrDefault(best.card().getUserCardId(), Map.of())
			)))
			.filter(entry -> entry.getValue().total() > 0)
			.sorted(Comparator.comparing(
				(Map.Entry<MerchantResponseDto, Mode3Result> entry) -> entry.getKey().getDistanceMeters(),
				Comparator.nullsLast(Long::compareTo)
			))
			.limit(TODAY_CARD_NEARBY_LIMIT)
			.map(entry -> TodayCardRecommendationResponseDto.NearbyMerchant.builder()
				.merchantId(entry.getKey().getMerchantId())
				.merchantName(entry.getKey().getMerchantName())
				.distanceMeters(entry.getKey().getDistanceMeters())
				.benefitLabel(entry.getValue().shortDescription())
				.build())
			.toList();

		return TodayCardRecommendationResponseDto.builder()
			.userCardId(best.card().getUserCardId())
			.cardName(best.card().getCardName())
			.categoryName(categoryNames.get(best.categoryCode()))
			.benefitLabel(best.result().shortDescription())
			.nearbyMerchants(nearby)
			.build();
	}

	// 보유 카드 x 추천 분석 대상 카테고리 전체 조합 중 total이 가장 큰 조합.
	private record WalletBestPick(
		RecommendationCardCandidateVO card,
		String categoryCode,
		Mode3Result result
	) {
	}

	/**
	 * 카드 하나를 카테고리 하나에 고정하지 않고, 보유 카드 x 카테고리(16개 대분류) 전체 조합을
	 * 모드 3으로 평가해 total이 가장 큰 조합 하나를 찾는다. "이 카드가 어느 카테고리에서 제일
	 * 값어치가 큰가"를 지갑 전체 기준으로 묻는 것이라, 매장(위치)과는 무관하다 - typicalAmount는
	 * compareCardsForMerchant/recommendMerchants와 같은 resolveTypicalAmount를 재사용해
	 * 기준 결제액 산정 방식을 통일한다.
	 */
	private WalletBestPick findWalletBestPick(
		List<RecommendationCardCandidateVO> heldCards,
		Map<String, String> categoryNames,
		Map<String, Long> typicalAmountByCategory,
		Map<String, Long> walletSpendHistory,
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers,
		Map<Long, Map<String, BenefitUsage>> usageByCard
	) {
		WalletBestPick best = null;
		for (RecommendationCardCandidateVO candidate : heldCards) {
			Map<String, BenefitUsage> usage = usageByCard.getOrDefault(candidate.getUserCardId(), Map.of());
			for (Map.Entry<String, Long> category : typicalAmountByCategory.entrySet()) {
				String categoryCode = category.getKey();
				// 매장이 특정되지 않은 지갑 전체 기준 계산이라 merchantName=null - MERCHANT_BRAND
				// 혜택도 "이 카테고리 어딘가에서는 유리하다"는 잠재력으로는 그대로 반영한다.
				Mode3Result result = scorePriority(candidate, categoryCode, null, categoryNames.get(categoryCode),
					category.getValue(), walletSpendHistory, parsedTiers.get(candidate), usage);
				if (result.total() > 0 && (best == null || result.total() > best.result().total())) {
					best = new WalletBestPick(candidate, categoryCode, result);
				}
			}
		}
		return best;
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

		// 카드별 이번 달/올해 혜택 소진액(card_benefit_monthly_usage) - 매장 N개를 평가하는
		// 동안 반복 조회하지 않도록 한 번만 가져온다. 한도가 이미 소진된 카드는 이 값으로
		// BenefitEngine이 잔여 한도를 계산해 실질 가치를 낮춰 반영한다.
		Map<Long, Map<String, BenefitUsage>> usageByCard = loadUsageByCard(userId);

		return merchants.stream()
			.map(merchant -> toOptimalCardRecommendation(merchant, heldCards, categoryNames, walletSpendHistory,
				parsedTiers, usageByCard))
			.toList();
	}

	private Map<Long, Map<String, BenefitUsage>> loadUsageByCard(Long userId) {
		YearMonth currentYearMonth = YearMonth.now(APP_ZONE);
		String targetYearMonth = currentYearMonth.format(YEAR_MONTH_FORMATTER);

		Map<Long, Map<String, Long>> monthlyAmount = new HashMap<>();
		Map<Long, Map<String, Integer>> monthlyCount = new HashMap<>();
		for (RecommendationBenefitUsageVO row : recommendationMapper.findMonthlyUsageByUserId(userId,
			targetYearMonth)) {
			monthlyAmount.computeIfAbsent(row.getUserCardId(), k -> new HashMap<>())
				.put(row.getBenefitServiceName(), row.getUsedAmount() == null ? 0L : row.getUsedAmount());
			monthlyCount.computeIfAbsent(row.getUserCardId(), k -> new HashMap<>())
				.put(row.getBenefitServiceName(), row.getUsedCount() == null ? 0 : row.getUsedCount());
		}

		Map<Long, Map<String, Integer>> annualCount = new HashMap<>();
		for (RecommendationBenefitUsageVO row
			: recommendationMapper.findAnnualCountByUserId(userId, currentYearMonth.getYear())) {
			annualCount.computeIfAbsent(row.getUserCardId(), k -> new HashMap<>())
				.put(row.getBenefitServiceName(), row.getUsedCount() == null ? 0 : row.getUsedCount());
		}

		Map<Long, Map<String, BenefitUsage>> usageByCard = new HashMap<>();
		for (Map.Entry<Long, Map<String, Long>> cardEntry : monthlyAmount.entrySet()) {
			Long userCardId = cardEntry.getKey();
			Map<String, Integer> counts = monthlyCount.getOrDefault(userCardId, Map.of());
			Map<String, Integer> annualCounts = annualCount.getOrDefault(userCardId, Map.of());

			Map<String, BenefitUsage> usage = new HashMap<>();
			for (Map.Entry<String, Long> serviceEntry : cardEntry.getValue().entrySet()) {
				String serviceName = serviceEntry.getKey();
				usage.put(serviceName, new BenefitUsage(
					serviceEntry.getValue(),
					counts.getOrDefault(serviceName, 0),
					annualCounts.getOrDefault(serviceName, 0)
				));
			}
			usageByCard.put(userCardId, usage);
		}
		return usageByCard;
	}

	/**
	 * 매장 하나에 대해 사용자 보유 카드 중 모드 3(우선순위) 기준 total(이번 달 확정 + beta ×
	 * 다음 달 기대)이 0보다 큰 카드를 total 내림차순으로 최대 {@value #TOP_CARD_LIMIT}장까지
	 * 골라 recommendedCards에 채운다. 필터링해서 매장을 걸러내지 않고 전달받은 매장 전부를
	 * 그대로 반환한다. 전달받은 매장 리스트를 카테고리로 미리 좁히지 않고 전부 평가하므로,
	 * 검색 카테고리 밖이라 혜택받을 수 있는 다른 매장을 놓치는 문제가 없다. distanceMeters는
	 * 입력 merchant에 이미 계산되어 있으면(위치 기반 조회, 예: 오늘의 추천) 그대로 넘기고,
	 * bounds 조회처럼 거리 개념이 없으면 null을 그대로 넘긴다.
	 *
	 * benefitAvailable(지도 핀 강조 여부)은 recommendedCards가 비었는지가 아니라, 그중
	 * now(이번 달 확정 혜택)가 0보다 큰 카드가 하나라도 있는지로 판단한다. total은 future
	 * (다음 달 실적 구간을 채웠을 때의 기대값)까지 섞여 있어서, 지금 당장은 혜택이 0원이어도
	 * "다음 달에 실적 채우면 받을 수도 있다"는 이유만으로 total>0이 되는 카드가 많다(카드의
	 * 70%가 기본 구간엔 혜택이 아예 없어 흔한 케이스다) - 그 기준을 그대로 쓰면 매장 상세에서
	 * "지금 적용되는 혜택 없음"이 뜨는데도 지도에서는 골드 핀(혜택 매장)으로 뜨는 불일치가
	 * 생긴다. total 랭킹 자체는 카드 추천 순서로는 여전히 유효해서 recommendedCards 구성에는
	 * 그대로 쓰고, "지금 여기서 받을 수 있는 혜택이 있는가"를 뜻하는 배지 표시만 now 기준으로
	 * 바꾼다.
	 */
	private NearbyMerchantRecommendationResponseDto toOptimalCardRecommendation(
		MerchantResponseDto merchant,
		List<RecommendationCardCandidateVO> heldCards,
		Map<String, String> categoryNames,
		Map<String, Long> walletSpendHistory,
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers,
		Map<Long, Map<String, BenefitUsage>> usageByCard
	) {
		String categoryName = categoryNames.get(merchant.getCategoryCode());
		TopCardsResult topCardsResult = categoryName == null
			? TopCardsResult.EMPTY
			: findTopCards(heldCards, merchant.getCategoryCode(), merchant.getMerchantName(), categoryName,
			walletSpendHistory, parsedTiers, usageByCard);

		// note()는 계산 근거를 전부 푸는 디버그용 문구라 UI에 그대로 노출하면 안 된다 -
		// shortDescription()이 "카페 10% 할인 · 최대 1,000원"처럼 한 줄 노출용으로 정리된 값이다.
		List<RecommendedCardResponseDto> recommendedCards = topCardsResult.cards().stream()
			.map(entry -> RecommendedCardResponseDto.builder()
				.userCardId(entry.getKey().getUserCardId())
				.cardName(entry.getKey().getCardName())
				.benefitSummary(entry.getValue().shortDescription())
				.build())
			.toList();

		boolean benefitAvailableNow = topCardsResult.cards().stream()
			.anyMatch(entry -> entry.getValue().now() > 0);

		return NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(merchant.getMerchantId())
			.categoryCode(merchant.getCategoryCode())
			.categoryName(categoryName)
			.brandId(merchant.getBrandId())
			.merchantCode(merchant.getMerchantCode())
			.merchantName(merchant.getMerchantName())
			.address(merchant.getAddress())
			.latitude(merchant.getLatitude())
			.longitude(merchant.getLongitude())
			.distanceMeters(merchant.getDistanceMeters())
			.benefitAvailable(benefitAvailableNow)
			.recommendedCards(recommendedCards)
			.typicalPaymentAmount(recommendedCards.isEmpty() ? null : topCardsResult.typicalAmount())
			.build();
	}

	// findTopCards 결과와, 그 카드들을 비교할 때 쓴 기준 결제액(typicalAmount)을 함께 담는다 -
	// 기준 금액은 응답 DTO의 "n원 기준" 표시에도 그대로 쓰여야 하므로 카드 목록과 분리해 반환한다.
	private record TopCardsResult(
		Long typicalAmount,
		List<Map.Entry<RecommendationCardCandidateVO, Mode3Result>> cards
	) {
		private static final TopCardsResult EMPTY = new TopCardsResult(null, List.of());
	}

	private TopCardsResult findTopCards(
		List<RecommendationCardCandidateVO> heldCards,
		String categoryCode,
		String merchantName,
		String categoryName,
		Map<String, Long> walletSpendHistory,
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers,
		Map<Long, Map<String, BenefitUsage>> usageByCard
	) {
		if (heldCards.isEmpty()) {
			return TopCardsResult.EMPTY;
		}

		Long typicalAmount = resolveTypicalAmount(heldCards, categoryCode, categoryName, parsedTiers);
		if (typicalAmount == null) {
			return TopCardsResult.EMPTY;
		}

		List<Map.Entry<RecommendationCardCandidateVO, Mode3Result>> cards = heldCards.stream()
			.map(candidate -> Map.entry(
				candidate,
				scorePriority(candidate, categoryCode, merchantName, categoryName, typicalAmount, walletSpendHistory,
					parsedTiers.get(candidate),
					usageByCard.getOrDefault(candidate.getUserCardId(), Map.of()))
			))
			.filter(entry -> entry.getValue().total() > 0)
			.sorted(Comparator.<Map.Entry<RecommendationCardCandidateVO, Mode3Result>>comparingDouble(
				e -> e.getValue().total()).reversed())
			.limit(TOP_CARD_LIMIT)
			.toList();

		return new TopCardsResult(typicalAmount, cards);
	}

	/**
	 * 이 카테고리에서 카드 비교에 쓸 기준 결제액(ticket)을 정한다. 비교 대상 카드들이 이
	 * 카테고리에 가진 혜택의 최소결제금액 중 가장 큰 값을 우선 쓰고, 전부 최소결제금액이
	 * 없으면(전부 0) 카테고리 통상결제액(params.json)을 천원 단위로 반올림해 대신 쓴다.
	 * 매장(카테고리) 하나당 한 번만 계산해 비교 대상 카드 전체에 동일하게 적용한다 -
	 * 그래야 정률 할인과 건당 정액 할인이 같은 기준 금액에서 공정하게 비교된다.
	 */
	private Long resolveTypicalAmount(
		List<RecommendationCardCandidateVO> heldCards,
		String categoryCode,
		String categoryName,
		Map<RecommendationCardCandidateVO, List<PerformanceTier>> parsedTiers
	) {
		long maxMinimumPaymentAmount = heldCards.stream()
			.flatMap(candidate -> parsedTiers.get(candidate).stream())
			.flatMap(tier -> tier.benefitsForCategory(categoryCode).stream())
			.mapToLong(BenefitNode::minimumPaymentAmount)
			.max()
			.orElse(0L);

		if (maxMinimumPaymentAmount > 0) {
			return maxMinimumPaymentAmount;
		}

		Long typicalPaymentAmount = recommendationParamsLoader.params().typicalPaymentAmount().get(categoryName);
		return typicalPaymentAmount == null ? null : roundToNearestThousand(typicalPaymentAmount);
	}

	private static long roundToNearestThousand(long amount) {
		return Math.round(amount / 1000.0) * 1000;
	}

	/**
	 * 카드 한 장에 대해 모드 3을 평가한다. prevMonthSpend(activeTier 기준)는 이 카드
	 * spendHistory의 가장 최신 달 값이고, currentMonthSpend(baselineTier/nextTier 기준)는
	 * 진행 중인 이번 달 누적이다. merchantName은 MERCHANT_BRAND 혜택을 거르는 데 쓰인다 -
	 * 특정 매장이 없는 지갑 전체 기준 호출(findWalletBestPick)이면 null을 넘긴다.
	 */
	private Mode3Result scorePriority(
		RecommendationCardCandidateVO candidate,
		String categoryCode,
		String merchantName,
		String categoryName,
		long typicalAmount,
		Map<String, Long> walletSpendHistory,
		List<PerformanceTier> tiers,
		Map<String, BenefitUsage> usageByServiceName
	) {
		Map<String, Long> spendHistory =
			candidate.getSpendHistory() == null ? Collections.emptyMap() : candidate.getSpendHistory();
		long prevMonthSpend = spendHistory.isEmpty() ? 0L : spendHistory.get(Collections.max(spendHistory.keySet()));
		long currentMonthSpend = candidate.getCurrentMonthSpend() == null ? 0L : candidate.getCurrentMonthSpend();

		return BenefitEngine.evaluatePriority(
			tiers, prevMonthSpend, currentMonthSpend, categoryCode, merchantName, categoryName, typicalAmount,
			spendHistory, walletSpendHistory, recommendationParamsLoader.params(), LocalDate.now(APP_ZONE),
			PRIORITY_BETA, usageByServiceName
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
