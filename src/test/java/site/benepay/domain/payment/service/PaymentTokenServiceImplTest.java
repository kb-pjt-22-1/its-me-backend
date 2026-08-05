package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.PaymentTokenNotFoundException;
import site.benepay.common.exception.PaymentTokenNotUsableException;
import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.vo.PaymentTokenVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTokenServiceImplTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 2L;
	private static final Long MERCHANT_ID = 3L;
	private static final Long PAYMENT_ID = 100L;
	private static final String PAYMENT_TOKEN_ID = "11111111-1111-1111-1111-111111111111";
	private static final BigDecimal ORIGINAL_AMOUNT = BigDecimal.valueOf(10000);
	private static final BigDecimal DISCOUNT_AMOUNT = BigDecimal.valueOf(1000);

	@Mock
	private PaymentTokenStore paymentTokenStore;

	@Mock
	private PaymentService paymentService;

	private PaymentTokenService paymentTokenService;

	@BeforeEach
	void setUp() {
		paymentTokenService = new PaymentTokenServiceImpl(paymentTokenStore, paymentService);
	}

	private PaymentTokenVO token(String status) {
		return new PaymentTokenVO(PAYMENT_TOKEN_ID, USER_ID, USER_CARD_ID, MERCHANT_ID,
			ORIGINAL_AMOUNT, DISCOUNT_AMOUNT, status, LocalDateTime.now().toString());
	}

	// ---- issueToken ----

	@Test
	void issueTokenDelegatesToTheStoreAndMapsTheResult() {
		when(paymentTokenStore.issue(USER_ID, USER_CARD_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT))
			.thenReturn(token(PaymentTokenStore.STATUS_ISSUED));

		PaymentTokenResponseDto response = paymentTokenService.issueToken(
			USER_ID, USER_CARD_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getTokenValue()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(response.getStatus()).isEqualTo("ISSUED");
		assertThat(response.getExpiresAt()).isEqualTo(response.getIssuedAt().plus(PaymentTokenStore.TTL));
	}

	// ---- getTokenStatus ----

	@Test
	void getTokenStatusReturnsTheMappedResponseWhenFound() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("ISSUED")));

		PaymentTokenResponseDto response = paymentTokenService.getTokenStatus(PAYMENT_TOKEN_ID);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
	}

	@Test
	void getTokenStatusThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.getTokenStatus(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotFoundException.class);
	}

	// ---- completeToken ----

	@Test
	void completeTokenCreatesAPaymentFromTheTokenAndMarksItCompleted() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("USED")));

		PaymentResponseDto created = PaymentResponseDto.builder()
			.paymentId(PAYMENT_ID)
			.paymentStatus("PENDING")
			.build();
		when(paymentService.createPayment(any(PaymentCreateRequestDto.class))).thenReturn(created);

		PaymentResponseDto completed = PaymentResponseDto.builder()
			.paymentId(PAYMENT_ID)
			.paymentStatus("COMPLETED")
			.build();
		when(paymentService.updatePaymentStatus(PAYMENT_ID, "COMPLETED")).thenReturn(completed);

		PaymentResponseDto response = paymentTokenService.completeToken(PAYMENT_TOKEN_ID);

		assertThat(response.getPaymentStatus()).isEqualTo("COMPLETED");

		ArgumentCaptor<PaymentCreateRequestDto> captor = ArgumentCaptor.forClass(PaymentCreateRequestDto.class);
		verify(paymentService).createPayment(captor.capture());
		PaymentCreateRequestDto request = captor.getValue();
		assertThat(request.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(request.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(request.getOriginalAmount()).isEqualByComparingTo(ORIGINAL_AMOUNT);
		assertThat(request.getDiscountAmount()).isEqualByComparingTo(DISCOUNT_AMOUNT);

		verify(paymentService).updatePaymentStatus(eq(PAYMENT_ID), eq("COMPLETED"));
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotFoundException.class);

		verify(paymentService, never()).createPayment(any());
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsAlreadyUsed() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("USED")));

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentService, never()).createPayment(any());
	}

	@Test
	void completeTokenThrowsWhenMarkingUsedLosesARaceToAnotherRequest() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentService, never()).createPayment(any());
	}
}
