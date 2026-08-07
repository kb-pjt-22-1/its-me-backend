package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import site.benepay.common.exception.UserCardNotAvailableException;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentHistoryVO;
import site.benepay.domain.payment.vo.PaymentTokenVO;
import site.benepay.domain.payment.vo.PaymentVO;
import site.benepay.domain.payment.vo.UserCardPaymentTokenVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTokenServiceImplTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 2L;
	private static final Long MERCHANT_ID = 3L;
	private static final Long RANDOM_MERCHANT_ID = 4L;
	private static final Long PAYMENT_ID = 100L;
	private static final String PAYMENT_TOKEN_ID = "11111111-1111-1111-1111-111111111111";
	private static final String CARD_PAYMENT_TOKEN = "9475000000001234";

	@Mock
	private PaymentTokenStore paymentTokenStore;

	@Mock
	private PaymentMapper paymentMapper;

	@Mock
	private MerchantService merchantService;

	private PaymentTokenService paymentTokenService;

	@BeforeEach
	void setUp() {
		paymentTokenService = new PaymentTokenServiceImpl(paymentTokenStore, paymentMapper, merchantService);
	}

	private PaymentTokenVO token(Long merchantId, String status) {
		return new PaymentTokenVO(PAYMENT_TOKEN_ID, USER_ID, USER_CARD_ID, merchantId, CARD_PAYMENT_TOKEN,
			"BARCODE", status, LocalDateTime.now().toString());
	}

	private UserCardPaymentTokenVO activeCardToken() {
		return new UserCardPaymentTokenVO(USER_CARD_ID, CARD_PAYMENT_TOKEN, LocalDate.now().plusYears(1));
	}

	private PaymentHistoryVO historyRow() {
		return PaymentHistoryVO.builder()
			.paymentId(PAYMENT_ID)
			.merchantName("스타벅스 강남점")
			.cardName("노리 체크카드")
			.panLast4("1234")
			.paymentTime(LocalDateTime.now())
			.originalAmount(BigDecimal.valueOf(15000))
			.discountAmount(BigDecimal.ZERO)
			.finalAmount(BigDecimal.valueOf(15000))
			.paymentStatus("APPROVED")
			.paymentMethod("BARCODE")
			.build();
	}

	// ---- issueToken ----

	@Test
	void issueTokenWithAKnownMerchantValidatesItAndPassesItToTheStore() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID))
			.thenReturn(Optional.of(activeCardToken()));
		when(paymentTokenStore.issue(USER_ID, USER_CARD_ID, MERCHANT_ID, CARD_PAYMENT_TOKEN))
			.thenReturn(token(MERCHANT_ID, PaymentTokenStore.STATUS_ISSUED));

		PaymentTokenResponseDto response = paymentTokenService.issueToken(USER_ID, USER_CARD_ID, MERCHANT_ID);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getTokenValue()).isEqualTo(PAYMENT_TOKEN_ID);
		verify(merchantService).getMerchant(MERCHANT_ID);
	}

	@Test
	void issueTokenWithoutAMerchantSkipsMerchantValidation() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID))
			.thenReturn(Optional.of(activeCardToken()));
		when(paymentTokenStore.issue(USER_ID, USER_CARD_ID, null, CARD_PAYMENT_TOKEN))
			.thenReturn(token(null, PaymentTokenStore.STATUS_ISSUED));

		PaymentTokenResponseDto response = paymentTokenService.issueToken(USER_ID, USER_CARD_ID, null);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		verify(merchantService, never()).getMerchant(any());
	}

	@Test
	void issueTokenThrowsWhenTheCardIsNotOwnedOrInactiveAndNeverCallsTheStore() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.issueToken(USER_ID, USER_CARD_ID, null))
			.isInstanceOf(UserCardNotAvailableException.class);

		verify(paymentTokenStore, never()).issue(any(), any(), any(), any());
	}

	// ---- getTokenStatus ----

	@Test
	void getTokenStatusReturnsTheMappedResponseWhenFound() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));

		PaymentTokenResponseDto response = paymentTokenService.getTokenStatus(PAYMENT_TOKEN_ID);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getStatus()).isEqualTo("ISSUED");
	}

	@Test
	void getTokenStatusThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.getTokenStatus(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotFoundException.class);
	}

	// ---- completeToken ----

	@Test
	void completeTokenUsesTheMerchantAlreadyOnTheTokenWhenPresent() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID))
			.thenReturn(Optional.of(token(MERCHANT_ID, "USED")));
		when(paymentMapper.findByPaymentId(any())).thenReturn(Optional.of(historyRow()));

		paymentTokenService.completeToken(PAYMENT_TOKEN_ID);

		ArgumentCaptor<PaymentVO> captor = ArgumentCaptor.forClass(PaymentVO.class);
		verify(paymentMapper).insertPayment(captor.capture());
		assertThat(captor.getValue().getMerchantId()).isEqualTo(MERCHANT_ID);
		verify(paymentMapper, never()).findRandomMerchantId();
	}

	@Test
	void completeTokenGeneratesARandomMerchantWhenTheTokenHasNone() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(null, "ISSUED")));
		when(paymentMapper.findRandomMerchantId()).thenReturn(Optional.of(RANDOM_MERCHANT_ID));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID))
			.thenReturn(Optional.of(token(null, "USED")));
		when(paymentMapper.findByPaymentId(any())).thenReturn(Optional.of(historyRow()));

		PaymentHistoryResponseDto response = paymentTokenService.completeToken(PAYMENT_TOKEN_ID);

		assertThat(response.getPaymentStatus()).isEqualTo("APPROVED");

		ArgumentCaptor<PaymentVO> captor = ArgumentCaptor.forClass(PaymentVO.class);
		verify(paymentMapper).insertPayment(captor.capture());
		PaymentVO inserted = captor.getValue();
		assertThat(inserted.getMerchantId()).isEqualTo(RANDOM_MERCHANT_ID);
		assertThat(inserted.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(inserted.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(inserted.getOriginalAmount()).isEqualByComparingTo(inserted.getFinalAmount());
		assertThat(inserted.getOriginalAmount()).isGreaterThanOrEqualTo(BigDecimal.valueOf(1000));
		assertThat(inserted.getOriginalAmount()).isLessThanOrEqualTo(BigDecimal.valueOf(50000));
		assertThat(inserted.getPaymentStatus()).isEqualTo("APPROVED");
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotFoundException.class);

		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsAlreadyUsed() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "USED")));

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void completeTokenThrowsWhenMarkingUsedLosesARaceToAnotherRequest() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentMapper, never()).insertPayment(any());
	}

	// ---- cancelToken ----

	@Test
	void cancelTokenCancelsAnIssuedTokenWithoutTouchingPayments() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));
		when(paymentTokenStore.cancelIfIssued(PAYMENT_TOKEN_ID))
			.thenReturn(Optional.of(token(MERCHANT_ID, "CANCELED")));

		PaymentTokenResponseDto response = paymentTokenService.cancelToken(PAYMENT_TOKEN_ID);

		assertThat(response.getStatus()).isEqualTo("CANCELED");
		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void cancelTokenThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.cancelToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotFoundException.class);

		verify(paymentTokenStore, never()).cancelIfIssued(any());
	}

	@Test
	void cancelTokenThrowsWhenTheTokenIsAlreadyUsedOrCanceled() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "USED")));
		when(paymentTokenStore.cancelIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.cancelToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);
	}
}