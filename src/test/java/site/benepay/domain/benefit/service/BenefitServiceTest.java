package site.benepay.domain.benefit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.InvalidBenefitPeriodException;
import site.benepay.common.facade.Facade;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto.MonthlyBenefitDto;
import site.benepay.domain.benefit.dto.CategoryBenefitStatusResponseDto;
import site.benepay.domain.benefit.dto.DailyBenefitAmountDto;
import site.benepay.domain.benefit.dto.ExpiringBenefitResponseDto;
import site.benepay.domain.benefit.dto.ExpiringBenefitsResponseDto;
import site.benepay.domain.benefit.dto.NearbyBenefitResponseDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto;
import site.benepay.domain.benefit.dto.MonthlyBenefitReportResponseDto.CategoryBenefitDto;
import site.benepay.domain.benefit.mapper.BenefitMapper;
import site.benepay.domain.benefit.vo.CategoryBenefitUsageVO;
import site.benepay.domain.benefit.vo.HeldCardBenefitVO;
import site.benepay.domain.benefit.vo.MonthlyCategoryBenefitVO;
import site.benepay.domain.benefit.vo.RecentPaymentLocationVO;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.RecommendedCardResponseDto;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 100L;

	private static final ZoneId ZONE =
		ZoneId.of("Asia/Seoul");

	/*
	 * 현재 연도는 실행 시각에 따라 조회 종료 시각이 달라지므로,
	 * 일반 계산 테스트에서는 이전 연도를 사용한다.
	 */
	private static final int BASE_YEAR =
		Year.now(ZONE).getValue() - 1;

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("uuuuMM");

	@Mock
	private BenefitMapper benefitMapper;

	private ObjectMapper objectMapper;

	@Mock
	private RecommendationParamsLoader recommendationParamsLoader;

	@Mock
	private OpenAiClient openAiClient;

	@Mock
	private MerchantCategoryService merchantCategoryService;

	@Mock
	private MerchantService merchantService;

	@Mock
	private Facade facade;

	private BenefitServiceImpl benefitService;
	private BenefitCoachDataLoader benefitCoachDataLoader;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		benefitCoachDataLoader =
			new BenefitCoachDataLoader(benefitMapper);

		benefitService =
			new BenefitServiceImpl(
				benefitMapper,
				merchantCategoryService,
				objectMapper,
				recommendationParamsLoader,
				openAiClient,
				benefitCoachDataLoader,
				merchantService,
				facade
			);
	}

	private DailyBenefitAmountDto benefitRow(
		LocalDate benefitDate,
		long dailyBenefitAmount
	) {
		DailyBenefitAmountDto row =
			createCardRow(
				USER_CARD_ID,
				10_000L
			);

		row.setBenefitDate(benefitDate);
		row.setDailyBenefitAmount(
			BigDecimal.valueOf(dailyBenefitAmount)
		);

		return row;
	}

	private DailyBenefitAmountDto createCardRow(
		Long userCardId,
		Long annualFee
	) {
		DailyBenefitAmountDto row =
			new DailyBenefitAmountDto();

		row.setUserCardId(userCardId);
		row.setCardId(userCardId + 1L);
		row.setCardName("청춘대로 톡톡카드");
		row.setCardImageUrl(
			"https://example.com/card.png"
		);
		row.setPanLast4("1234");
		row.setAnnualFee(annualFee);

		return row;
	}

	/*
	 * VO의 생성자 및 setter 구성에 영향을 받지 않도록
	 * Mockito 객체로 테스트 데이터를 만든다.
	 */
	private MonthlyCategoryBenefitVO categoryRow(
		String categoryCode,
		String categoryName,
		Long categoryAmount
	) {
		MonthlyCategoryBenefitVO row =
			mock(MonthlyCategoryBenefitVO.class);

		lenient()
			.when(row.getCategoryCode())
			.thenReturn(categoryCode);

		lenient()
			.when(row.getCategoryName())
			.thenReturn(categoryName);

		lenient()
			.when(row.getCategoryAmount())
			.thenReturn(categoryAmount);

		return row;
	}

	@Test
	void getAnnualFeeBreakEvenCalculatesBreakEvenDateAndMonthlyBenefits() {
		LocalDateTime startPaymentTime =
			LocalDate.of(BASE_YEAR, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			LocalDate.of(BASE_YEAR + 1, 1, 1)
				.atStartOfDay();

		List<DailyBenefitAmountDto> rows = List.of(
			benefitRow(
				LocalDate.of(BASE_YEAR, 1, 10),
				4_000L
			),
			benefitRow(
				LocalDate.of(BASE_YEAR, 2, 5),
				3_000L
			),
			benefitRow(
				LocalDate.of(BASE_YEAR, 2, 20),
				5_000L
			)
		);

		when(
			benefitMapper.findAnnualFeeBenefitsByUserId(
				USER_ID,
				startPaymentTime,
				endPaymentTime
			)
		).thenReturn(rows);

		List<AnnualFeeBreakEvenResponseDto> responses =
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			);

		assertThat(responses).hasSize(1);

		AnnualFeeBreakEvenResponseDto response =
			responses.get(0);

		assertThat(response.getUserCardId())
			.isEqualTo(USER_CARD_ID);

		assertThat(response.getAnnualFee())
			.isEqualTo(10_000L);

		assertThat(response.getAccumulatedBenefit())
			.isEqualTo(12_000L);

		assertThat(response.getNetBenefit())
			.isEqualTo(2_000L);

		assertThat(response.getRemainingAmount())
			.isEqualTo(0L);

		assertThat(response.getBreakEvenAchieved())
			.isTrue();

		assertThat(response.getBreakEvenDate())
			.isEqualTo(
				LocalDate.of(BASE_YEAR, 2, 20)
			);

		List<MonthlyBenefitDto> monthlyBenefits =
			response.getMonthlyBenefits();

		assertThat(monthlyBenefits).hasSize(12);

		MonthlyBenefitDto january =
			monthlyBenefits.get(0);

		assertThat(january.getYearMonth())
			.isEqualTo(BASE_YEAR + "-01");

		assertThat(january.getMonthlyBenefitAmount())
			.isEqualTo(4_000L);

		assertThat(january.getAccumulatedBenefitAmount())
			.isEqualTo(4_000L);

		MonthlyBenefitDto february =
			monthlyBenefits.get(1);

		assertThat(february.getYearMonth())
			.isEqualTo(BASE_YEAR + "-02");

		assertThat(february.getMonthlyBenefitAmount())
			.isEqualTo(8_000L);

		assertThat(february.getAccumulatedBenefitAmount())
			.isEqualTo(12_000L);

		MonthlyBenefitDto december =
			monthlyBenefits.get(11);

		assertThat(december.getMonthlyBenefitAmount())
			.isEqualTo(0L);

		assertThat(december.getAccumulatedBenefitAmount())
			.isEqualTo(12_000L);

		verify(benefitMapper)
			.findAnnualFeeBenefitsByUserId(
				USER_ID,
				startPaymentTime,
				endPaymentTime
			);
	}

	@Test
	void getAnnualFeeBreakEvenCalculatesRemainingAmountWhenNotAchieved() {
		LocalDateTime startPaymentTime =
			LocalDate.of(BASE_YEAR, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			LocalDate.of(BASE_YEAR + 1, 1, 1)
				.atStartOfDay();

		List<DailyBenefitAmountDto> rows = List.of(
			benefitRow(
				LocalDate.of(BASE_YEAR, 3, 10),
				3_000L
			)
		);

		when(
			benefitMapper.findAnnualFeeBenefitsByUserId(
				USER_ID,
				startPaymentTime,
				endPaymentTime
			)
		).thenReturn(rows);

		AnnualFeeBreakEvenResponseDto response =
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			).get(0);

		assertThat(response.getAccumulatedBenefit())
			.isEqualTo(3_000L);

		assertThat(response.getNetBenefit())
			.isEqualTo(-7_000L);

		assertThat(response.getRemainingAmount())
			.isEqualTo(7_000L);

		assertThat(response.getBreakEvenAchieved())
			.isFalse();

		assertThat(response.getBreakEvenDate())
			.isNull();
	}

	@Test
	void getAnnualFeeBreakEvenReturnsZeroWhenTheCardHasNoBenefits() {
		LocalDateTime startPaymentTime =
			LocalDate.of(BASE_YEAR, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			LocalDate.of(BASE_YEAR + 1, 1, 1)
				.atStartOfDay();

		DailyBenefitAmountDto row =
			createCardRow(
				USER_CARD_ID,
				10_000L
			);

		row.setBenefitDate(null);
		row.setDailyBenefitAmount(BigDecimal.ZERO);

		when(
			benefitMapper.findAnnualFeeBenefitsByUserId(
				USER_ID,
				startPaymentTime,
				endPaymentTime
			)
		).thenReturn(List.of(row));

		AnnualFeeBreakEvenResponseDto response =
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			).get(0);

		assertThat(response.getAccumulatedBenefit())
			.isEqualTo(0L);

		assertThat(response.getNetBenefit())
			.isEqualTo(-10_000L);

		assertThat(response.getRemainingAmount())
			.isEqualTo(10_000L);

		assertThat(response.getBreakEvenAchieved())
			.isFalse();

		assertThat(response.getBreakEvenDate())
			.isNull();

		assertThat(response.getMonthlyBenefits())
			.hasSize(12)
			.allSatisfy(month -> {
				assertThat(
					month.getMonthlyBenefitAmount()
				).isEqualTo(0L);

				assertThat(
					month.getAccumulatedBenefitAmount()
				).isEqualTo(0L);
			});
	}

	@Test
	void getAnnualFeeBreakEvenGroupsRowsByUserCardId() {
		LocalDateTime startPaymentTime =
			LocalDate.of(BASE_YEAR, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			LocalDate.of(BASE_YEAR + 1, 1, 1)
				.atStartOfDay();

		DailyBenefitAmountDto firstCard =
			createCardRow(100L, 10_000L);

		firstCard.setBenefitDate(
			LocalDate.of(BASE_YEAR, 1, 10)
		);

		firstCard.setDailyBenefitAmount(
			BigDecimal.valueOf(2_000L)
		);

		DailyBenefitAmountDto secondCard =
			createCardRow(200L, 15_000L);

		secondCard.setBenefitDate(
			LocalDate.of(BASE_YEAR, 1, 15)
		);

		secondCard.setDailyBenefitAmount(
			BigDecimal.valueOf(5_000L)
		);

		when(
			benefitMapper.findAnnualFeeBenefitsByUserId(
				USER_ID,
				startPaymentTime,
				endPaymentTime
			)
		).thenReturn(
			List.of(firstCard, secondCard)
		);

		List<AnnualFeeBreakEvenResponseDto> responses =
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			);

		assertThat(responses)
			.hasSize(2)
			.extracting(
				AnnualFeeBreakEvenResponseDto::getUserCardId
			)
			.containsExactly(100L, 200L);

		assertThat(
			responses.get(0).getAccumulatedBenefit()
		).isEqualTo(2_000L);

		assertThat(
			responses.get(1).getAccumulatedBenefit()
		).isEqualTo(5_000L);
	}

	@Test
	void getAnnualFeeBreakEvenReturnsEmptyListWhenTheUserHasNoCards() {
		LocalDateTime startPaymentTime =
			LocalDate.of(BASE_YEAR, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			LocalDate.of(BASE_YEAR + 1, 1, 1)
				.atStartOfDay();

		when(
			benefitMapper.findAnnualFeeBenefitsByUserId(
				USER_ID,
				startPaymentTime,
				endPaymentTime
			)
		).thenReturn(List.of());

		assertThat(
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				BASE_YEAR
			)
		).isEmpty();
	}

	@Test
	void getAnnualFeeBreakEvenThrowsWhenYearIsBefore2000() {
		assertThatThrownBy(
			() -> benefitService.getAnnualFeeBreakEven(
				USER_ID,
				1999
			)
		)
			.isInstanceOf(
				InvalidBenefitPeriodException.class
			)
			.hasMessage(
				"year는 2000년부터 현재 연도 사이여야 합니다."
			);

		verifyNoInteractions(benefitMapper);
	}

	@Test
	void getAnnualFeeBreakEvenThrowsWhenYearIsInTheFuture() {
		int futureYear =
			Year.now(ZONE).getValue() + 1;

		assertThatThrownBy(
			() -> benefitService.getAnnualFeeBreakEven(
				USER_ID,
				futureYear
			)
		)
			.isInstanceOf(
				InvalidBenefitPeriodException.class
			)
			.hasMessage(
				"year는 2000년부터 현재 연도 사이여야 합니다."
			);

		verifyNoInteractions(benefitMapper);
	}

	@Test
	void getAnnualFeeBreakEvenReturnsMonthsUntilCurrentMonth() {
		int currentYear =
			Year.now(ZONE).getValue();

		DailyBenefitAmountDto row =
			createCardRow(
				USER_CARD_ID,
				10_000L
			);

		row.setBenefitDate(
			LocalDate.of(currentYear, 1, 1)
		);

		/*
		 * 일별 혜택 금액이 null인 경우의 분기도 검증한다.
		 */
		row.setDailyBenefitAmount(null);

		when(
			benefitMapper.findAnnualFeeBenefitsByUserId(
				eq(USER_ID),
				eq(
					LocalDate.of(currentYear, 1, 1)
						.atStartOfDay()
				),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of(row));

		AnnualFeeBreakEvenResponseDto response =
			benefitService.getAnnualFeeBreakEven(
				USER_ID,
				currentYear
			).get(0);

		assertThat(response.getAccumulatedBenefit())
			.isEqualTo(0L);

		assertThat(response.getMonthlyBenefits())
			.hasSize(
				YearMonth.now(ZONE).getMonthValue()
			);
	}

	@Test
	void getMonthlyBenefitReportCalculatesTotalDeltaAndPercentage() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 5);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		MonthlyCategoryBenefitVO food =
			categoryRow(
				"5812",
				"음식점",
				6_000L
			);

		MonthlyCategoryBenefitVO cafe =
			categoryRow(
				"5813",
				"카페",
				4_000L
			);

		MonthlyCategoryBenefitVO previous =
			categoryRow(
				"5812",
				"음식점",
				7_000L
			);

		when(
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				USER_ID,
				targetYearMonth
					.atDay(1)
					.atStartOfDay(),
				targetYearMonth
					.plusMonths(1)
					.atDay(1)
					.atStartOfDay()
			)
		).thenReturn(
			List.of(food, cafe)
		);

		when(
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				USER_ID,
				previousYearMonth
					.atDay(1)
					.atStartOfDay(),
				previousYearMonth
					.plusMonths(1)
					.atDay(1)
					.atStartOfDay()
			)
		).thenReturn(
			List.of(previous)
		);

		MonthlyBenefitReportResponseDto response =
			benefitService.getMonthlyBenefitReport(
				USER_ID,
				targetYearMonth.format(
					YEAR_MONTH_FORMATTER
				)
			);

		assertThat(response.getYearMonth())
			.isEqualTo(targetYearMonth.toString());

		assertThat(response.getTotalBenefitAmount())
			.isEqualTo(10_000L);

		assertThat(response.getDeltaVsLastMonth())
			.isEqualTo(3_000L);

		assertThat(response.getCategoryBreakdown())
			.hasSize(2);

		CategoryBenefitDto foodBenefit =
			response.getCategoryBreakdown().get(0);

		assertThat(foodBenefit.getCategoryCode())
			.isEqualTo("5812");

		assertThat(foodBenefit.getCategoryName())
			.isEqualTo("음식점");

		assertThat(foodBenefit.getAmount())
			.isEqualTo(6_000L);

		assertThat(foodBenefit.getPercent())
			.isEqualTo(60);

		CategoryBenefitDto cafeBenefit =
			response.getCategoryBreakdown().get(1);

		assertThat(cafeBenefit.getCategoryCode())
			.isEqualTo("5813");

		assertThat(cafeBenefit.getCategoryName())
			.isEqualTo("카페");

		assertThat(cafeBenefit.getAmount())
			.isEqualTo(4_000L);

		assertThat(cafeBenefit.getPercent())
			.isEqualTo(40);

		verify(benefitMapper)
			.findMonthlyCategoryBenefitsByUserId(
				USER_ID,
				targetYearMonth
					.atDay(1)
					.atStartOfDay(),
				targetYearMonth
					.plusMonths(1)
					.atDay(1)
					.atStartOfDay()
			);

		verify(benefitMapper)
			.findMonthlyCategoryBenefitsByUserId(
				USER_ID,
				previousYearMonth
					.atDay(1)
					.atStartOfDay(),
				previousYearMonth
					.plusMonths(1)
					.atDay(1)
					.atStartOfDay()
			);
	}

	@Test
	void getMonthlyBenefitReportTreatsNullAmountAsZero() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 6);

		MonthlyCategoryBenefitVO row =
			categoryRow(
				"5813",
				"카페",
				null
			);

		when(
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(
			List.of(row),
			List.of()
		);

		MonthlyBenefitReportResponseDto response =
			benefitService.getMonthlyBenefitReport(
				USER_ID,
				targetYearMonth.format(
					YEAR_MONTH_FORMATTER
				)
			);

		assertThat(response.getTotalBenefitAmount())
			.isEqualTo(0L);

		assertThat(response.getDeltaVsLastMonth())
			.isEqualTo(0L);

		assertThat(response.getCategoryBreakdown())
			.hasSize(1);

		CategoryBenefitDto category =
			response.getCategoryBreakdown().get(0);

		assertThat(category.getAmount())
			.isEqualTo(0L);

		assertThat(category.getPercent())
			.isEqualTo(0);
	}

	@Test
	void getMonthlyBenefitReportUsesCurrentMonthWhenYearMonthIsNull() {
		YearMonth currentYearMonth =
			YearMonth.now(ZONE);

		when(
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		MonthlyBenefitReportResponseDto response =
			benefitService.getMonthlyBenefitReport(
				USER_ID,
				null
			);

		assertThat(response.getYearMonth())
			.isEqualTo(currentYearMonth.toString());

		verify(benefitMapper, times(2))
			.findMonthlyCategoryBenefitsByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			);
	}

	@Test
	void getMonthlyBenefitReportUsesCurrentMonthWhenYearMonthIsBlank() {
		when(
			benefitMapper.findMonthlyCategoryBenefitsByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		MonthlyBenefitReportResponseDto response =
			benefitService.getMonthlyBenefitReport(
				USER_ID,
				" "
			);

		assertThat(response.getYearMonth())
			.isEqualTo(
				YearMonth.now(ZONE).toString()
			);
	}

	@Test
	void getMonthlyBenefitReportThrowsWhenYearMonthFormatIsInvalid() {
		assertThatThrownBy(
			() -> benefitService.getMonthlyBenefitReport(
				USER_ID,
				"2026-08"
			)
		)
			.isInstanceOf(
				InvalidBenefitPeriodException.class
			)
			.hasMessage(
				"yearMonth는 yyyyMM 형식이어야 합니다."
			);

		verifyNoInteractions(benefitMapper);
	}

	@Test
	void getMonthlyBenefitReportThrowsWhenYearMonthIsInTheFuture() {
		YearMonth futureYearMonth =
			YearMonth.now(ZONE).plusMonths(1);

		assertThatThrownBy(
			() -> benefitService.getMonthlyBenefitReport(
				USER_ID,
				futureYearMonth.format(
					YEAR_MONTH_FORMATTER
				)
			)
		)
			.isInstanceOf(
				InvalidBenefitPeriodException.class
			)
			.hasMessage(
				"yearMonth는 이번 달보다 미래일 수 없습니다."
			);

		verifyNoInteractions(benefitMapper);
	}

	// ---- getCategoryBenefitStatus ----

	private HeldCardBenefitVO heldCard(
		Long userCardId,
		String benefitsInfo,
		Long previousMonthSpendingAmount
	) {
		HeldCardBenefitVO card =
			new HeldCardBenefitVO();

		card.setUserCardId(userCardId);
		card.setCardId(userCardId + 1L);
		card.setCardName("청춘대로 톡톡카드");
		card.setCardImageUrl("https://example.com/card.png");
		card.setBenefitsInfo(benefitsInfo);
		card.setPreviousMonthSpendingAmount(previousMonthSpendingAmount);

		return card;
	}

	private String singleTierBenefitsInfo(
		String serviceName,
		String categoryCode,
		Long monthlyDiscountLimit,
		Integer monthlyCountLimit
	) {
		return String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":10,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":%s,\"monthlyCountLimit\":%s}]}]}",
			serviceName,
			categoryCode,
			monthlyDiscountLimit == null ? "null" : monthlyDiscountLimit,
			monthlyCountLimit == null ? "null" : monthlyCountLimit
		);
	}

	private String twoTierBenefitsInfo(
		String categoryCode
	) {
		return String.format(
			"{\"performanceTiers\":["
				+ "{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"기본 할인\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":5,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":10000,\"monthlyCountLimit\":2}]},"
				+ "{\"minimumSpending\":300000,\"benefits\":["
				+ "{\"serviceName\":\"우수 할인\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":10,\"minimumPaymentAmount\":300000,"
				+ "\"maximumDiscountAmountPerMonth\":50000,\"monthlyCountLimit\":10}]}"
				+ "]}",
			categoryCode,
			categoryCode
		);
	}

	private CategoryBenefitUsageVO usageRow(
		Long userCardId,
		String categoryCode,
		Long usedAmount,
		Integer usedCount
	) {
		CategoryBenefitUsageVO row =
			new CategoryBenefitUsageVO();

		row.setUserCardId(userCardId);
		row.setCategoryCode(categoryCode);
		row.setUsedAmount(usedAmount);
		row.setUsedCount(usedCount);

		return row;
	}

	@Test
	void getCategoryBenefitStatusReturnsUsedAndRemainingAmountsForActiveTierCategories() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(
					USER_CARD_ID,
					singleTierBenefitsInfo("카페 5% 할인", "5813", 30_000L, 5),
					0L
				)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(
			List.of(
				usageRow(USER_CARD_ID, "5813", 12_000L, 3)
			)
		);

		when(merchantCategoryService.getCategoryList()).thenReturn(
			List.of(
				MerchantCategoryResponseDto.builder()
					.categoryCode("5813")
					.categoryName("카페")
					.build()
			)
		);

		List<CategoryBenefitStatusResponseDto> result =
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			);

		assertThat(result).hasSize(1);

		CategoryBenefitStatusResponseDto status =
			result.get(0);

		assertThat(status.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(status.getYearMonth()).isEqualTo(targetYearMonth.toString());
		assertThat(status.getCategoryCode()).isEqualTo("5813");
		assertThat(status.getCategoryName()).isEqualTo("카페");
		assertThat(status.getServiceName()).isEqualTo("카페 5% 할인");
		assertThat(status.getAmountLimit()).isEqualTo(30_000L);
		assertThat(status.getUsedAmount()).isEqualTo(12_000L);
		assertThat(status.getRemainingAmount()).isEqualTo(18_000L);
		assertThat(status.isAmountLimitReached()).isFalse();
		assertThat(status.getCountLimit()).isEqualTo(5);
		assertThat(status.getUsedCount()).isEqualTo(3);
		assertThat(status.getRemainingCount()).isEqualTo(2);
		assertThat(status.isCountLimitReached()).isFalse();
	}

	@Test
	void getCategoryBenefitStatusMarksLimitReachedWhenUsageMeetsOrExceedsLimit() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(
					USER_CARD_ID,
					singleTierBenefitsInfo("카페 5% 할인", "5813", 10_000L, 2),
					0L
				)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(
			List.of(
				usageRow(USER_CARD_ID, "5813", 10_000L, 2)
			)
		);

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		CategoryBenefitStatusResponseDto status =
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			).get(0);

		assertThat(status.getRemainingAmount()).isEqualTo(0L);
		assertThat(status.isAmountLimitReached()).isTrue();
		assertThat(status.getRemainingCount()).isEqualTo(0);
		assertThat(status.isCountLimitReached()).isTrue();
	}

	@Test
	void getCategoryBenefitStatusTreatsMissingUsageRowAsZero() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(
					USER_CARD_ID,
					singleTierBenefitsInfo("카페 5% 할인", "5813", 10_000L, 2),
					0L
				)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		CategoryBenefitStatusResponseDto status =
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			).get(0);

		assertThat(status.getUsedAmount()).isEqualTo(0L);
		assertThat(status.getUsedCount()).isEqualTo(0);
		assertThat(status.getRemainingAmount()).isEqualTo(10_000L);
		assertThat(status.isAmountLimitReached()).isFalse();
	}

	@Test
	void getCategoryBenefitStatusLeavesRemainingAndReachedFalseWhenNoLimitDefined() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(
					USER_CARD_ID,
					singleTierBenefitsInfo("카페 5% 할인", "5813", null, null),
					0L
				)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		CategoryBenefitStatusResponseDto status =
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			).get(0);

		assertThat(status.getAmountLimit()).isNull();
		assertThat(status.getRemainingAmount()).isNull();
		assertThat(status.isAmountLimitReached()).isFalse();
		assertThat(status.getCountLimit()).isNull();
		assertThat(status.getRemainingCount()).isNull();
		assertThat(status.isCountLimitReached()).isFalse();
	}

	@Test
	void getCategoryBenefitStatusUsesTierActivatedByPreviousMonthSpend() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(
					USER_CARD_ID,
					twoTierBenefitsInfo("5813"),
					300_000L
				)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		CategoryBenefitStatusResponseDto status =
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			).get(0);

		assertThat(status.getServiceName()).isEqualTo("우수 할인");
		assertThat(status.getAmountLimit()).isEqualTo(50_000L);
	}

	@Test
	void getCategoryBenefitStatusExpandsEachCategoryCodeOnABenefitIntoItsOwnRow() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		String benefitsInfo =
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"마트/편의점 할인\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"5411\",\"5412\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":5,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":20000,\"monthlyCountLimit\":4}]}]}";

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(USER_CARD_ID, benefitsInfo, 0L)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		List<CategoryBenefitStatusResponseDto> result =
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			);

		assertThat(result)
			.extracting(CategoryBenefitStatusResponseDto::getCategoryCode)
			.containsExactlyInAnyOrder("5411", "5412");
	}

	@Test
	void getCategoryBenefitStatusReturnsEmptyListWhenUserHasNoHeldCards() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(List.of());

		assertThat(
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).isEmpty();

		verify(benefitMapper, never()).findCategoryBenefitUsageByUserId(
			any(), any(), any()
		);
	}

	@Test
	void getCategoryBenefitStatusSkipsCardsWithBlankBenefitsInfo() {
		YearMonth targetYearMonth =
			YearMonth.of(BASE_YEAR, 8);

		YearMonth previousYearMonth =
			targetYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(USER_CARD_ID, "", 0L)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		assertThat(
			benefitService.getCategoryBenefitStatus(
				USER_ID,
				targetYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).isEmpty();
	}

	@Test
	void getCategoryBenefitStatusUsesCurrentMonthWhenYearMonthIsNull() {
		YearMonth currentYearMonth =
			YearMonth.now(ZONE);

		YearMonth previousYearMonth =
			currentYearMonth.minusMonths(1);

		when(
			benefitMapper.findHeldCardBenefitsByUserId(
				USER_ID,
				previousYearMonth.format(YEAR_MONTH_FORMATTER)
			)
		).thenReturn(
			List.of(
				heldCard(
					USER_CARD_ID,
					singleTierBenefitsInfo("카페 할인", "5813", 10_000L, 2),
					0L
				)
			)
		);

		when(
			benefitMapper.findCategoryBenefitUsageByUserId(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		List<CategoryBenefitStatusResponseDto> result =
			benefitService.getCategoryBenefitStatus(USER_ID, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getYearMonth()).isEqualTo(currentYearMonth.toString());
	}

	// ---- getExpiringBenefits ----

	private String allMerchantsBenefitsInfo(
		String serviceName,
		long discountAmount,
		List<String> merchantNames
	) {
		String merchantNamesJson = merchantNames.isEmpty()
			? "[]"
			: "[\"" + String.join("\",\"", merchantNames) + "\"]";

		return String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"ALL_MERCHANTS\","
				+ "\"categoryCodes\":[],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountAmount\":%d,\"minimumPaymentAmount\":0,"
				+ "\"merchantNames\":%s}]}]}",
			serviceName,
			discountAmount,
			merchantNamesJson
		);
	}

	@Test
	void getExpiringBenefitsExcludesCategoryBenefitsAlreadyUsedThisMonth() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		lenient().when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());
		YearMonth targetYearMonth = YearMonth.from(today);
		YearMonth previousYearMonth = targetYearMonth.minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of(
				heldCard(USER_CARD_ID, singleTierBenefitsInfo("카페 5% 할인", "5813", 30_000L, 5), 0L)
			));

		// 이미 이번 달에 그 카테고리로 결제해서 혜택을 받은 상태 -> 후보에서 빠져야 한다.
		when(benefitMapper.findCategoryBenefitUsageByUserId(
			eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of(usageRow(USER_CARD_ID, "5813", 3_000L, 1)));

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getBenefits()).isEmpty();
		assertThat(result.getDaysRemaining()).isEqualTo(today.lengthOfMonth() - today.getDayOfMonth());
		verify(benefitMapper, never()).findThisMonthMerchantNamesByUserCardId(any(), any(), any());
	}

	@Test
	void getExpiringBenefitsIncludesAnUnusedCategoryBenefit() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		lenient().when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of(
				heldCard(USER_CARD_ID, singleTierBenefitsInfo("카페 5% 할인", "5813", 30_000L, 5), 0L)
			));

		when(benefitMapper.findCategoryBenefitUsageByUserId(
			eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of());

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getBenefits()).hasSize(1);
		ExpiringBenefitResponseDto benefit = result.getBenefits().get(0);
		assertThat(benefit.getServiceName()).isEqualTo("카페 5% 할인");
		assertThat(benefit.getAmount()).isEqualTo(30_000L);
		assertThat(benefit.getMerchantNote()).isNull();
	}

	@Test
	void getExpiringBenefitsChecksMerchantNamesForBrandLimitedBenefitsInsteadOfCategoryUsage() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		lenient().when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of(
				heldCard(USER_CARD_ID,
					allMerchantsBenefitsInfo("교보문고 할인", 2_500L, List.of("교보문고")), 0L)
			));

		when(benefitMapper.findCategoryBenefitUsageByUserId(
			eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of());

		// 이번 달에 교보문고가 아니라 다른 곳에서만 결제함 -> 이 혜택은 여전히 안 쓴 것.
		when(benefitMapper.findThisMonthMerchantNamesByUserCardId(
			eq(USER_CARD_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of("스타벅스 강남점"));

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getBenefits()).hasSize(1);
		ExpiringBenefitResponseDto benefit = result.getBenefits().get(0);
		assertThat(benefit.getServiceName()).isEqualTo("교보문고 할인");
		assertThat(benefit.getAmount()).isEqualTo(2_500L);
		assertThat(benefit.getMerchantNote()).isEqualTo("교보문고 한정");
	}

	@Test
	void getExpiringBenefitsExcludesABrandLimitedBenefitAlreadyUsedAtThatMerchant() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		lenient().when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of(
				heldCard(USER_CARD_ID,
					allMerchantsBenefitsInfo("교보문고 할인", 2_500L, List.of("교보문고")), 0L)
			));

		when(benefitMapper.findCategoryBenefitUsageByUserId(
			eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of());

		when(benefitMapper.findThisMonthMerchantNamesByUserCardId(
			eq(USER_CARD_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of("교보문고"));

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getBenefits()).isEmpty();
	}

	@Test
	void getExpiringBenefitsSortsByAmountDescendingAndCapsAtThree() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		lenient().when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		String fourBenefitsInfo =
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"편의점\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"5499\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":5,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":1500,\"monthlyCountLimit\":5},"
				+ "{\"serviceName\":\"카페\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"5813\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":10,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":2000,\"monthlyCountLimit\":5},"
				+ "{\"serviceName\":\"영화\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"7832\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":20,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":4000,\"monthlyCountLimit\":2},"
				+ "{\"serviceName\":\"주유\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"5541\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":5,\"minimumPaymentAmount\":0,"
				+ "\"maximumDiscountAmountPerMonth\":1000,\"monthlyCountLimit\":5}"
				+ "]}]}";

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of(heldCard(USER_CARD_ID, fourBenefitsInfo, 0L)));

		when(benefitMapper.findCategoryBenefitUsageByUserId(
			eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(List.of());

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getBenefits()).hasSize(3);
		assertThat(result.getBenefits())
			.extracting(ExpiringBenefitResponseDto::getServiceName)
			.containsExactly("영화", "카페", "편의점");
	}

	@Test
	void getExpiringBenefitsReturnsEmptyWhenTheUserHoldsNoCards() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		lenient().when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of());

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getBenefits()).isEmpty();
		verify(benefitMapper, never()).findCategoryBenefitUsageByUserId(any(), any(), any());
	}

	private RecentPaymentLocationVO recentPaymentLocation(double lat, double lng) {
		RecentPaymentLocationVO location = new RecentPaymentLocationVO();
		location.setLatitude(BigDecimal.valueOf(lat));
		location.setLongitude(BigDecimal.valueOf(lng));
		return location;
	}

	private MerchantResponseDto nearbyMerchant(Long merchantId, String merchantName, long distanceMeters) {
		return MerchantResponseDto.builder()
			.merchantId(merchantId)
			.merchantName(merchantName)
			.distanceMeters(distanceMeters)
			.build();
	}

	@Test
	void getExpiringBenefitsIncludesNearbyMerchantBenefitsWithinTwoKilometers() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of());
		when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.of(recentPaymentLocation(37.5, 127.0)));

		// 반경(2km) 밖 매장은 facade에 아예 안 넘어가야 한다.
		MerchantResponseDto near = nearbyMerchant(1L, "스타벅스 강남점", 500L);
		MerchantResponseDto far = nearbyMerchant(2L, "저 멀리 매장", 3_000L);
		when(merchantService.getNearbyMerchants(37.5, 127.0, null, 50))
			.thenReturn(List.of(near, far));

		NearbyMerchantRecommendationResponseDto evaluated = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(1L)
			.merchantName("스타벅스 강남점")
			.distanceMeters(500L)
			.benefitAvailable(true)
			.recommendedCards(List.of(RecommendedCardResponseDto.builder()
				.cardName("청춘대로 톡톡카드")
				.benefitSummary("이번 달 확정 3,000원")
				.build()))
			.build();

		when(facade.getRecommendedMerchants(eq(USER_ID), argThat(list -> list.size() == 1)))
			.thenReturn(List.of(evaluated));

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getNearbyMerchantBenefits()).hasSize(1);
		NearbyBenefitResponseDto nearbyBenefit = result.getNearbyMerchantBenefits().get(0);
		assertThat(nearbyBenefit.getMerchantName()).isEqualTo("스타벅스 강남점");
		assertThat(nearbyBenefit.getCardName()).isEqualTo("청춘대로 톡톡카드");
		assertThat(nearbyBenefit.getDistanceMeters()).isEqualTo(500L);
	}

	@Test
	void getExpiringBenefitsReturnsNoNearbyMerchantBenefitsWithoutPaymentHistory() {
		LocalDate today = LocalDate.of(BASE_YEAR, 8, 26);
		YearMonth previousYearMonth = YearMonth.from(today).minusMonths(1);

		when(benefitMapper.findHeldCardBenefitsByUserId(
			USER_ID, previousYearMonth.format(YEAR_MONTH_FORMATTER)))
			.thenReturn(List.of());
		when(benefitMapper.findMostRecentPaymentLocationByUserId(USER_ID))
			.thenReturn(Optional.empty());

		ExpiringBenefitsResponseDto result = benefitService.getExpiringBenefits(USER_ID, today);

		assertThat(result.getNearbyMerchantBenefits()).isEmpty();
		verify(merchantService, never()).getNearbyMerchants(anyDouble(), anyDouble(), any(), anyInt());
	}
}