package site.benepay.domain.benefit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.InvalidBenefitPeriodException;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto.MonthlyBenefitDto;
import site.benepay.domain.benefit.dto.CategoryBenefitStatusResponseDto;
import site.benepay.domain.benefit.dto.DailyBenefitAmountDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto.CategoryBenefitDto;
import site.benepay.domain.benefit.mapper.BenefitMapper;
import site.benepay.domain.benefit.vo.CategoryBenefitUsageVO;
import site.benepay.domain.benefit.vo.HeldCardBenefitVO;
import site.benepay.domain.benefit.vo.MonthlyCategoryBenefitVO;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.engine.BenefitEngine;
import site.benepay.domain.recommendation.engine.BenefitJsonParser;
import site.benepay.domain.recommendation.engine.BenefitNode;
import site.benepay.domain.recommendation.engine.PerformanceTier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitServiceImpl implements BenefitService {

	private static final ZoneId ZONE =
		ZoneId.of("Asia/Seoul");

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("uuuuMM");

	private final BenefitMapper benefitMapper;
	private final MerchantCategoryService merchantCategoryService;
	private final ObjectMapper objectMapper;

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
