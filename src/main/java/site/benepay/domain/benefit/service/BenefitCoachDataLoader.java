package site.benepay.domain.benefit.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.CardData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.MonthlyUsageData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.PaymentData;
import site.benepay.domain.benefit.mapper.BenefitMapper;

@Component
@RequiredArgsConstructor
public class BenefitCoachDataLoader {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("uuuuMM");

	private final BenefitMapper benefitMapper;

	@Transactional(readOnly = true)
	public LoadedCoachingData load(
		Long userId,
		LocalDateTime now
	) {
		List<PaymentData> payments =
			benefitMapper.findBenefitCoachingPayments(
				userId,
				now.minusMonths(3),
				now
			);

		if (payments.isEmpty()) {
			return new LoadedCoachingData(
				payments,
				List.of(),
				List.of()
			);
		}

		YearMonth currentYearMonth =
			YearMonth.from(now);

		List<CardData> cards =
			benefitMapper.findBenefitCoachingCards(
				userId,
				currentYearMonth
					.minusMonths(1)
					.format(YEAR_MONTH_FORMATTER)
			);

		if (cards.isEmpty()) {
			return new LoadedCoachingData(
				payments,
				cards,
				List.of()
			);
		}

		List<MonthlyUsageData> monthlyUsages =
			benefitMapper.findBenefitCoachingMonthlyUsages(
				userId,
				currentYearMonth.format(
					YEAR_MONTH_FORMATTER
				)
			);

		return new LoadedCoachingData(
			payments,
			cards,
			monthlyUsages
		);
	}

	public record LoadedCoachingData(
		List<PaymentData> payments,
		List<CardData> cards,
		List<MonthlyUsageData> monthlyUsages
	) {
	}
}