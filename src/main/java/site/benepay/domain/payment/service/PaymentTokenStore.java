package site.benepay.domain.payment.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.util.RedisKeys;
import site.benepay.domain.payment.vo.PaymentTokenVO;

/**
 * 결제 바코드 토큰의 Redis 저장소. SignupVerificationStore와 동일한 issue/조회 패턴을 따른다.
 */
@Service
public class PaymentTokenStore {

	// 화면에 띄우는 동적 바코드 특성상 길게 살려두지 않는다. 정책 확정 전까지 임시값.
	// dto 패키지(PaymentTokenResponseDto)에서 expiresAt 계산에 참조하므로 public.
	public static final Duration TTL = Duration.ofMinutes(3);

	// 완료/취소 처리 직후 상태 조회(폴링 중이던 클라이언트)가 최종 상태를 볼 수 있게 짧게만 더 살려둔다.
	private static final Duration FINAL_STATE_TTL = Duration.ofSeconds(30);

	public static final String STATUS_ISSUED = "ISSUED";
	public static final String STATUS_USED = "USED";
	public static final String STATUS_CANCELED = "CANCELED";
	private static final String PAYMENT_METHOD_BARCODE = "BARCODE";

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	// transitionIfIssued를 GET 후 SET(check-then-act)으로 구현하면 동시에 여러 완료/취소
	// 요청이 들어왔을 때 전부 "ISSUED"를 보고 통과해버려 결제가 중복 생성될 수 있다(레이스
	// 컨디션 - k6 concurrency-stress.js로 10개 동시 요청 중 3건 중복 결제 재현됨). Redis는
	// 스크립트를 단일 스레드로 원자 실행하므로, 조회-검사-저장을 스크립트 하나로 묶어야
	// 동시 요청 중 정확히 하나만 전환에 성공하는 것을 보장할 수 있다.
	private static final RedisScript<String> TRANSITION_IF_ISSUED_SCRIPT = RedisScript.of(
		"local raw = redis.call('GET', KEYS[1]) "
			+ "if not raw then return false end "
			+ "local token = cjson.decode(raw) "
			+ "if token.status ~= '" + STATUS_ISSUED + "' then return false end "
			+ "token.status = ARGV[1] "
			+ "local newRaw = cjson.encode(token) "
			+ "redis.call('SET', KEYS[1], newRaw, 'EX', ARGV[2]) "
			+ "return newRaw",
		String.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public PaymentTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// merchantId는 nullable - 매장 페이지를 거쳐온 흐름이면 채워서, 결제 페이지 직접 진입이면 null로 발급한다.
	public PaymentTokenVO issue(Long userId, Long userCardId, Long merchantId, String cardPaymentToken) {
		String paymentTokenId = UUID.randomUUID().toString();
		PaymentTokenVO token = new PaymentTokenVO(
			paymentTokenId,
			userId,
			userCardId,
			merchantId,
			cardPaymentToken,
			PAYMENT_METHOD_BARCODE,
			STATUS_ISSUED,
			LocalDateTime.now(ZONE).toString()
		);

		save(token, TTL);

		return token;
	}

	// 상태 조회(폴링)에 쓰인다. 조회 후 삭제하지 않는다.
	public Optional<PaymentTokenVO> find(String paymentTokenId) {
		String raw = redisTemplate.opsForValue().get(RedisKeys.paymentToken(paymentTokenId));
		if (raw == null) {
			return Optional.empty();
		}
		return Optional.of(deserialize(raw));
	}

	// ISSUED 상태인 토큰만 USED로 바꾼다 (결제완료).
	public Optional<PaymentTokenVO> markUsedIfIssued(String paymentTokenId) {
		return transitionIfIssued(paymentTokenId, STATUS_USED);
	}

	// ISSUED 상태인 토큰만 CANCELED로 바꾼다 (바코드 화면에서 취소, payments 테이블은 건드리지 않는다 -
	// 아직 실제로 결제가 이뤄진 적이 없어서 취소할 결제 자체가 없다).
	public Optional<PaymentTokenVO> cancelIfIssued(String paymentTokenId) {
		return transitionIfIssued(paymentTokenId, STATUS_CANCELED);
	}

	// ISSUED 상태인 토큰만 targetStatus로 바꾼다. 이미 다른 상태거나 존재하지 않으면(만료 포함)
	// 아무것도 안 하고 빈 값을 돌려준다 - 상태 판단(예외를 던질지)은 Service의 책임으로 남겨둔다.
	// GET-검사-SET을 Lua 스크립트 하나로 묶어 원자적으로 실행한다(TRANSITION_IF_ISSUED_SCRIPT 참고).
	private Optional<PaymentTokenVO> transitionIfIssued(String paymentTokenId, String targetStatus) {
		String newRaw = redisTemplate.execute(
			TRANSITION_IF_ISSUED_SCRIPT,
			Collections.singletonList(RedisKeys.paymentToken(paymentTokenId)),
			targetStatus,
			String.valueOf(FINAL_STATE_TTL.getSeconds())
		);

		if (newRaw == null) {
			return Optional.empty();
		}
		return Optional.of(deserialize(newRaw));
	}

	private void save(PaymentTokenVO token, Duration ttl) {
		String json = serialize(token);
		redisTemplate.opsForValue().set(RedisKeys.paymentToken(token.getPaymentTokenId()), json, ttl);
	}

	private String serialize(PaymentTokenVO token) {
		try {
			return objectMapper.writeValueAsString(token);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("결제 토큰 직렬화에 실패했습니다.", e);
		}
	}

	private PaymentTokenVO deserialize(String raw) {
		try {
			return objectMapper.readValue(raw, PaymentTokenVO.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("결제 토큰 역직렬화에 실패했습니다.", e);
		}
	}
}
