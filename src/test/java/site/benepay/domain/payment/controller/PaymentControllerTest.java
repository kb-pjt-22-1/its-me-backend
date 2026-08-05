package site.benepay.domain.payment.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.dto.PaymentStatusUpdateRequestDto;
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

	private PaymentResponseDto sampleResponse(String status) {
		return PaymentResponseDto.builder()
			.paymentId(PAYMENT_ID)
			.paymentStatus(status)
			.build();
	}

	@Test
	void getPaymentHistoryReturnsOkWithTheServiceResult() {
		List<PaymentResponseDto> history = List.of(sampleResponse("COMPLETED"), sampleResponse("PENDING"));
		when(paymentService.getPaymentHistory(USER_ID)).thenReturn(history);

		ResponseEntity<List<PaymentResponseDto>> response = controller.getPaymentHistory(USER_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(history);
	}

	@Test
	void createPaymentReturnsCreatedWithTheServiceResult() {
		PaymentCreateRequestDto request = new PaymentCreateRequestDto(
			1L, 2L, BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), "CARD");
		PaymentResponseDto created = sampleResponse("PENDING");
		when(paymentService.createPayment(request)).thenReturn(created);

		ResponseEntity<PaymentResponseDto> response = controller.createPayment(USER_ID, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(created);
	}

	@Test
	void getPaymentReturnsOkWithTheServiceResult() {
		PaymentResponseDto found = sampleResponse("PENDING");
		when(paymentService.getPayment(PAYMENT_ID)).thenReturn(found);

		ResponseEntity<PaymentResponseDto> response = controller.getPayment(USER_ID, PAYMENT_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(found);
	}

	@Test
	void updatePaymentStatusDelegatesTheParsedStatusToTheService() {
		PaymentStatusUpdateRequestDto request = new PaymentStatusUpdateRequestDto("COMPLETED");
		PaymentResponseDto updated = sampleResponse("COMPLETED");
		when(paymentService.updatePaymentStatus(PAYMENT_ID, "COMPLETED")).thenReturn(updated);

		ResponseEntity<PaymentResponseDto> response = controller.updatePaymentStatus(USER_ID, PAYMENT_ID, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(updated);
	}
}
