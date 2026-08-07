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

import site.benepay.common.exception.InvalidPaymentAmountException;
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
	private static final Long PAYMENT_ID = 100L;
	private static final String PAYMENT_TOKEN_ID = "11111111-1111-1111-1111-111111111111";
	private static final String CARD_PAYMENT_TOKEN = "9475000000001234";
	private static final BigDecimal ORIGINAL_AMOUNT = BigDecimal.valueOf(10000);
	private static final BigDecimal DISCOUNT_AMOUNT = BigDecimal.valueOf(1000);

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

	private PaymentTokenVO token(String status) {
		return new PaymentTokenVO(PAYMENT_TOKEN_ID, USER_ID, USER_CARD_ID, CARD_PAYMENT_TOKEN,
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
			.originalAmount(ORIGINAL_AMOUNT)
			.discountAmount(DISCOUNT_AMOUNT)
			.finalAmount(ORIGINAL_AMOUNT.subtract(DISCOUNT_AMOUNT))
			.paymentStatus("APPROVED")
			.paymentMethod("BARCODE")
			.build();
	}

	// ---- issueToken ----

	@Test
	void issueTokenValidatesTheCardThenDelegatesToTheStore() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID))
			.thenReturn(Optional.of(activeCardToken()));
		when(paymentTokenStore.issue(USER_ID, USER_CARD_ID, CARD_PAYMENT_TOKEN))
			.thenReturn(token(PaymentTokenStore.STATUS_ISSUED));

		PaymentTokenResponseDto response = paymentTokenService.issueToken(USER_ID, USER_CARD_ID);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getTokenValue()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getStatus()).isEqualTo("ISSUED");
		assertThat(response.getExpiresAt()).isEqualTo(response.getIssuedAt().plus(PaymentTokenStore.TTL));
	}

	@Test
	void issueTokenThrowsWhenTheCardIsNotOwnedOrInactiveAndNeverCallsTheStore() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.issueToken(USER_ID, USER_CARD_ID))
			.isInstanceOf(UserCardNotAvailableException.class);

		verify(paymentTokenStore, never()).issue(USER_ID, USER_CARD_ID, CARD_PAYMENT_TOKEN);
	}

	// ---- getTokenStatus ----

	@Test
	void getTokenStatusReturnsTheMappedResponseWhenFound() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("ISSUED")));

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
	void completeTokenCreatesAnApprovedPaymentAndMarksTheTokenUsed() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("USED")));
		when(paymentMapper.findByPaymentId(any())).thenReturn(Optional.of(historyRow()));

		PaymentHistoryResponseDto response = paymentTokenService.completeToken(
			PAYMENT_TOKEN_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT);

		assertThat(response.getPaymentStatus()).isEqualTo("APPROVED");
		assertThat(response.getFinalAmount()).isEqualByComparingTo("9000");

		verify(merchantService).getMerchant(MERCHANT_ID);

		ArgumentCaptor<PaymentVO> captor = ArgumentCaptor.forClass(PaymentVO.class);
		verify(paymentMapper).insertPayment(captor.capture());
		PaymentVO inserted = captor.getValue();
		assertThat(inserted.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(inserted.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(inserted.getFinalAmount()).isEqualByComparingTo("9000");
		assertThat(inserted.getPaymentStatus()).isEqualTo("APPROVED");
		assertThat(inserted.getPaymentMethod()).isEqualTo("BARCODE");
	}

	@Test
	void completeTokenThrowsWhenDiscountIsLargerThanOriginalAmountAndNeverTouchesTheToken() {
		assertThatThrownBy(() -> paymentTokenService.completeToken(
			PAYMENT_TOKEN_ID, MERCHANT_ID, BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)))
			.isInstanceOf(InvalidPaymentAmountException.class);

		verify(paymentTokenStore, never()).find(any());
		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(
			PAYMENT_TOKEN_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT))
			.isInstanceOf(PaymentTokenNotFoundException.class);

		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsAlreadyUsed() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("USED")));

		assertThatThrownBy(() -> paymentTokenService.completeToken(
			PAYMENT_TOKEN_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(merchantService, never()).getMerchant(any());
		verify(paymentMapper, never()).insertPayment(any());
	}

	@Test
	void completeTokenThrowsWhenMarkingUsedLosesARaceToAnotherRequest() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token("ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(
			PAYMENT_TOKEN_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentMapper, never()).insertPayment(any());
	}
}