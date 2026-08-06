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
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentHistoryVO;
import site.benepay.domain.payment.vo.PaymentVO;

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

	private PaymentVO storedPayment() {
		PaymentVO payment = new PaymentVO();
		payment.setPaymentId(PAYMENT_ID);
		payment.setMerchantId(1L);
		payment.setUserCardId(2L);
		payment.setPaymentTime(LocalDateTime.now());
		payment.setOriginalAmount(BigDecimal.valueOf(10000));
		payment.setDiscountAmount(BigDecimal.valueOf(1000));
		payment.setFinalAmount(BigDecimal.valueOf(9000));
		payment.setPaymentStatus("APPROVED");
		payment.setPaymentMethod("BARCODE");
		return payment;
	}

	private PaymentHistoryVO historyRow(String status) {
		PaymentHistoryVO row = new PaymentHistoryVO();
		row.setPaymentId(PAYMENT_ID);
		row.setMerchantName("스타벅스 강남점");
		row.setCardName("노리 체크카드");
		row.setPanLast4("1234");
		row.setPaymentTime(LocalDateTime.now());
		row.setOriginalAmount(BigDecimal.valueOf(10000));
		row.setDiscountAmount(BigDecimal.valueOf(1000));
		row.setFinalAmount(BigDecimal.valueOf(9000));
		row.setPaymentStatus(status);
		row.setPaymentMethod("QR");
		return row;
	}

	// ---- getPayment ----

	@Test
	void getPaymentReturnsTheMappedResponseWhenFound() {
		when(paymentMapper.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(storedPayment()));

		PaymentResponseDto response = paymentService.getPayment(PAYMENT_ID);

		assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
		assertThat(response.getPaymentStatus()).isEqualTo("APPROVED");
		assertThat(response.getPaymentMethod()).isEqualTo("BARCODE");
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
			.thenReturn(List.of(historyRow("APPROVED"), historyRow("CANCELED")));

		List<PaymentHistoryResponseDto> history = paymentService.getPaymentHistory(USER_ID);

		assertThat(history).hasSize(2);
		assertThat(history.get(0).getMerchantName()).isEqualTo("스타벅스 강남점");
		assertThat(history.get(0).getCardName()).isEqualTo("노리 체크카드");
		assertThat(history.get(0).getMaskedCardNumber()).isEqualTo("**** 1234");
		assertThat(history.get(0).getPaymentStatus()).isEqualTo("APPROVED");
		assertThat(history.get(1).getPaymentStatus()).isEqualTo("CANCELED");
	}

	@Test
	void getPaymentHistoryReturnsAnEmptyListWhenTheUserHasNoPayments() {
		when(paymentMapper.findPaymentHistoryByUserId(USER_ID)).thenReturn(List.of());

		assertThat(paymentService.getPaymentHistory(USER_ID)).isEmpty();
	}
}