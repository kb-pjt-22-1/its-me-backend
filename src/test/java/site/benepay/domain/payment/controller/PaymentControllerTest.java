package site.benepay.domain.payment.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.service.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long PAYMENT_ID = 100L;

	@Mock
	private PaymentService paymentService;

	private PaymentController controller;

	@BeforeEach
	void setUp() {
		controller = new PaymentController(paymentService);
	}

	@Test
	void getPaymentHistoryReturnsOkWithTheServiceResult() {
		List<PaymentHistoryResponseDto> history = List.of(
			PaymentHistoryResponseDto.builder().paymentId(PAYMENT_ID).paymentStatus("APPROVED").build());
		when(paymentService.getPaymentHistory(USER_ID, "202608")).thenReturn(history);

		ResponseEntity<List<PaymentHistoryResponseDto>> response = controller.getPaymentHistory(USER_ID, "202608");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(history);
	}

	@Test
	void getPaymentHistoryPassesANullYearMonthWhenOmitted() {
		List<PaymentHistoryResponseDto> history = List.of(
			PaymentHistoryResponseDto.builder().paymentId(PAYMENT_ID).paymentStatus("APPROVED").build());
		when(paymentService.getPaymentHistory(USER_ID, null)).thenReturn(history);

		ResponseEntity<List<PaymentHistoryResponseDto>> response = controller.getPaymentHistory(USER_ID, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(history);
	}

	@Test
	void getPaymentReturnsOkWithTheServiceResult() {
		PaymentHistoryResponseDto found =
			PaymentHistoryResponseDto.builder().paymentId(PAYMENT_ID).paymentStatus("APPROVED").build();
		when(paymentService.getPayment(PAYMENT_ID)).thenReturn(found);

		ResponseEntity<PaymentHistoryResponseDto> response = controller.getPayment(USER_ID, PAYMENT_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(found);
	}

	@Test
	void cancelPaymentReturnsOkWithTheServiceResult() {
		PaymentHistoryResponseDto canceled =
			PaymentHistoryResponseDto.builder().paymentId(PAYMENT_ID).paymentStatus("CANCELED").build();
		when(paymentService.cancelPayment(USER_ID, PAYMENT_ID)).thenReturn(canceled);

		ResponseEntity<PaymentHistoryResponseDto> response = controller.cancelPayment(USER_ID, PAYMENT_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(canceled);
	}
}