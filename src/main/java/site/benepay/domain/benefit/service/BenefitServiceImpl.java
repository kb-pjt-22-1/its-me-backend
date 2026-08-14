package site.benepay.domain.benefit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.common.exception.InvalidBenefitPeriodException;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto.MonthlyBenefitDto;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.CalculatedCoachingData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.CardData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.MonthlyUsageData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.PaymentData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.SpendingPatternData;
import site.benepay.domain.benefit.dto.BenefitCoachResponseDto;
import site.benepay.domain.benefit.dto.BenefitCoachResponseDto.BenefitCoachItemDto;
import site.benepay.domain.benefit.dto.CategoryBenefitStatusResponseDto;
import site.benepay.domain.benefit.dto.DailyBenefitAmountDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto.CategoryBenefitDto;
import site.benepay.domain.benefit.mapper.BenefitMapper;
import site.benepay.domain.benefit.service.BenefitCoachDataLoader.LoadedCoachingData;
import site.benepay.domain.benefit.service.OpenAiClient.OpenAiCoachingItemText;
import site.benepay.domain.benefit.service.OpenAiClient.OpenAiCoachingText;
import site.benepay.domain.benefit.vo.CategoryBenefitUsageVO;
import site.benepay.domain.benefit.vo.HeldCardBenefitVO;
import site.benepay.domain.benefit.vo.MonthlyCategoryBenefitVO;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.engine.BenefitEngine;
import site.benepay.domain.recommendation.engine.BenefitJsonParser;
import site.benepay.domain.recommendation.engine.BenefitNode;
import site.benepay.domain.recommendation.engine.PerformanceTier;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BenefitServiceImpl implements BenefitService {

	private static final ZoneId ZONE =
		ZoneId.of("Asia/Seoul");

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("uuuuMM");

	private static final long MINIMUM_SWITCH_SAVING_AMOUNT = 500L;

	private final BenefitMapper benefitMapper;
	private final MerchantCategoryService merchantCategoryService;
	private final ObjectMapper objectMapper;
	private final RecommendationParamsLoader recommendationParamsLoader;
	private final OpenAiClient openAiClient;
	private final BenefitCoachDataLoader benefitCoachDataLoader;

	@Override
	public List<AnnualFeeBreakEvenResponseDto> getAnnualFeeBreakEven(
		Long userId,
		int year
	) {
		LocalDateTime now =
			LocalDateTime.now(ZONE);

		validateYear(year, now.getYear());

		LocalDateTime startPaymentTime =
			LocalDate.of(year, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			calculateEndPaymentTime(year, now);

		List<DailyBenefitAmountDto> rows =
			benefitMapper.findAnnualFeeBenefitsByUserId(
				userId,
				startPaymentTime,
				endPaymentTime
			);

		Map<Long, List<DailyBenefitAmountDto>> rowsByUserCardId =
			groupByUserCardId(rows);

		List<AnnualFeeBreakEvenResponseDto> responses =
			new ArrayList<>();

		for (List<DailyBenefitAmountDto> cardRows
			: rowsByUserCardId.values()) {

			responses.add(
				createResponse(
					year,
					now,
					cardRows
				)
			);
		}

		return responses;
	}

	/**
	 * 조회 결과를 사용자 보유 카드 단위로 묶는다.
	 *
	 * <p>LinkedHashMap을 사용해 SQL에서 정렬한
	 * 대표카드 우선순위를 그대로 유지한다.</p>
	 */
	private Map<Long, List<DailyBenefitAmountDto>> groupByUserCardId(
		List<DailyBenefitAmountDto> rows
	) {
		Map<Long, List<DailyBenefitAmountDto>> groupedRows =
			new LinkedHashMap<>();

		for (DailyBenefitAmountDto row : rows) {
			groupedRows
				.computeIfAbsent(
					row.getUserCardId(),
					key -> new ArrayList<>()
				)
				.add(row);
		}

		return groupedRows;
	}

	/**
	 * 카드 한 장의 연회비 본전 현황을 계산한다.
	 */
	private AnnualFeeBreakEvenResponseDto createResponse(
		int year,
		LocalDateTime now,
		List<DailyBenefitAmountDto> cardRows
	) {
		DailyBenefitAmountDto card =
			cardRows.get(0);

		long annualFee =
			card.getAnnualFee();

		long accumulatedBenefit = 0L;
		LocalDate breakEvenDate = null;

		Map<YearMonth, Long> monthlyBenefitAmounts =
			new LinkedHashMap<>();

		/*
		 * SQL 결과가 결제일 오름차순으로 정렬되어 있으므로,
		 * 누적 혜택이 처음 연회비 이상이 된 날짜를
		 * 본전 달성일로 사용할 수 있다.
		 */
		for (DailyBenefitAmountDto row : cardRows) {
			if (row.getBenefitDate() == null) {
				continue;
			}

			long dailyBenefit =
				row.getDailyBenefitAmount() == null
					? 0L
					: row.getDailyBenefitAmount().longValue();

			accumulatedBenefit += dailyBenefit;

			YearMonth benefitMonth =
				YearMonth.from(row.getBenefitDate());

			monthlyBenefitAmounts.merge(
				benefitMonth,
				dailyBenefit,
				Long::sum
			);

			if (breakEvenDate == null
				&& accumulatedBenefit >= annualFee) {

				breakEvenDate =
					row.getBenefitDate();
			}
		}

		long netBenefit =
			accumulatedBenefit - annualFee;

		long remainingAmount =
			Math.max(
				annualFee - accumulatedBenefit,
				0L
			);

		return AnnualFeeBreakEvenResponseDto.builder()
			.userCardId(card.getUserCardId())
			.cardId(card.getCardId())
			.cardName(card.getCardName())
			.cardImageUrl(card.getCardImageUrl())
			.panLast4(card.getPanLast4())
			.baseYear(year)
			.annualFee(annualFee)
			.accumulatedBenefit(accumulatedBenefit)
			.netBenefit(netBenefit)
			.remainingAmount(remainingAmount)
			.breakEvenAchieved(breakEvenDate != null)
			.breakEvenDate(breakEvenDate)
			.monthlyBenefits(
				createMonthlyBenefits(
					year,
					now,
					monthlyBenefitAmounts
				)
			)
			.build();
	}

	/**
	 * 그래프에서 사용할 월별 혜택과 누적 혜택을 생성한다.
	 *
	 * <p>현재 연도는 현재 월까지만 반환하고,
	 * 과거 연도는 12월까지 반환한다.</p>
	 */
	private List<MonthlyBenefitDto> createMonthlyBenefits(
		int year,
		LocalDateTime now,
		Map<YearMonth, Long> monthlyBenefitAmounts
	) {
		int lastMonth =
			year == now.getYear()
				? now.getMonthValue()
				: 12;

		List<MonthlyBenefitDto> monthlyBenefits =
			new ArrayList<>();

		long accumulatedAmount = 0L;

		for (int month = 1; month <= lastMonth; month++) {
			YearMonth yearMonth =
				YearMonth.of(year, month);

			long monthlyAmount =
				monthlyBenefitAmounts.getOrDefault(
					yearMonth,
					0L
				);

			accumulatedAmount += monthlyAmount;

			monthlyBenefits.add(
				MonthlyBenefitDto.builder()
					.yearMonth(yearMonth.toString())
					.monthlyBenefitAmount(monthlyAmount)
					.accumulatedBenefitAmount(
						accumulatedAmount
					)
					.build()
			);
		}

		return monthlyBenefits;
	}

	/**
	 * 현재 연도는 현재 시각까지만 조회한다.
	 *
	 * <p>현재 날짜보다 미래인 결제 데이터가
	 * 혜택 금액에 포함되는 것을 막는다.</p>
	 *
	 * <p>과거 연도는 다음 연도 1월 1일 전까지 조회한다.</p>
	 */
	private LocalDateTime calculateEndPaymentTime(
		int year,
		LocalDateTime now
	) {
		if (year == now.getYear()) {
			return now;
		}

		return LocalDate.of(year + 1, 1, 1)
			.atStartOfDay();
	}

	/**
	 * 미래 연도나 지나치게 오래된 연도 조회를 막는다.
	 */
	private void validateYear(
		int year,
		int currentYear
	) {
		if (year < 2000 || year > currentYear) {
			throw new InvalidBenefitPeriodException(
				"year는 2000년부터 현재 연도 사이여야 합니다."
			);
		}
	}

	@Override
	public MonthlyBenefitReportResponseDto getMonthlyBenefitReport(
		Long userId,
		String yearMonth
	) {
		YearMonth targetYearMonth =
			parseYearMonth(yearMonth);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		List<MonthlyCategoryBenefitVO> categoryRows =
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				userId,
				startOfMonth(targetYearMonth),
				startOfMonth(
					targetYearMonth.plusMonths(1)
				)
			);

		long totalBenefitAmount =
			sumCategoryAmounts(categoryRows);

		long previousTotalBenefitAmount =
			sumCategoryBenefits(
				userId,
				previousYearMonth
			);

		return MonthlyBenefitReportResponseDto.builder()
			.yearMonth(targetYearMonth.toString())
			.totalBenefitAmount(totalBenefitAmount)
			.deltaVsLastMonth(
				totalBenefitAmount
					- previousTotalBenefitAmount
			)
			.categoryBreakdown(
				createCategoryBreakdown(
					categoryRows,
					totalBenefitAmount
				)
			)
			.build();
	}

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public BenefitCoachResponseDto getBenefitCoaching(
		Long userId
	) {
		LocalDateTime now =
			LocalDateTime.now(ZONE);

		LoadedCoachingData loadedData =
			benefitCoachDataLoader.load(userId, now);

		if (loadedData.payments().isEmpty()) {
			return createEmptyCoachingResponse(
				"최근 3개월 결제 내역이 없어 분석할 소비 패턴이 없습니다."
			);
		}

		List<SpendingPatternData> patterns =
			createSpendingPatterns(
				loadedData.payments()
			);

		if (loadedData.cards().isEmpty()) {
			return createEmptyCoachingResponse(
				"혜택 코칭에 사용할 수 있는 보유 카드가 없습니다."
			);
		}

		List<CalculatedCoachingData> calculatedData =
			calculateCoachingData(
				patterns,
				loadedData.cards(),
				loadedData.monthlyUsages()
			);

		if (calculatedData.isEmpty()) {
			return createEmptyCoachingResponse(
				"현재 소비 패턴에 적용 가능한 카드 혜택이 없습니다."
			);
		}

		/*
		 * DataLoader의 읽기 전용 트랜잭션이 종료된 뒤
		 * 외부 OpenAI API를 호출한다.
		 */
		OpenAiCoachingText coachingText;

		try {
			coachingText =
				openAiClient.generateCoachingText(
					calculatedData
				);
		} catch (IllegalStateException e) {
			log.warn(
				"OpenAI 코칭 텍스트 생성에 실패하여 기본 문구로 대체합니다.",
				e
			);

			coachingText =
				new OpenAiCoachingText(
					"이번 주 카드 사용 전략을 준비했어요.",
					List.of()
				);
		}

		return BenefitCoachResponseDto.builder()
			.summary(coachingText.summary())
			.items(
				createBenefitCoachItems(
					calculatedData,
					coachingText.items()
				)
			)
			.build();
	}

	private BenefitCoachResponseDto createEmptyCoachingResponse(
		String summary
	) {
		return BenefitCoachResponseDto.builder()
			.summary(summary)
			.items(List.of())
			.build();
	}

	private List<BenefitCoachItemDto> createBenefitCoachItems(
		List<CalculatedCoachingData> calculatedData,
		List<OpenAiCoachingItemText> coachingTexts
	) {
		Map<Integer, OpenAiCoachingItemText> coachingTextByIndex =
			new LinkedHashMap<>();

		for (OpenAiCoachingItemText coachingText
			: coachingTexts) {

			if (coachingText.index() >= 0
				&& coachingText.index()
				< calculatedData.size()) {

				coachingTextByIndex.putIfAbsent(
					coachingText.index(),
					coachingText
				);
			}
		}

		List<BenefitCoachItemDto> items =
			new ArrayList<>();

		for (int index = 0;
		     index < calculatedData.size();
		     index++) {

			CalculatedCoachingData data =
				calculatedData.get(index);

			OpenAiCoachingItemText coachingText =
				coachingTextByIndex.get(index);

			String title =
				coachingText == null
					|| coachingText.title().isBlank()
					? data.getCategoryName() + " 혜택 안내"
					: coachingText.title();

			String message =
				coachingText == null
					|| coachingText.message().isBlank()
					? data.getRecommendedCardName()
					  + " 사용이 유리합니다."
					: coachingText.message();

			items.add(
				BenefitCoachItemDto.builder()
					.title(title)
					.message(message)
					.recommendedCardName(
						data.getRecommendedCardName()
					)
					.expectedSavingAmount(
						data.getExpectedSavingAmount()
					)
					.strategyType(
						data.getStrategyType()
					)
					.nextRecommendedCardName(
						data.getNextRecommendedCardName()
					)
					.remainingUsageCount(
						data.getRemainingUsageCount()
					)
					.expectedAdditionalSavingAmount(
						data.getExpectedAdditionalSavingAmount()
					)
					.reason(data.getReason())
					.build()
			);
		}

		return items;
	}

	private long sumCategoryBenefits(
		Long userId,
		YearMonth yearMonth
	) {
		List<MonthlyCategoryBenefitVO> rows =
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				userId,
				startOfMonth(yearMonth),
				startOfMonth(
					yearMonth.plusMonths(1)
				)
			);

		return sumCategoryAmounts(rows);
	}

	private long sumCategoryAmounts(
		List<MonthlyCategoryBenefitVO> rows
	) {
		return rows.stream()
			.mapToLong(
				row -> row.getCategoryAmount() == null
					? 0L
					: row.getCategoryAmount()
			)
			.sum();
	}

	private CalculatedCoachingData createCalculatedCoachingData(
		SpendingPatternData pattern,
		CardBenefitEvaluation evaluation,
		StrategyDecision strategy
	) {
		CardData card = evaluation.card();
		PerformanceTier tier = evaluation.tier();
		BenefitNode benefit = evaluation.benefit();

		long previousMonthSpendingAmount =
			card.getPreviousMonthSpendingAmount() == null
				? 0L
				: card.getPreviousMonthSpendingAmount();

		String condition = String.format(
			"%s, 전월 실적 %,d원, %s, 최소 결제금액 %,d원",
			tier.tierName(),
			previousMonthSpendingAmount,
			benefit.serviceName(),
			benefit.minimumPaymentAmount()
		);

		return CalculatedCoachingData.builder()
			.categoryCode(pattern.getCategoryCode())
			.categoryName(pattern.getCategoryName())
			.usualDayOfWeek(pattern.getUsualDayOfWeek())
			.usualMerchantName(pattern.getUsualMerchantName())
			.averageAmount(pattern.getAverageAmount())
			.paymentCount(pattern.getPaymentCount())
			.userCardId(card.getUserCardId())
			.recommendedCardName(card.getCardName())
			.expectedSavingAmount(
				evaluation.expectedSavingAmount()
			)
			.strategyType(strategy.strategyType())
			.nextRecommendedCardName(
				strategy.nextRecommendedCardName()
			)
			.remainingUsageCount(
				strategy.remainingUsageCount()
			)
			.expectedAdditionalSavingAmount(
				strategy.expectedAdditionalSavingAmount()
			)
			.previousMonthSpendingAmount(
				previousMonthSpendingAmount
			)
			.appliedBenefitCondition(condition)
			.reason(strategy.reason())
			.build();
	}

	private long calculateExpectedSavingAmount(
		SpendingPatternData pattern,
		CardData card,
		PerformanceTier activeTier,
		BenefitNode benefit,
		List<MonthlyUsageData> monthlyUsages
	) {
		long paymentAmount =
			pattern.getAverageAmount().longValue();

		if (paymentAmount < benefit.minimumPaymentAmount()) {
			return 0L;
		}

		int usedCount =
			findUsedCount(
				card.getUserCardId(),
				pattern.getCategoryCode(),
				monthlyUsages
			);

		if (benefit.monthlyCountLimit() != null
			&& usedCount >= benefit.monthlyCountLimit()) {

			return 0L;
		}

		long eligibleAmount = paymentAmount;

		if (benefit.maximumEligiblePerTransaction() != null) {
			eligibleAmount =
				Math.min(
					eligibleAmount,
					benefit.maximumEligiblePerTransaction()
				);
		}

		if (benefit.monthlyEligibleLimit() != null) {
			eligibleAmount =
				Math.min(
					eligibleAmount,
					benefit.monthlyEligibleLimit()
				);
		}

		long expectedSavingAmount;

		if (benefit.discountAmount() > 0L) {
			expectedSavingAmount =
				benefit.discountAmount();
		} else {
			boolean weekend =
				isWeekend(pattern.getUsualDayOfWeek());

			double fuelPricePerLiter =
				recommendationParamsLoader
					.params()
					.constants()
					.fuelPricePerLiter();

			double effectiveRate =
				BenefitEngine.effectiveRate(
					benefit,
					weekend,
					fuelPricePerLiter
				);

			expectedSavingAmount =
				Math.round(eligibleAmount * effectiveRate);
		}

		if (benefit.maximumDiscountPerTransaction() != null) {
			expectedSavingAmount =
				Math.min(
					expectedSavingAmount,
					benefit.maximumDiscountPerTransaction()
				);
		}

		long categoryUsedBenefit =
			findUsedBenefitAmount(
				card.getUserCardId(),
				pattern.getCategoryCode(),
				monthlyUsages
			);

		if (benefit.monthlyDiscountLimit() != null) {
			long remainingBenefit =
				Math.max(
					benefit.monthlyDiscountLimit()
						- categoryUsedBenefit,
					0L
				);

			expectedSavingAmount =
				Math.min(
					expectedSavingAmount,
					remainingBenefit
				);
		}

		Long combinedCap = activeTier.combinedCap();

		if (!benefit.integratedLimitExcluded()
			&& combinedCap != null) {

			long totalUsedBenefit =
				findTotalUsedBenefitAmount(
					card.getUserCardId(),
					monthlyUsages
				);

			long remainingCombinedBenefit =
				Math.max(
					combinedCap - totalUsedBenefit,
					0L
				);

			expectedSavingAmount =
				Math.min(
					expectedSavingAmount,
					remainingCombinedBenefit
				);
		}

		return Math.max(expectedSavingAmount, 0L);
	}

	private boolean matchesMerchant(
		SpendingPatternData pattern,
		BenefitNode benefit
	) {
		if (!benefit.isMerchantLimited()) {
			return true;
		}

		String merchantName =
			normalizeMerchantName(
				pattern.getUsualMerchantName()
			);

		if (merchantName.isBlank()) {
			return false;
		}

		for (String targetMerchant : benefit.merchantNames()) {
			String normalizedTarget =
				normalizeMerchantName(targetMerchant);

			if (merchantName.contains(normalizedTarget)
				|| normalizedTarget.contains(merchantName)) {

				return true;
			}
		}

		return false;
	}

	private String normalizeMerchantName(String merchantName) {
		if (merchantName == null) {
			return "";
		}

		return merchantName
			.replace(" ", "")
			.toLowerCase(Locale.KOREAN);
	}

	private boolean isWeekend(String dayOfWeek) {
		DayOfWeek day =
			DayOfWeek.valueOf(dayOfWeek);

		return day == DayOfWeek.SATURDAY
			|| day == DayOfWeek.SUNDAY;
	}

	private int findUsedCount(
		Long userCardId,
		String categoryCode,
		List<MonthlyUsageData> monthlyUsages
	) {
		for (MonthlyUsageData usage : monthlyUsages) {
			if (userCardId.equals(usage.getUserCardId())
				&& categoryCode.equals(usage.getCategoryCode())) {

				return usage.getUsageCount() == null
					? 0
					: usage.getUsageCount();
			}
		}

		return 0;
	}

	private long findUsedBenefitAmount(
		Long userCardId,
		String categoryCode,
		List<MonthlyUsageData> monthlyUsages
	) {
		for (MonthlyUsageData usage : monthlyUsages) {
			if (userCardId.equals(usage.getUserCardId())
				&& categoryCode.equals(usage.getCategoryCode())) {

				return usage.getUsedBenefitAmount() == null
					? 0L
					: usage.getUsedBenefitAmount().longValue();
			}
		}

		return 0L;
	}

	private long findTotalUsedBenefitAmount(
		Long userCardId,
		List<MonthlyUsageData> monthlyUsages
	) {
		long totalUsedBenefitAmount = 0L;

		for (MonthlyUsageData usage : monthlyUsages) {
			if (userCardId.equals(usage.getUserCardId())
				&& usage.getUsedBenefitAmount() != null) {

				totalUsedBenefitAmount +=
					usage.getUsedBenefitAmount().longValue();
			}
		}

		return totalUsedBenefitAmount;
	}

	private List<CategoryBenefitDto> createCategoryBreakdown(
		List<MonthlyCategoryBenefitVO> rows,
		long totalBenefitAmount
	) {
		List<CategoryBenefitDto> breakdown =
			new ArrayList<>();

		for (MonthlyCategoryBenefitVO row : rows) {
			long amount =
				row.getCategoryAmount() == null
					? 0L
					: row.getCategoryAmount();

			int percent =
				totalBenefitAmount <= 0
					? 0
					: Math.round(
					(amount * 100f)
					/ totalBenefitAmount
				);

			breakdown.add(
				CategoryBenefitDto.builder()
					.categoryCode(
						row.getCategoryCode()
					)
					.categoryName(
						row.getCategoryName()
					)
					.amount(amount)
					.percent(percent)
					.build()
			);
		}

		return breakdown;
	}

	private LocalDateTime startOfMonth(
		YearMonth yearMonth
	) {
		return yearMonth
			.atDay(1)
			.atStartOfDay();
	}

	/**
	 * yyyyMM 문자열을 YearMonth로 변환한다.
	 *
	 * <p>값이 비어 있으면 KST 기준 이번 달을 사용한다.
	 * 형식이 잘못됐거나 미래 달이면 예외가 발생한다.</p>
	 */
	private YearMonth parseYearMonth(
		String yearMonth
	) {
		if (yearMonth == null || yearMonth.isBlank()) {
			return YearMonth.now(ZONE);
		}

		YearMonth parsed;

		try {
			parsed = YearMonth.parse(
				yearMonth,
				YEAR_MONTH_FORMATTER
			);
		} catch (DateTimeParseException e) {
			throw new InvalidBenefitPeriodException(
				"yearMonth는 yyyyMM 형식이어야 합니다."
			);
		}

		if (parsed.isAfter(YearMonth.now(ZONE))) {
			throw new InvalidBenefitPeriodException(
				"yearMonth는 이번 달보다 미래일 수 없습니다."
			);
		}

		return parsed;
	}

	/**
	 * 최근 3개월 결제를 카테고리별로 집계하고,
	 * 가장 자주 결제한 요일과 평균 결제 금액을 계산한다.
	 */
	private List<SpendingPatternData> createSpendingPatterns(
		List<PaymentData> payments
	) {
		Map<String, SpendingPatternAccumulator> accumulatorByCategory =
			new LinkedHashMap<>();

		for (PaymentData payment : payments) {
			SpendingPatternAccumulator accumulator =
				accumulatorByCategory.computeIfAbsent(
					payment.getCategoryCode(),
					categoryCode -> new SpendingPatternAccumulator(
						categoryCode,
						payment.getCategoryName()
					)
				);

			accumulator.add(payment);
		}

		List<SpendingPatternData> patterns = new ArrayList<>();

		for (SpendingPatternAccumulator accumulator
			: accumulatorByCategory.values()) {

			patterns.add(accumulator.toPattern());
		}

		return patterns;
	}

	private List<CalculatedCoachingData> calculateCoachingData(
		List<SpendingPatternData> patterns,
		List<CardData> cards,
		List<MonthlyUsageData> monthlyUsages
	) {
		List<CalculatedCoachingData> results =
			new ArrayList<>();

		for (SpendingPatternData pattern : patterns) {
			List<CardBenefitEvaluation> evaluations =
				evaluateCards(
					pattern,
					cards,
					monthlyUsages
				);

			if (evaluations.isEmpty()) {
				continue;
			}

			CardBenefitEvaluation bestEvaluation =
				evaluations.get(0);

			CardBenefitEvaluation usualEvaluation =
				findEvaluationByUserCardId(
					evaluations,
					pattern.getUsualUserCardId()
				);

			CardBenefitEvaluation nextEvaluation =
				findNextCardEvaluation(
					evaluations,
					bestEvaluation.card().getUserCardId()
				);

			StrategyDecision strategy =
				determineStrategy(
					pattern,
					bestEvaluation,
					usualEvaluation,
					nextEvaluation,
					monthlyUsages
				);

			results.add(
				createCalculatedCoachingData(
					pattern,
					bestEvaluation,
					strategy
				)
			);
		}

		return results.stream()
			.sorted(
				Comparator.comparingLong(
						CalculatedCoachingData
							::getExpectedAdditionalSavingAmount
					)
					.reversed()
					.thenComparing(
						Comparator.comparingLong(
							CalculatedCoachingData
								::getExpectedSavingAmount
						).reversed()
					)
			)
			.limit(3)
			.toList();
	}

	private StrategyDecision determineStrategy(
		SpendingPatternData pattern,
		CardBenefitEvaluation bestEvaluation,
		CardBenefitEvaluation usualEvaluation,
		CardBenefitEvaluation nextEvaluation,
		List<MonthlyUsageData> monthlyUsages
	) {
		Long usualUserCardId =
			pattern.getUsualUserCardId();

		Long bestUserCardId =
			bestEvaluation.card().getUserCardId();

		boolean usingBestCard =
			usualUserCardId != null
				&& usualUserCardId.equals(bestUserCardId);

		Integer remainingUsageCount =
			calculateRemainingUsageCount(
				bestEvaluation,
				pattern.getCategoryCode(),
				monthlyUsages
			);

		if (remainingUsageCount != null
			&& remainingUsageCount > 0
			&& nextEvaluation != null) {

			long additionalSavingAmount =
				calculateUseThenSwitchSaving(
					usingBestCard,
					bestEvaluation,
					usualEvaluation,
					nextEvaluation
				);

			String reason = String.format(
				"%s 혜택을 %d회 더 사용한 뒤 %s로 전환하는 전략입니다.",
				bestEvaluation.card().getCardName(),
				remainingUsageCount,
				nextEvaluation.card().getCardName()
			);

			return new StrategyDecision(
				"USE_THEN_SWITCH",
				nextEvaluation.card().getCardName(),
				remainingUsageCount,
				additionalSavingAmount,
				reason
			);
		}

		if (!usingBestCard) {
			long usualSavingAmount =
				usualEvaluation == null
					? 0L
					: usualEvaluation.expectedSavingAmount();

			long additionalSavingAmount =
				Math.max(
					bestEvaluation.expectedSavingAmount()
						- usualSavingAmount,
					0L
				);

			// 추가 혜택이 작으면 카드 전환을 강하게 권하지 않는다.
			if (additionalSavingAmount
				< MINIMUM_SWITCH_SAVING_AMOUNT) {

				String reason = String.format(
					"%s 사용 시 평균 결제 1회 기준 %,d원의 혜택이 예상됩니다.",
					bestEvaluation.card().getCardName(),
					bestEvaluation.expectedSavingAmount()
				);

				return new StrategyDecision(
					"BENEFIT_GUIDE",
					null,
					remainingUsageCount,
					additionalSavingAmount,
					reason
				);
			}

			String reason = String.format(
				"기존 사용 카드보다 %s를 사용할 때 "
					+ "1회 결제 기준 %,d원의 추가 혜택이 예상됩니다.",
				bestEvaluation.card().getCardName(),
				additionalSavingAmount
			);

			return new StrategyDecision(
				"SWITCH_NOW",
				null,
				remainingUsageCount,
				additionalSavingAmount,
				reason
			);
		}

		String reason = String.format(
			"현재 사용 중인 %s가 해당 소비 패턴에서 가장 유리합니다.",
			bestEvaluation.card().getCardName()
		);

		return new StrategyDecision(
			"KEEP_USING",
			null,
			remainingUsageCount,
			0L,
			reason
		);
	}

	private CardBenefitEvaluation findEvaluationByUserCardId(
		List<CardBenefitEvaluation> evaluations,
		Long userCardId
	) {
		if (userCardId == null) {
			return null;
		}

		for (CardBenefitEvaluation evaluation : evaluations) {
			if (userCardId.equals(
				evaluation.card().getUserCardId()
			)) {
				return evaluation;
			}
		}

		return null;
	}

	private CardBenefitEvaluation findNextCardEvaluation(
		List<CardBenefitEvaluation> evaluations,
		Long excludedUserCardId
	) {
		for (CardBenefitEvaluation evaluation : evaluations) {
			if (!excludedUserCardId.equals(
				evaluation.card().getUserCardId()
			)) {
				return evaluation;
			}
		}

		return null;
	}

	private List<CardBenefitEvaluation> evaluateCards(
		SpendingPatternData pattern,
		List<CardData> cards,
		List<MonthlyUsageData> monthlyUsages
	) {
		List<CardBenefitEvaluation> evaluations =
			new ArrayList<>();

		for (CardData card : cards) {
			List<PerformanceTier> tiers =
				BenefitJsonParser.parse(
					card.getBenefitsInfo(),
					objectMapper
				);

			if (tiers.isEmpty()) {
				continue;
			}

			long previousMonthSpendingAmount =
				card.getPreviousMonthSpendingAmount() == null
					? 0L
					: card.getPreviousMonthSpendingAmount();

			PerformanceTier activeTier =
				BenefitEngine.activeTier(
					tiers,
					previousMonthSpendingAmount
				);

			CardBenefitEvaluation bestCardEvaluation = null;

			for (BenefitNode benefit
				: activeTier.benefitsForCategory(
				pattern.getCategoryCode()
			)) {

				if (!matchesMerchant(pattern, benefit)) {
					continue;
				}

				long expectedSavingAmount =
					calculateExpectedSavingAmount(
						pattern,
						card,
						activeTier,
						benefit,
						monthlyUsages
					);

				if (expectedSavingAmount <= 0L) {
					continue;
				}

				CardBenefitEvaluation evaluation =
					new CardBenefitEvaluation(
						card,
						activeTier,
						benefit,
						expectedSavingAmount
					);

				if (bestCardEvaluation == null
					|| expectedSavingAmount
					> bestCardEvaluation
					.expectedSavingAmount()) {

					bestCardEvaluation = evaluation;
				}
			}

			if (bestCardEvaluation != null) {
				evaluations.add(bestCardEvaluation);
			}
		}

		return evaluations.stream()
			.sorted(
				Comparator.comparingLong(
					CardBenefitEvaluation
						::expectedSavingAmount
				).reversed()
			)
			.toList();
	}

	private long calculateUseThenSwitchSaving(
		boolean usingBestCard,
		CardBenefitEvaluation bestEvaluation,
		CardBenefitEvaluation usualEvaluation,
		CardBenefitEvaluation nextEvaluation
	) {
		if (usingBestCard) {
			return nextEvaluation.expectedSavingAmount();
		}

		long usualSavingAmount =
			usualEvaluation == null
				? 0L
				: usualEvaluation.expectedSavingAmount();

		return Math.max(
			bestEvaluation.expectedSavingAmount()
				- usualSavingAmount,
			0L
		);
	}

	private Integer calculateRemainingUsageCount(
		CardBenefitEvaluation evaluation,
		String categoryCode,
		List<MonthlyUsageData> monthlyUsages
	) {
		Integer monthlyCountLimit =
			evaluation.benefit().monthlyCountLimit();

		if (monthlyCountLimit == null) {
			return null;
		}

		int usedCount =
			findUsedCount(
				evaluation.card().getUserCardId(),
				categoryCode,
				monthlyUsages
			);

		return Math.max(
			monthlyCountLimit - usedCount,
			0
		);
	}

	private record CardBenefitEvaluation(
		CardData card,
		PerformanceTier tier,
		BenefitNode benefit,
		long expectedSavingAmount
	) {
	}

	private record StrategyDecision(
		String strategyType,
		String nextRecommendedCardName,
		Integer remainingUsageCount,
		long expectedAdditionalSavingAmount,
		String reason
	) {
	}

	// 딱 한 번만 존재해야 함
	private static final class SpendingPatternAccumulator {

		private final String categoryCode;
		private final String categoryName;

		private final Map<DayOfWeek, Integer> paymentCountByDay =
			new EnumMap<>(DayOfWeek.class);

		private final Map<String, Integer> paymentCountByMerchant =
			new LinkedHashMap<>();

		private final Map<Long, Integer> paymentCountByUserCard =
			new LinkedHashMap<>();

		private BigDecimal totalAmount = BigDecimal.ZERO;
		private int paymentCount;

		private SpendingPatternAccumulator(
			String categoryCode,
			String categoryName
		) {
			this.categoryCode = categoryCode;
			this.categoryName = categoryName;
		}

		private void add(PaymentData payment) {
			totalAmount =
				totalAmount.add(payment.getOriginalAmount());

			paymentCount++;

			DayOfWeek dayOfWeek =
				payment.getPaymentTime().getDayOfWeek();

			paymentCountByDay.merge(
				dayOfWeek,
				1,
				Integer::sum
			);

			paymentCountByMerchant.merge(
				payment.getMerchantName(),
				1,
				Integer::sum
			);

			if (payment.getUserCardId() != null) {
				paymentCountByUserCard.merge(
					payment.getUserCardId(),
					1,
					Integer::sum
				);
			}
		}

		private SpendingPatternData toPattern() {
			BigDecimal averageAmount =
				totalAmount.divide(
					BigDecimal.valueOf(paymentCount),
					0,
					RoundingMode.HALF_UP
				);

			return SpendingPatternData.builder()
				.categoryCode(categoryCode)
				.categoryName(categoryName)
				.usualDayOfWeek(
					findUsualDayOfWeek().name()
				)
				.usualMerchantName(
					findUsualMerchantName()
				)
				.usualUserCardId(
					findUsualUserCardId()
				)
				.averageAmount(averageAmount)
				.paymentCount(paymentCount)
				.build();
		}

		private DayOfWeek findUsualDayOfWeek() {
			DayOfWeek usualDayOfWeek =
				DayOfWeek.MONDAY;

			int maximumCount = -1;

			for (DayOfWeek dayOfWeek
				: DayOfWeek.values()) {

				int count =
					paymentCountByDay.getOrDefault(
						dayOfWeek,
						0
					);

				if (count > maximumCount) {
					maximumCount = count;
					usualDayOfWeek = dayOfWeek;
				}
			}

			return usualDayOfWeek;
		}

		private String findUsualMerchantName() {
			String usualMerchantName = null;
			int maximumCount = -1;

			for (Map.Entry<String, Integer> entry
				: paymentCountByMerchant.entrySet()) {

				if (entry.getValue() > maximumCount) {
					maximumCount = entry.getValue();
					usualMerchantName = entry.getKey();
				}
			}

			return usualMerchantName;
		}

		private Long findUsualUserCardId() {
			Long usualUserCardId = null;
			int maximumCount = -1;

			for (Map.Entry<Long, Integer> entry
				: paymentCountByUserCard.entrySet()) {

				if (entry.getValue() > maximumCount) {
					maximumCount = entry.getValue();
					usualUserCardId = entry.getKey();
				}
			}

			return usualUserCardId;
		}
	}

	@Override
	public List<CategoryBenefitStatusResponseDto> getCategoryBenefitStatus(
		Long userId,
		String yearMonth
	) {
		YearMonth targetYearMonth =
			parseYearMonth(yearMonth);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		List<HeldCardBenefitVO> heldCards =
			benefitMapper.findHeldCardBenefitsByUserId(
				userId,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			);

		if (heldCards.isEmpty()) {
			return List.of();
		}

		List<CategoryBenefitUsageVO> usageRows =
			benefitMapper.findCategoryBenefitUsageByUserId(
				userId,
				startOfMonth(targetYearMonth),
				startOfMonth(
					targetYearMonth.plusMonths(1)
				)
			);

		Map<String, CategoryBenefitUsageVO> usageByCardAndCategory =
			groupUsageByCardAndCategory(usageRows);

		Map<String, String> categoryNames =
			findCategoryNames();

		List<CategoryBenefitStatusResponseDto> responses =
			new ArrayList<>();

		for (HeldCardBenefitVO card : heldCards) {
			responses.addAll(
				createCardCategoryStatuses(
					card,
					targetYearMonth,
					categoryNames,
					usageByCardAndCategory
				)
			);
		}

		return responses;
	}

	private Map<String, CategoryBenefitUsageVO> groupUsageByCardAndCategory(
		List<CategoryBenefitUsageVO> usageRows
	) {
		Map<String, CategoryBenefitUsageVO> grouped =
			new HashMap<>();

		for (CategoryBenefitUsageVO row : usageRows) {
			grouped.put(
				usageKey(
					row.getUserCardId(),
					row.getCategoryCode()
				),
				row
			);
		}

		return grouped;
	}

	private String usageKey(
		Long userCardId,
		String categoryCode
	) {
		return userCardId + "|" + categoryCode;
	}

	private Map<String, String> findCategoryNames() {
		Map<String, String> categoryNames =
			new HashMap<>();

		for (MerchantCategoryResponseDto category
			: merchantCategoryService.getCategoryList()) {

			categoryNames.putIfAbsent(
				category.getCategoryCode(),
				category.getCategoryName()
			);
		}

		return categoryNames;
	}

	/**
	 * 카드 한 장의 이번 달 적용 중인 혜택(전월 실적으로 정해지는 activeTier, 추천 도메인 모드
	 * 3과 동일 규약)을 카테고리별로 풀어 소진 현황 DTO 목록을 만든다. ALL_MERCHANTS(카테고리
	 * 구분 없는 전 가맹점 혜택)는 "카테고리별" 현황에 자연스럽게 대응되지 않아 제외한다.
	 */
	private List<CategoryBenefitStatusResponseDto> createCardCategoryStatuses(
		HeldCardBenefitVO card,
		YearMonth targetYearMonth,
		Map<String, String> categoryNames,
		Map<String, CategoryBenefitUsageVO> usageByCardAndCategory
	) {
		List<PerformanceTier> tiers =
			BenefitJsonParser.parse(card.getBenefitsInfo(), objectMapper);

		if (tiers.isEmpty()) {
			return List.of();
		}

		long prevMonthSpend =
			card.getPreviousMonthSpendingAmount() == null
				? 0L
				: card.getPreviousMonthSpendingAmount();

		PerformanceTier activeTier =
			BenefitEngine.activeTier(tiers, prevMonthSpend);

		List<CategoryBenefitStatusResponseDto> statuses =
			new ArrayList<>();

		for (BenefitNode benefit : activeTier.realBenefits()) {
			for (String categoryCode : benefit.categoryCodes()) {
				statuses.add(
					createStatus(
						card,
						targetYearMonth,
						categoryCode,
						categoryNames.get(categoryCode),
						benefit,
						usageByCardAndCategory.get(
							usageKey(card.getUserCardId(), categoryCode)
						)
					)
				);
			}
		}

		return statuses;
	}

	private CategoryBenefitStatusResponseDto createStatus(
		HeldCardBenefitVO card,
		YearMonth targetYearMonth,
		String categoryCode,
		String categoryName,
		BenefitNode benefit,
		CategoryBenefitUsageVO usage
	) {
		long usedAmount =
			usage == null || usage.getUsedAmount() == null
				? 0L
				: usage.getUsedAmount();

		int usedCount =
			usage == null || usage.getUsedCount() == null
				? 0
				: usage.getUsedCount();

		Long amountLimit =
			benefit.monthlyDiscountLimit();

		Integer countLimit =
			benefit.monthlyCountLimit();

		return CategoryBenefitStatusResponseDto.builder()
			.userCardId(card.getUserCardId())
			.cardId(card.getCardId())
			.cardName(card.getCardName())
			.cardImageUrl(card.getCardImageUrl())
			.yearMonth(targetYearMonth.toString())
			.categoryCode(categoryCode)
			.categoryName(categoryName)
			.serviceName(benefit.serviceName())
			.amountLimit(amountLimit)
			.usedAmount(usedAmount)
			.remainingAmount(
				amountLimit == null
					? null
					: Math.max(amountLimit - usedAmount, 0L)
			)
			.amountLimitReached(
				amountLimit != null && usedAmount >= amountLimit
			)
			.countLimit(countLimit)
			.usedCount(usedCount)
			.remainingCount(
				countLimit == null
					? null
					: Math.max(countLimit - usedCount, 0)
			)
			.countLimitReached(
				countLimit != null && usedCount >= countLimit
			)
			.build();
	}
}
