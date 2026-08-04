package site.benepay.domain.payment.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.util.RedisKeys;
import site.benepay.domain.payment.vo.PaymentTokenVO;

/**
 * 결제 QR/바코드 토큰의 Redis 저장소. SignupVerificationStore와 동일한 issue/조회 패턴을 따른다.
 */
@Service
public class PaymentTokenStore {

	// 화면에 띄우는 동적 바코드/QR 특성상 길게 살려두지 않는다. 정책 확정 전까지 임시값.
	// dto 패키지(PaymentTokenResponseDto)에서 expiresAt 계산에 참조하므로 public.
	public static final Duration TTL = Duration.ofMinutes(3);

	private static final String STATUS_ISSUED = "ISSUED";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public PaymentTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public PaymentTokenVO issue(Long userId, Long userCardId) {
		String paymentTokenId = UUID.randomUUID().toString();
		PaymentTokenVO token = new PaymentTokenVO(
			paymentTokenId,
			userId,
			userCardId,
			STATUS_ISSUED,
			LocalDateTime.now().toString()
		);

		save(token);

		return token;
	}

	// 상태 조회(폴링)에 쓰인다. redeem과 달리 조회 후 삭제하지 않는다.
	public Optional<PaymentTokenVO> find(String paymentTokenId) {
		String raw = redisTemplate.opsForValue().get(RedisKeys.paymentToken(paymentTokenId));
		if (raw == null) {
			return Optional.empty();
		}
		return Optional.of(deserialize(raw));
	}

	private void save(PaymentTokenVO token) {
		String json = serialize(token);
		redisTemplate.opsForValue().set(RedisKeys.paymentToken(token.getPaymentTokenId()), json, TTL);
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
