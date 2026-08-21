package site.benepay.domain.card.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.CardPerformanceUpdateException;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.event.PaymentCanceledEvent;

@ExtendWith(MockitoExtension.class)
class CardPerformanceEventHandlerTest {

	@Mock
	private CardMapper cardMapper;

	@InjectMocks
	private CardPerformanceEventHandler eventHandler;

	@Test
	@DisplayName("결제가 승인되면 승인 월의 카드 실적을 증가시킨다")
	void handlePaymentApproved_addsMonthlySpending() {
		// given
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			100L,
			7L,
			"5813",
			LocalDateTime.of(2026, 8, 12, 15, 30),
			BigDecimal.valueOf(9000),
			BigDecimal.valueOf(1000),
			null,
			1L,
			"스타벅스 강남점"
		);

		when(cardMapper.addMonthlySpending(
			7L,
			"202608",
			BigDecimal.valueOf(9000)
		)).thenReturn(1);

		// when
		eventHandler.handlePaymentApproved(event);

		// then
		verify(cardMapper).addMonthlySpending(
			7L,
			"202608",
			BigDecimal.valueOf(9000)
		);
	}

	@Test
	@DisplayName("결제가 취소되면 원래 승인 월의 카드 실적을 차감한다")
	void handlePaymentCanceled_subtractsMonthlySpending() {
		// given
		PaymentCanceledEvent event = new PaymentCanceledEvent(
			100L,
			7L,
			"5813",
			LocalDateTime.of(2026, 7, 31, 23, 50),
			BigDecimal.valueOf(9000),
			BigDecimal.valueOf(1000),
			null
		);

		when(cardMapper.subtractMonthlySpending(
			7L,
			"202607",
			BigDecimal.valueOf(9000)
		)).thenReturn(1);

		// when
		eventHandler.handlePaymentCanceled(event);

		// then
		verify(cardMapper).subtractMonthlySpending(
			7L,
			"202607",
			BigDecimal.valueOf(9000)
		);
	}

	@Test
	@DisplayName("승인 실적 갱신에 실패하면 카드 실적 갱신 예외가 발생한다")
	void handlePaymentApproved_throwsExceptionWhenUpdateFails() {
		// given
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			100L,
			7L,
			"5813",
			LocalDateTime.of(2026, 8, 12, 15, 30),
			BigDecimal.valueOf(9000),
			BigDecimal.valueOf(1000),
			null,
			1L,
			"스타벅스 강남점"
		);

		when(cardMapper.addMonthlySpending(
			7L,
			"202608",
			BigDecimal.valueOf(9000)
		)).thenReturn(0);

		// when & then
		assertThrows(
			CardPerformanceUpdateException.class,
			() -> eventHandler.handlePaymentApproved(event)
		);
	}

	@Test
	@DisplayName("취소 실적 집계 행이 없어도 예외가 발생하지 않는다")
	void handlePaymentCanceled_doesNotThrowWhenUpdateFails() {
		// given
		PaymentCanceledEvent event = new PaymentCanceledEvent(
			100L,
			7L,
			"5813",
			LocalDateTime.of(2026, 7, 31, 23, 50),
			BigDecimal.valueOf(9000),
			BigDecimal.valueOf(1000),
			null
		);

		when(cardMapper.subtractMonthlySpending(
			7L,
			"202607",
			BigDecimal.valueOf(9000)
		)).thenReturn(0);

		// when & then
		assertDoesNotThrow(
			() -> eventHandler.handlePaymentCanceled(event)
		);

		verify(cardMapper).subtractMonthlySpending(
			7L,
			"202607",
			BigDecimal.valueOf(9000)
		);
	}
}
