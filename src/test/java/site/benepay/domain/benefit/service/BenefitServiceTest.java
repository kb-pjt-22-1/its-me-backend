package site.benepay.domain.benefit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto.MonthlyBenefitDto;
import site.benepay.domain.benefit.mapper.BenefitMapper;
import site.benepay.domain.benefit.vo.DailyBenefitAmountVO;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 100L;

	/*
	 * 현재 연도는 실행 시각에 따라 조회 종료 시각이 달라지므로,
	 * 계산 테스트에서는 이전 연도를 사용한다.
	 */
	private static final int BASE_YEAR =
		Year.now().getValue() - 1;

	@Mock
	private BenefitMapper benefitMapper;

	private BenefitServiceImpl benefitService;

	@BeforeEach
	void setUp() {
		benefitService = new BenefitServiceImpl(benefitMapper);
	}

	/**
	 * 카드 한 장의 특정 날짜 혜택 조회 결과를 만든다.
	 */
	private DailyBenefitAmountVO benefitRow(
		LocalDate benefitDate,
		long dailyBenefitAmount
	) {
		DailyBenefitAmountVO row =
			createCardRow(USER_CARD_ID, 10_000L);

		row.setBenefitDate(benefitDate);
		row.setDailyBenefitAmount(
			BigDecimal.valueOf(dailyBenefitAmount)
		);

		return row;
	}

	/**
	 * 공통 카드 정보를 가진 VO를 만든다.
	 */
	private DailyBenefitAmountVO createCardRow(
		Long userCardId,
		Long annualFee
	) {
		DailyBenefitAmountVO row =
			new DailyBenefitAmountVO();

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

	@Test
	void getAnnualFeeBreakEvenCalculatesBreakEvenDateAndMonthlyBenefits() {
		LocalDateTime startPaymentTime =
			LocalDate.of(BASE_YEAR, 1, 1)
				.atStartOfDay();

		LocalDateTime endPaymentTime =
			LocalDate.of(BASE_YEAR + 1, 1, 1)
				.atStartOfDay();

		List<DailyBenefitAmountVO> rows = List.of(
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

		/*
		 * 1월 4,000원 + 2월 3,000원 + 5,000원으로
		 * 2월 20일에 누적 금액이 연회비를 넘는다.
		 */
		assertThat(response.getBreakEvenDate())
			.isEqualTo(
				LocalDate.of(BASE_YEAR, 2, 20)
			);

		List<MonthlyBenefitDto> monthlyBenefits =
			response.getMonthlyBenefits();

		/*
		 * 과거 연도를 조회했기 때문에
		 * 1월부터 12월까지 반환한다.
		 */
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

		/*
		 * 혜택이 없는 3월 이후에도 누적 금액은 유지된다.
		 */
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

		List<DailyBenefitAmountVO> rows = List.of(
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

		/*
		 * LEFT JOIN 결과이므로 결제가 없는 카드도
		 * 카드 정보가 담긴 한 행으로 조회될 수 있다.
		 */
		DailyBenefitAmountVO row =
			createCardRow(USER_CARD_ID, 10_000L);

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
				assertThat(month.getMonthlyBenefitAmount())
					.isEqualTo(0L);
				assertThat(month.getAccumulatedBenefitAmount())
					.isEqualTo(0L);
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

		DailyBenefitAmountVO firstCard =
			createCardRow(100L, 10_000L);

		firstCard.setBenefitDate(
			LocalDate.of(BASE_YEAR, 1, 10)
		);
		firstCard.setDailyBenefitAmount(
			BigDecimal.valueOf(2_000L)
		);

		DailyBenefitAmountVO secondCard =
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

		assertThat(responses.get(0).getAccumulatedBenefit())
			.isEqualTo(2_000L);
		assertThat(responses.get(1).getAccumulatedBenefit())
			.isEqualTo(5_000L);
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
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage(
				"year는 2000년부터 현재 연도 사이여야 합니다."
			);

		verifyNoInteractions(benefitMapper);
	}

	@Test
	void getAnnualFeeBreakEvenThrowsWhenYearIsInTheFuture() {
		int futureYear =
			Year.now().getValue() + 1;

		assertThatThrownBy(
			() -> benefitService.getAnnualFeeBreakEven(
				USER_ID,
				futureYear
			)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage(
				"year는 2000년부터 현재 연도 사이여야 합니다."
			);

		verifyNoInteractions(benefitMapper);
	}
}
