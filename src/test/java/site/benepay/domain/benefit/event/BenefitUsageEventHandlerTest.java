package site.benepay.domain.benefit.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.benefit.mapper.BenefitUsageMapper;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.event.PaymentCanceledEvent;

@ExtendWith(MockitoExtension.class)
class BenefitUsageEventHandlerTest {

	@Mock
	private BenefitUsageMapper benefitUsageMapper;

	@InjectMocks
	private BenefitUsageEventHandler eventHandler;

	@Test
	void handlePaymentApprovedUpsertsUsageWhenABenefitWasApplied() {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			100L, 7L, "5813", LocalDateTime.of(2026, 8, 12, 15, 30),
			BigDecimal.valueOf(9000), BigDecimal.valueOf(1000), "카페 할인", 1L, "스타벅스 강남점"
		);

		eventHandler.handlePaymentApproved(event);

		verify(benefitUsageMapper).upsertMonthlyUsage(7L, "카페 할인", 2026, "202608", 1000L);
	}

	@Test
	void handlePaymentApprovedSkipsUpsertWhenNoBenefitWasApplied() {
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			100L, 7L, "5813", LocalDateTime.of(2026, 8, 12, 15, 30),
			BigDecimal.valueOf(9000), BigDecimal.ZERO, null, 1L, "스타벅스 강남점"
		);

		eventHandler.handlePaymentApproved(event);

		verify(benefitUsageMapper, never()).upsertMonthlyUsage(
			eq(7L), eq((String) null), eq(2026), eq("202608"), eq(0L));
	}

	@Test
	void handlePaymentCanceledDecrementsUsageInTheOriginalApprovalMonth() {
		// approvedAt은 취소 시각이 아니라 원래 승인 시각이다 - 소진액은 그 달에서 빠져야 한다.
		PaymentCanceledEvent event = new PaymentCanceledEvent(
			100L, 7L, "5813", LocalDateTime.of(2026, 7, 31, 23, 50),
			BigDecimal.valueOf(9000), BigDecimal.valueOf(1000), "카페 할인"
		);
		when(benefitUsageMapper.decrementMonthlyUsage(7L, "카페 할인", "202607", 1000L)).thenReturn(1);

		eventHandler.handlePaymentCanceled(event);

		verify(benefitUsageMapper).decrementMonthlyUsage(7L, "카페 할인", "202607", 1000L);
	}

	@Test
	void handlePaymentCanceledSkipsDecrementWhenNoBenefitWasApplied() {
		PaymentCanceledEvent event = new PaymentCanceledEvent(
			100L, 7L, "5813", LocalDateTime.of(2026, 7, 31, 23, 50),
			BigDecimal.valueOf(9000), BigDecimal.ZERO, null
		);

		eventHandler.handlePaymentCanceled(event);

		verify(benefitUsageMapper, never()).decrementMonthlyUsage(
			eq(7L), eq((String) null), eq("202607"), eq(0L));
	}

	@Test
	void handlePaymentCanceledDoesNotThrowWhenNoAggregateRowExists() {
		PaymentCanceledEvent event = new PaymentCanceledEvent(
			100L, 7L, "5813", LocalDateTime.of(2026, 7, 31, 23, 50),
			BigDecimal.valueOf(9000), BigDecimal.valueOf(1000), "카페 할인"
		);
		when(benefitUsageMapper.decrementMonthlyUsage(7L, "카페 할인", "202607", 1000L)).thenReturn(0);

		assertThatCode(() -> eventHandler.handlePaymentCanceled(event)).doesNotThrowAnyException();
	}
}
