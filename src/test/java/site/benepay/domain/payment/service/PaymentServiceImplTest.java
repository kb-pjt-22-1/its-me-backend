package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.InvalidPaymentAmountException;
import site.benepay.common.exception.PaymentNotFoundException;
import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	private static final Long PAYMENT_ID = 100L;
	private static final Long MERCHANT_ID = 1L;
	private static final Long USER_CARD_ID = 2L;
	private static final Long USER_ID = 42L;

	@Mock
	private PaymentMapper paymentMapper;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentServiceImpl(paymentMapper);
	}

	private PaymentCreateRequestDto createRequest(BigDecimal originalAmount, BigDecimal discountAmount) {
		return new PaymentCreateRequestDto(MERCHANT_ID, USER_CARD_ID, originalAmount, discountAmount, "CARD");
	}

	private PaymentVO storedPayment(String status) {
		PaymentVO payment = new PaymentVO();
		payment.setPaymentId(PAYMENT_ID);
		payment.setMerchantId(MERCHANT_ID);
		payment.setUserCardId(USER_CARD_ID);
		payment.setPaymentTime(LocalDateTime.now());
		payment.setOriginalAmount(BigDecimal.valueOf(10000));
		payment.setDiscountAmount(BigDecimal.valueOf(1000));
		payment.setFinalAmount(BigDecimal.valueOf(9000));
		payment.setPaymentStatus(status);
		payment.setPaymentMethod("CARD");
		return payment;
	}

	// ---- createPayment ----

	@Test
	void createPaymentComputesFinalAmountAndInsertsAPendingPayment() {
		PaymentCreateRequestDto request = createRequest(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000));

		PaymentResponseDto response = paymentService.createPayment(request);

		ArgumentCaptor<PaymentVO> inserted = ArgumentCaptor.forClass(PaymentVO.class);
		verify(paymentMapper).insertPayment(inserted.capture());

		PaymentVO payment = inserted.getValue();
		assertThat(payment.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(payment.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(payment.getOriginalAmount()).isEqualByComparingTo("10000");
		assertThat(payment.getDiscountAmount()).isEqualByComparingTo("1000");
		assertThat(payment.getFinalAmount()).isEqualByComparingTo("9000");
		assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
		assertThat(payment.getPaymentTime()).isNotNull();

		assertThat(response.getFinalAmount()).isEqualByComparingTo("9000");
		assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
	}

	@Test
	void createPaymentWithNullDiscountTreatsItAsZero() {
		PaymentCreateRequestDto request = createRequest(BigDecimal.valueOf(10000), null);

		PaymentResponseDto response = paymentService.createPayment(request);

		assertThat(response.getDiscountAmount()).isEqualByComparingTo("0");
		assertThat(response.getFinalAmount()).isEqualByComparingTo("10000");
	}

	@Test
	void createPaymentWithDiscountLargerThanOriginalAmountThrowsAndNeverInserts() {
		PaymentCreateRequestDto request = createRequest(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000));

		assertThatThrownBy(() -> paymentService.createPayment(request))
			.isInstanceOf(InvalidPaymentAmountException.class);

		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void createPaymentWithDiscountExactlyEqualToOriginalAmountResultsInAZeroFinalAmount() {
		PaymentCreateRequestDto request = createRequest(BigDecimal.valueOf(1000), BigDecimal.valueOf(1000));

		PaymentResponseDto response = paymentService.createPayment(request);

		assertThat(response.getFinalAmount()).isEqualByComparingTo("0");
	}

	// ---- getPayment ----

	@Test
	void getPaymentReturnsTheMappedResponseWhenFound() {
		when(paymentMapper.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(storedPayment("PENDING")));

		PaymentResponseDto response = paymentService.getPayment(PAYMENT_ID);

		assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
		assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
	}

	@Test
	void getPaymentThrowsWhenNotFound() {
		when(paymentMapper.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentService.getPayment(PAYMENT_ID))
			.isInstanceOf(PaymentNotFoundException.class);
	}

	// ---- updatePaymentStatus ----

	@Test
	void updatePaymentStatusUpdatesThenReturnsTheRefreshedPayment() {
		when(paymentMapper.updatePaymentStatus(PAYMENT_ID, "COMPLETED")).thenReturn(1);
		when(paymentMapper.findByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(storedPayment("COMPLETED")));

		PaymentResponseDto response = paymentService.updatePaymentStatus(PAYMENT_ID, "COMPLETED");

		assertThat(response.getPaymentStatus()).isEqualTo("COMPLETED");
		verify(paymentMapper).updatePaymentStatus(eq(PAYMENT_ID), eq("COMPLETED"));
	}

	@Test
	void updatePaymentStatusThrowsWhenNoRowWasUpdatedAndNeverReReads() {
		when(paymentMapper.updatePaymentStatus(PAYMENT_ID, "COMPLETED")).thenReturn(0);

		assertThatThrownBy(() -> paymentService.updatePaymentStatus(PAYMENT_ID, "COMPLETED"))
			.isInstanceOf(PaymentNotFoundException.class);

		verify(paymentMapper, never()).findByPaymentId(any());
	}

	// ---- getPaymentHistory ----

	@Test
	void getPaymentHistoryReturnsTheMappedListForTheUser() {
		when(paymentMapper.findPaymentsByUserId(USER_ID))
			.thenReturn(List.of(storedPayment("COMPLETED"), storedPayment("PENDING")));

		List<PaymentResponseDto> history = paymentService.getPaymentHistory(USER_ID);

		assertThat(history).hasSize(2);
		assertThat(history.get(0).getPaymentStatus()).isEqualTo("COMPLETED");
		assertThat(history.get(1).getPaymentStatus()).isEqualTo("PENDING");
	}

	@Test
	void getPaymentHistoryReturnsAnEmptyListWhenTheUserHasNoPayments() {
		when(paymentMapper.findPaymentsByUserId(USER_ID)).thenReturn(List.of());

		assertThat(paymentService.getPaymentHistory(USER_ID)).isEmpty();
	}
}