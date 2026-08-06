package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.PaymentNotFoundException;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentHistoryVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	private static final Long PAYMENT_ID = 100L;
	private static final Long USER_ID = 42L;

	@Mock
	private PaymentMapper paymentMapper;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentServiceImpl(paymentMapper);
	}

	private PaymentHistoryVO row(String status) {
		return PaymentHistoryVO.builder()
			.paymentId(PAYMENT_ID)
			.merchantName("스타벅스 강남점")
			.cardName("노리 체크카드")
			.panLast4("1234")
			.paymentTime(LocalDateTime.now())
			.originalAmount(BigDecimal.valueOf(10000))
			.discountAmount(BigDecimal.valueOf(1000))
			.finalAmount(BigDecimal.valueOf(9000))
			.paymentStatus(status)
			.paymentMethod("QR")
			.build();
	}

	// ---- getPayment ----

	@Test
	void getPaymentReturnsTheMappedResponseWhenFound() {
		when(paymentMapper.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(row("APPROVED")));

		PaymentHistoryResponseDto response = paymentService.getPayment(PAYMENT_ID);

		assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
		assertThat(response.getMerchantName()).isEqualTo("스타벅스 강남점");
		assertThat(response.getMaskedCardNumber()).isEqualTo("**** 1234");
		assertThat(response.getPaymentStatus()).isEqualTo("APPROVED");
		assertThat(response.getFinalAmount()).isEqualByComparingTo("9000");
	}

	@Test
	void getPaymentThrowsWhenNotFound() {
		when(paymentMapper.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentService.getPayment(PAYMENT_ID))
			.isInstanceOf(PaymentNotFoundException.class);
	}

	// ---- getPaymentHistory ----

	@Test
	void getPaymentHistoryReturnsTheEnrichedListForTheUser() {
		when(paymentMapper.findPaymentHistoryByUserId(USER_ID))
			.thenReturn(List.of(row("APPROVED"), row("CANCELED")));

		List<PaymentHistoryResponseDto> history = paymentService.getPaymentHistory(USER_ID);

		assertThat(history).hasSize(2);
		assertThat(history.get(0).getPaymentStatus()).isEqualTo("APPROVED");
		assertThat(history.get(1).getPaymentStatus()).isEqualTo("CANCELED");
	}

	@Test
	void getPaymentHistoryReturnsAnEmptyListWhenTheUserHasNoPayments() {
		when(paymentMapper.findPaymentHistoryByUserId(USER_ID)).thenReturn(List.of());

		assertThat(paymentService.getPaymentHistory(USER_ID)).isEmpty();
	}
}