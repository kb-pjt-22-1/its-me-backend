package site.benepay.domain.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.service.PaymentTokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTokenControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 2L;
	private static final String PAYMENT_TOKEN_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private PaymentTokenService paymentTokenService;

	private PaymentTokenController controller;

	@BeforeEach
	void setUp() {
		controller = new PaymentTokenController(paymentTokenService);
	}

	@Test
	void issueTokenReturnsCreatedWithTheServiceResult() {
		PaymentTokenCreateRequestDto request = new PaymentTokenCreateRequestDto(USER_CARD_ID, null);
		PaymentTokenResponseDto issued = PaymentTokenResponseDto.builder()
			.paymentTokenId(PAYMENT_TOKEN_ID)
			.status("ISSUED")
			.build();
		when(paymentTokenService.issueToken(USER_ID, USER_CARD_ID, null)).thenReturn(issued);

		ResponseEntity<PaymentTokenResponseDto> response = controller.issueToken(USER_ID, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(issued);
	}

	@Test
	void getTokenStatusReturnsOkWithTheServiceResult() {
		PaymentTokenResponseDto found = PaymentTokenResponseDto.builder()
			.paymentTokenId(PAYMENT_TOKEN_ID)
			.status("ISSUED")
			.build();
		when(paymentTokenService.getTokenStatus(PAYMENT_TOKEN_ID)).thenReturn(found);

		ResponseEntity<PaymentTokenResponseDto> response = controller.getTokenStatus(USER_ID, PAYMENT_TOKEN_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(found);
	}

	@Test
	void completeTokenReturnsOkWithTheServiceResult() {
		PaymentHistoryResponseDto completed = PaymentHistoryResponseDto.builder()
			.paymentId(100L)
			.paymentStatus("APPROVED")
			.build();
		when(paymentTokenService.completeToken(PAYMENT_TOKEN_ID)).thenReturn(completed);

		ResponseEntity<PaymentHistoryResponseDto> response = controller.completeToken(USER_ID, PAYMENT_TOKEN_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(completed);
	}

	@Test
	void cancelTokenReturnsOkWithTheServiceResult() {
		PaymentTokenResponseDto canceled = PaymentTokenResponseDto.builder()
			.paymentTokenId(PAYMENT_TOKEN_ID)
			.status("CANCELED")
			.build();
		when(paymentTokenService.cancelToken(PAYMENT_TOKEN_ID)).thenReturn(canceled);

		ResponseEntity<PaymentTokenResponseDto> response = controller.cancelToken(USER_ID, PAYMENT_TOKEN_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(canceled);
	}
}