package site.benepay.domain.card.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.event.PaymentCanceledEvent;

@Component
@RequiredArgsConstructor
public class CardPerformanceEventHandler {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMM");

	private final CardMapper cardMapper;

	@EventListener
	public void handlePaymentApproved(PaymentApprovedEvent event) {
		String targetYearMonth = event.approvedAt()
			.format(YEAR_MONTH_FORMATTER);

		int affectedRows = cardMapper.addMonthlySpending(
			event.userCardId(),
			targetYearMonth,
			event.performanceAmount()
		);

		if (affectedRows < 1) {
			throw new IllegalStateException("카드 실적 갱신에 실패했습니다.");
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

		if (affectedRows != 1) {
			throw new IllegalStateException("카드 실적 차감에 실패했습니다.");
		}
	}

	private String toYearMonth(LocalDateTime approvedAt) {
		return approvedAt.format(YEAR_MONTH_FORMATTER);
	}

}
