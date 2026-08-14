package site.benepay.domain.card.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.common.exception.CardPerformanceUpdateException;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.event.PaymentCanceledEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardPerformanceEventHandler {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMM");

	private final CardMapper cardMapper;

	@EventListener
	public void handlePaymentApproved(PaymentApprovedEvent event) {
		String targetYearMonth = toYearMonth(event.approvedAt());

		int affectedRows = cardMapper.addMonthlySpending(
			event.userCardId(),
			targetYearMonth,
			event.performanceAmount()
		);

		if (affectedRows < 1) {
			throw new CardPerformanceUpdateException();
		}
	}

	@EventListener
	public void handlePaymentCanceled(PaymentCanceledEvent event) {
		String targetYearMonth = toYearMonth(event.approvedAt());

		int affectedRows = cardMapper.subtractMonthlySpending(
			event.userCardId(),
			targetYearMonth,
			event.performanceAmount()
		);

		if (affectedRows < 1) {
			log.warn(
				"카드 실적 집계 행이 없어 차감을 건너뜁니다: paymentId={}, userCardId={}, month={}",
				event.paymentId(),
				event.userCardId(),
				targetYearMonth
			);
		}
	}

	private static String toYearMonth(LocalDateTime approvedAt) {
		return approvedAt.format(YEAR_MONTH_FORMATTER);
	}

}
