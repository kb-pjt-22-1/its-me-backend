package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.PaymentTokenNotFoundException;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.common.exception.PaymentTokenNotUsableException;
import site.benepay.common.exception.UserCardNotAvailableException;
import site.benepay.domain.benefit.mapper.BenefitUsageMapper;
import site.benepay.domain.benefit.vo.BenefitUsageVO;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.CardBenefitContextVO;
import site.benepay.domain.payment.vo.PaymentHistoryVO;
import site.benepay.domain.payment.vo.PaymentTokenVO;
import site.benepay.domain.payment.vo.PaymentVO;
import site.benepay.domain.payment.vo.UserCardPaymentTokenVO;

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

	@Mock
	private BenefitUsageMapper benefitUsageMapper;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private PaymentTokenService paymentTokenService;

	@BeforeEach
	void setUp() {
		paymentTokenService = new PaymentTokenServiceImpl(paymentTokenStore, paymentMapper, merchantService,
			benefitUsageMapper, new ObjectMapper(), eventPublisher);
	}

	private PaymentTokenVO token(Long merchantId, String status) {
		return new PaymentTokenVO(PAYMENT_TOKEN_ID, USER_ID, USER_CARD_ID, merchantId, CARD_PAYMENT_TOKEN,
			"BARCODE", status, LocalDateTime.now().toString());
	}

	private UserCardPaymentTokenVO activeCardToken() {
		return new UserCardPaymentTokenVO(USER_CARD_ID, CARD_PAYMENT_TOKEN, LocalDate.now().plusYears(1));
	}

	private CardBenefitContextVO cardBenefitContext(long previousMonthSpendingAmount, String benefitsInfo) {
		CardBenefitContextVO context = new CardBenefitContextVO();
		context.setPreviousMonthSpendingAmount(previousMonthSpendingAmount);
		context.setBenefitsInfo(benefitsInfo);
		return context;
	}

	private BenefitUsageVO usageRow(String serviceName, long usedAmount, int usedCount) {
		BenefitUsageVO row = new BenefitUsageVO();
		row.setBenefitServiceName(serviceName);
		row.setUsedAmount(usedAmount);
		row.setUsedCount(usedCount);
		return row;
	}

	private static final String CAFE_STATEMENT_DISCOUNT_50_PERCENT = "{\"performanceTiers\":[{\"minimumSpending\":0,"
		+ "\"benefits\":[{\"serviceName\":\"카페 할인\",\"benefitType\":\"MERCHANT_CATEGORY\","
		+ "\"categoryCodes\":[\"5813\"],\"discountMethod\":\"STATEMENT_DISCOUNT\",\"discountRate\":50,"
		+ "\"minimumPaymentAmount\":0}]}]}";

	private PaymentHistoryVO historyRow() {
		return historyRow(null);
	}

	// findByPaymentId는 DB 재조회를 흉내내는 목이라, completeToken이 실제로 계산한 benefitServiceName을
	// 검증하려면(이벤트에 실리는 값은 이 재조회 결과 기준) 호출부에서 원하는 값을 넣어줘야 한다.
	private PaymentHistoryVO historyRow(String benefitServiceName) {
		return PaymentHistoryVO.builder()
			.paymentId(PAYMENT_ID)
			.userCardId(USER_CARD_ID)
			.merchantName("스타벅스 강남점")
			.categoryCode("5813")
			.cardName("노리 체크카드")
			.panLast4("1234")
			.paymentTime(LocalDateTime.now())
			.originalAmount(BigDecimal.valueOf(15000))
			.discountAmount(BigDecimal.ZERO)
			.finalAmount(BigDecimal.valueOf(15000))
			.benefitServiceName(benefitServiceName)
			.paymentStatus("APPROVED")
			.paymentMethod("BARCODE")
			.build();
	}

	// ---- issueToken ----

	@Test
	void issueTokenWithAKnownMerchantValidatesItAndPassesItToTheStore() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID))
			.thenReturn(Optional.of(activeCardToken()));
		when(paymentMapper.existsMerchant(MERCHANT_ID)).thenReturn(true);
		when(paymentTokenStore.issue(USER_ID, USER_CARD_ID, MERCHANT_ID, CARD_PAYMENT_TOKEN))
			.thenReturn(token(MERCHANT_ID, PaymentTokenStore.STATUS_ISSUED));

		PaymentTokenResponseDto response = paymentTokenService.issueToken(USER_ID, USER_CARD_ID, MERCHANT_ID);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		assertThat(response.getTokenValue()).isEqualTo(PAYMENT_TOKEN_ID);
		verify(paymentMapper).existsMerchant(MERCHANT_ID);
	}

	@Test
	void issueTokenThrowsWhenTheMerchantDoesNotExistAndNeverCallsTheStore() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID))
			.thenReturn(Optional.of(activeCardToken()));
		when(paymentMapper.existsMerchant(MERCHANT_ID)).thenReturn(false);

		assertThatThrownBy(() -> paymentTokenService.issueToken(USER_ID, USER_CARD_ID, MERCHANT_ID))
			.isInstanceOf(MerchantNotFoundException.class);

		verify(paymentTokenStore, never()).issue(any(), any(), any(), any());
	}

	@Test
	void issueTokenWithoutAMerchantSkipsMerchantValidation() {
		when(paymentMapper.findActiveCardPaymentToken(USER_ID, USER_CARD_ID))
			.thenReturn(Optional.of(activeCardToken()));
		when(paymentTokenStore.issue(USER_ID, USER_CARD_ID, null, CARD_PAYMENT_TOKEN))
			.thenReturn(token(null, PaymentTokenStore.STATUS_ISSUED));

		PaymentTokenResponseDto response = paymentTokenService.issueToken(USER_ID, USER_CARD_ID, null);

		assertThat(response.getPaymentTokenId()).isEqualTo(PAYMENT_TOKEN_ID);
		verify(paymentMapper, never()).existsMerchant(any());
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

		ArgumentCaptor<PaymentApprovedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		PaymentApprovedEvent event = eventCaptor.getValue();
		assertThat(event.userCardId()).isEqualTo(USER_CARD_ID);
		assertThat(event.categoryCode()).isEqualTo("5813");
		assertThat(event.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
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
		assertThat(inserted.getOriginalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
		assertThat(inserted.getPaymentStatus()).isEqualTo("APPROVED");
	}

	@Test
	void completeTokenAppliesTheBestMatchingBenefitAndPublishesItsServiceName() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID))
			.thenReturn(Optional.of(token(MERCHANT_ID, "USED")));
		when(paymentMapper.findByPaymentId(any())).thenReturn(Optional.of(historyRow("카페 할인")));
		when(merchantService.getMerchant(MERCHANT_ID))
			.thenReturn(MerchantResponseDto.builder().merchantId(MERCHANT_ID).categoryCode("5813").build());
		when(paymentMapper.findCardBenefitContext(eq(USER_CARD_ID), any()))
			.thenReturn(Optional.of(cardBenefitContext(0L, CAFE_STATEMENT_DISCOUNT_50_PERCENT)));

		paymentTokenService.completeToken(PAYMENT_TOKEN_ID);

		ArgumentCaptor<PaymentVO> captor = ArgumentCaptor.forClass(PaymentVO.class);
		verify(paymentMapper).insertPayment(captor.capture());
		PaymentVO inserted = captor.getValue();
		assertThat(inserted.getBenefitServiceName()).isEqualTo("카페 할인");
		BigDecimal expectedDiscount =
			BigDecimal.valueOf(Math.round(inserted.getOriginalAmount().doubleValue() * 0.5));
		assertThat(inserted.getDiscountAmount()).isEqualByComparingTo(expectedDiscount);
		assertThat(inserted.getFinalAmount())
			.isEqualByComparingTo(inserted.getOriginalAmount().subtract(inserted.getDiscountAmount()));

		ArgumentCaptor<PaymentApprovedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentApprovedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().benefitServiceName()).isEqualTo("카페 할인");
	}

	@Test
	void completeTokenCapsDiscountByAlreadyConsumedMonthlyLimit() {
		// 이 카드의 "카페 할인"은 정률 50%지만, 이번 달 이미 discountAmount와 거의 맞먹는 금액을
		// 소진했다고 가정하면(monthlyDiscountLimit 100원, 이미 100원 소진) 이번 결제엔 혜택이
		// 전혀 적용되지 않아야 한다.
		// BenefitJsonParser는 monthlyDiscountLimit을 "maximumDiscountAmountPerMonth"(또는 구형
		// "monthlyLimit") 키로 읽는다 - BenefitNode 필드명과 실제 JSON 키가 다르다.
		String benefitsInfoWithLimit = "{\"performanceTiers\":[{\"minimumSpending\":0,"
			+ "\"benefits\":[{\"serviceName\":\"카페 할인\",\"benefitType\":\"MERCHANT_CATEGORY\","
			+ "\"categoryCodes\":[\"5813\"],\"discountMethod\":\"STATEMENT_DISCOUNT\",\"discountRate\":50,"
			+ "\"minimumPaymentAmount\":0,\"maximumDiscountAmountPerMonth\":100}]}]}";

		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID))
			.thenReturn(Optional.of(token(MERCHANT_ID, "USED")));
		when(paymentMapper.findByPaymentId(any())).thenReturn(Optional.of(historyRow()));
		when(merchantService.getMerchant(MERCHANT_ID))
			.thenReturn(MerchantResponseDto.builder().merchantId(MERCHANT_ID).categoryCode("5813").build());
		when(paymentMapper.findCardBenefitContext(eq(USER_CARD_ID), any()))
			.thenReturn(Optional.of(cardBenefitContext(0L, benefitsInfoWithLimit)));
		when(benefitUsageMapper.findMonthlyUsageByUserCardId(eq(USER_CARD_ID), any()))
			.thenReturn(List.of(usageRow("카페 할인", 100L, 1)));

		paymentTokenService.completeToken(PAYMENT_TOKEN_ID);

		ArgumentCaptor<PaymentVO> captor = ArgumentCaptor.forClass(PaymentVO.class);
		verify(paymentMapper).insertPayment(captor.capture());
		PaymentVO inserted = captor.getValue();
		assertThat(inserted.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(inserted.getBenefitServiceName()).isNull();
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsMissingOrExpired() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotFoundException.class);

		verify(paymentMapper, never()).insertPayment(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void completeTokenThrowsWhenTheTokenIsAlreadyUsed() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "USED")));

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentMapper, never()).insertPayment(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void completeTokenThrowsWhenMarkingUsedLosesARaceToAnotherRequest() {
		when(paymentTokenStore.find(PAYMENT_TOKEN_ID)).thenReturn(Optional.of(token(MERCHANT_ID, "ISSUED")));
		when(paymentTokenStore.markUsedIfIssued(PAYMENT_TOKEN_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentTokenService.completeToken(PAYMENT_TOKEN_ID))
			.isInstanceOf(PaymentTokenNotUsableException.class);

		verify(paymentMapper, never()).insertPayment(any());
		verify(eventPublisher, never()).publishEvent(any());
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