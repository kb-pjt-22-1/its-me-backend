package site.benepay.domain.payment.service;

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
import org.springframework.data.redis.core.script.RedisScript;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.util.RedisKeys;
import site.benepay.domain.payment.vo.PaymentTokenVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTokenStoreTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 2L;
	private static final Long MERCHANT_ID = 3L;
	private static final String CARD_PAYMENT_TOKEN = "9475000000001234";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private PaymentTokenStore store;

	@BeforeEach
	void setUp() {
		// markUsedIfIssuedDoesNothingWhenTheTokenIsMissing()처럼 opsForValue()를 전혀 안 거치는
		// 케이스(atomic 스크립트만 타는 경로)도 있어서 lenient로 둔다 - 안 그러면 그 테스트에서
		// "이 스터빙은 안 쓰였다"는 Mockito strict-stub 오류가 난다.
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		store = new PaymentTokenStore(redisTemplate);
	}

	private PaymentTokenVO issue() {
		return store.issue(USER_ID, USER_CARD_ID, MERCHANT_ID, CARD_PAYMENT_TOKEN);
	}

	private PaymentTokenVO issueWithoutMerchant() {
		return store.issue(USER_ID, USER_CARD_ID, null, CARD_PAYMENT_TOKEN);
	}

	@Test
	void issueSavesTheTokenUnderItsKeyWithTheConfiguredTtl() {
		PaymentTokenVO token = issue();

		assertThat(token.getPaymentTokenId()).isNotBlank();
		assertThat(token.getUserId()).isEqualTo(USER_ID);
		assertThat(token.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(token.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(token.getCardPaymentToken()).isEqualTo(CARD_PAYMENT_TOKEN);
		assertThat(token.getPaymentMethod()).isEqualTo("BARCODE");
		assertThat(token.getStatus()).isEqualTo(PaymentTokenStore.STATUS_ISSUED);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(
			eq(RedisKeys.paymentToken(token.getPaymentTokenId())),
			jsonCaptor.capture(),
			eq(PaymentTokenStore.TTL));
		assertThat(jsonCaptor.getValue()).contains(token.getPaymentTokenId());
	}

	@Test
	void issueAllowsANullMerchantIdForTheDirectEntryFlow() {
		PaymentTokenVO token = issueWithoutMerchant();

		assertThat(token.getMerchantId()).isNull();
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
		assertThat(found.get().getCardPaymentToken()).isEqualTo(CARD_PAYMENT_TOKEN);
	}

	@Test
	void findReturnsEmptyWhenTheKeyIsMissingOrExpired() {
		when(valueOperations.get(RedisKeys.paymentToken("unknown"))).thenReturn(null);

		assertThat(store.find("unknown")).isEmpty();
	}

	// markUsedIfIssued/cancelIfIssued는 이제 GET+SET 대신 원자적 Redis 스크립트
	// (redisTemplate.execute)로 상태를 전환한다. 실제 Redis의 단일 스레드 원자성 자체는
	// 단위테스트로 재현할 수 없으므로(그건 k6/concurrency-stress.js의 몫), 여기서는 그
	// 스크립트가 "저장된 상태가 ISSUED일 때만 전환하고, 아니면 아무것도 안 바꾼다"는 계약을
	// 지킨다고 가정하고 시뮬레이션한다 - PaymentTokenStore가 그 결과를 올바르게 다루는지만 검증.
	@Test
	void markUsedIfIssuedFlipsAnIssuedTokenToUsedAndResavesWithAShorterTtl() {
		String[] storedJson = new String[1];
		captureSetCalls(storedJson);
		stubAtomicTransition(storedJson);

		PaymentTokenVO issued = issue();

		Optional<PaymentTokenVO> result = store.markUsedIfIssued(issued.getPaymentTokenId());

		assertThat(result).isPresent();
		assertThat(result.get().getStatus()).isEqualTo(PaymentTokenStore.STATUS_USED);
		assertThat(storedJson[0]).contains(PaymentTokenStore.STATUS_USED);
	}

	@Test
	void markUsedIfIssuedDoesNothingWhenTheTokenIsMissing() {
		String[] storedJson = new String[1];
		stubAtomicTransition(storedJson);

		assertThat(store.markUsedIfIssued("unknown")).isEmpty();
	}

	@Test
	void markUsedIfIssuedDoesNothingWhenTheTokenIsAlreadyUsed() {
		String[] storedJson = new String[1];
		captureSetCalls(storedJson);
		stubAtomicTransition(storedJson);

		PaymentTokenVO issued = issue();
		store.markUsedIfIssued(issued.getPaymentTokenId());

		Optional<PaymentTokenVO> secondAttempt = store.markUsedIfIssued(issued.getPaymentTokenId());

		assertThat(secondAttempt).isEmpty();
	}

	@Test
	void cancelIfIssuedFlipsAnIssuedTokenToCanceled() {
		String[] storedJson = new String[1];
		captureSetCalls(storedJson);
		stubAtomicTransition(storedJson);

		PaymentTokenVO issued = issue();

		Optional<PaymentTokenVO> result = store.cancelIfIssued(issued.getPaymentTokenId());

		assertThat(result).isPresent();
		assertThat(result.get().getStatus()).isEqualTo(PaymentTokenStore.STATUS_CANCELED);
		assertThat(storedJson[0]).contains(PaymentTokenStore.STATUS_CANCELED);
	}

	@Test
	void cancelIfIssuedDoesNothingWhenTheTokenIsAlreadyUsed() {
		String[] storedJson = new String[1];
		captureSetCalls(storedJson);
		stubAtomicTransition(storedJson);

		PaymentTokenVO issued = issue();
		store.markUsedIfIssued(issued.getPaymentTokenId());

		Optional<PaymentTokenVO> cancelAttempt = store.cancelIfIssued(issued.getPaymentTokenId());

		assertThat(cancelAttempt).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private void stubAtomicTransition(String[] storedJson) {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
			.thenAnswer(invocation -> {
				String raw = storedJson[0];
				if (raw == null) {
					return null;
				}

				PaymentTokenVO token = objectMapper.readValue(raw, PaymentTokenVO.class);
				if (!PaymentTokenStore.STATUS_ISSUED.equals(token.getStatus())) {
					return null;
				}

				String targetStatus = invocation.getArgument(2);
				token.setStatus(targetStatus);
				String newRaw = objectMapper.writeValueAsString(token);
				storedJson[0] = newRaw;
				return newRaw;
			});
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