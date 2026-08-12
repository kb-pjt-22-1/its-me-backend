package site.benepay.domain.benefit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto;
import site.benepay.domain.benefit.dto.AnnualFeeBreakEvenResponseDto.MonthlyBenefitDto;
import site.benepay.domain.benefit.mapper.BenefitMapper;
import site.benepay.domain.benefit.vo.DailyBenefitAmountVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitServiceImpl implements BenefitService {

	private final BenefitMapper benefitMapper;

	@Override
	public List<AnnualFeeBreakEvenResponseDto> getAnnualFeeBreakEven(
		Long userId,
		int year
	) {
		LocalDateTime now = LocalDateTime.now();

		validateYear(year, now.getYear());

		LocalDateTime startPaymentTime = LocalDate
			.of(year, 1, 1)
			.atStartOfDay();

		LocalDateTime endPaymentTime =
			calculateEndPaymentTime(year, now);

		List<DailyBenefitAmountVO> rows =
			benefitMapper.findAnnualFeeBenefitsByUserId(
				userId,
				startPaymentTime,
				endPaymentTime
			);

		Map<Long, List<DailyBenefitAmountVO>> rowsByUserCardId =
			groupByUserCardId(rows);

		List<AnnualFeeBreakEvenResponseDto> responses =
			new ArrayList<>();

		for (List<DailyBenefitAmountVO> cardRows
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
	 * LinkedHashMap을 사용해 SQL에서 정렬한
	 * 대표카드 우선순위를 그대로 유지한다.
	 */
	private Map<Long, List<DailyBenefitAmountVO>> groupByUserCardId(
		List<DailyBenefitAmountVO> rows
	) {
		Map<Long, List<DailyBenefitAmountVO>> groupedRows =
			new LinkedHashMap<>();

		for (DailyBenefitAmountVO row : rows) {
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
		List<DailyBenefitAmountVO> cardRows
	) {
		DailyBenefitAmountVO card = cardRows.get(0);

		long annualFee = card.getAnnualFee();
		long accumulatedBenefit = 0L;

		LocalDate breakEvenDate = null;

		Map<YearMonth, Long> monthlyBenefitAmounts =
			new LinkedHashMap<>();

		/*
		 * SQL 결과가 결제일 오름차순으로 정렬되어 있으므로,
		 * 누적 혜택이 처음 연회비 이상이 된 날짜를
		 * 본전 달성일로 사용할 수 있다.
		 */
		for (DailyBenefitAmountVO row : cardRows) {
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

				breakEvenDate = row.getBenefitDate();
			}
		}

		long netBenefit =
			accumulatedBenefit - annualFee;

		long remainingAmount =
			Math.max(annualFee - accumulatedBenefit, 0L);

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
	 * 현재 연도는 현재 월까지만 반환하고,
	 * 과거 연도는 12월까지 반환한다.
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
	 * 이를 통해 현재 날짜보다 미래인 seed 결제가
	 * 혜택 금액에 포함되는 것을 막는다.
	 *
	 * 과거 연도는 다음 연도 1월 1일 전까지 조회한다.
	 */
	private LocalDateTime calculateEndPaymentTime(
		int year,
		LocalDateTime now
	) {
		if (year == now.getYear()) {
			return now;
		}

		return LocalDate
			.of(year + 1, 1, 1)
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
			throw new IllegalArgumentException(
				"year는 2000년부터 현재 연도 사이여야 합니다."
			);
		}
	}
}
