package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import site.benepay.common.util.RedisKeys;
import site.benepay.domain.payment.vo.PaymentTokenVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTokenStoreTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 2L;
	private static final Long MERCHANT_ID = 3L;
	private static final BigDecimal ORIGINAL_AMOUNT = BigDecimal.valueOf(10000);
	private static final BigDecimal DISCOUNT_AMOUNT = BigDecimal.valueOf(1000);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private PaymentTokenStore store;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		store = new PaymentTokenStore(redisTemplate);
	}

	private PaymentTokenVO issue() {
		return store.issue(USER_ID, USER_CARD_ID, MERCHANT_ID, ORIGINAL_AMOUNT, DISCOUNT_AMOUNT);
	}

	@Test
	void issueSavesTheTokenUnderItsKeyWithTheConfiguredTtl() {
		PaymentTokenVO token = issue();

		assertThat(token.getPaymentTokenId()).isNotBlank();
		assertThat(token.getUserId()).isEqualTo(USER_ID);
		assertThat(token.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(token.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(token.getOriginalAmount()).isEqualByComparingTo(ORIGINAL_AMOUNT);
		assertThat(token.getStatus()).isEqualTo(PaymentTokenStore.STATUS_ISSUED);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(
			eq(RedisKeys.paymentToken(token.getPaymentTokenId())),
			jsonCaptor.capture(),
			eq(PaymentTokenStore.TTL));
		assertThat(jsonCaptor.getValue()).contains(token.getPaymentTokenId());
	}

	@Test
	void issueGeneratesADifferentTokenIdEachTime() {
		PaymentTokenVO first = issue();
		PaymentTokenVO second = issue();

		assertThat(first.getPaymentTokenId()).isNotEqualTo(second.getPaymentTokenId());
	}

	@Test
	void findReturnsTheStoredTokenWhenThePresentKeyHoldsIt() {
		String paymentTokenId = capturedTokenIdAfterIssuing();

		Optional<PaymentTokenVO> found = store.find(paymentTokenId);

		assertThat(found).isPresent();
		assertThat(found.get().getPaymentTokenId()).isEqualTo(paymentTokenId);
		assertThat(found.get().getMerchantId()).isEqualTo(MERCHANT_ID);
	}

	@Test
	void findReturnsEmptyWhenTheKeyIsMissingOrExpired() {
		when(valueOperations.get(RedisKeys.paymentToken("unknown"))).thenReturn(null);

		assertThat(store.find("unknown")).isEmpty();
	}

	@Test
	void markUsedIfIssuedFlipsAnIssuedTokenToUsedAndResavesWithAShorterTtl() {
		String[] savedJson = new String[1];
		captureSetCalls(savedJson);

		PaymentTokenVO issued = issue();
		when(valueOperations.get(RedisKeys.paymentToken(issued.getPaymentTokenId())))
			.thenAnswer(invocation -> savedJson[0]);

		Optional<PaymentTokenVO> result = store.markUsedIfIssued(issued.getPaymentTokenId());

		assertThat(result).isPresent();
		assertThat(result.get().getStatus()).isEqualTo(PaymentTokenStore.STATUS_USED);
		assertThat(savedJson[0]).contains(PaymentTokenStore.STATUS_USED);
	}

	@Test
	void markUsedIfIssuedDoesNothingWhenTheTokenIsMissing() {
		when(valueOperations.get(RedisKeys.paymentToken("unknown"))).thenReturn(null);

		assertThat(store.markUsedIfIssued("unknown")).isEmpty();
	}

	@Test
	void markUsedIfIssuedDoesNothingWhenTheTokenIsAlreadyUsed() {
		String[] savedJson = new String[1];
		captureSetCalls(savedJson);

		PaymentTokenVO issued = issue();
		when(valueOperations.get(RedisKeys.paymentToken(issued.getPaymentTokenId())))
			.thenAnswer(invocation -> savedJson[0]);
		store.markUsedIfIssued(issued.getPaymentTokenId());

		Optional<PaymentTokenVO> secondAttempt = store.markUsedIfIssued(issued.getPaymentTokenId());

		assertThat(secondAttempt).isEmpty();
	}

	private void captureSetCalls(String[] savedJson) {
		doAnswer(invocation -> {
			savedJson[0] = invocation.getArgument(1);
			return null;
		}).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
	}

	private String capturedTokenIdAfterIssuing() {
		String[] savedJson = new String[1];
		captureSetCalls(savedJson);

		PaymentTokenVO issued = issue();
		when(valueOperations.get(RedisKeys.paymentToken(issued.getPaymentTokenId()))).thenReturn(savedJson[0]);
		return issued.getPaymentTokenId();
	}
}
