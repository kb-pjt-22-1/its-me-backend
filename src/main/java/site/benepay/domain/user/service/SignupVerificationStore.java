package site.benepay.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.benepay.common.util.RedisKeys;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * PortOne 인증이 끝난 시점부터 회원가입이 끝나는 시점까지, 검증된 신원 정보(이름·전화번호·
 * 생년월일·CI·DI)를 프론트가 아니라 서버가 들고 있게 하는 다리 역할.
 *
 * <p>왜 이렇게 하나: 검증된 값을 프론트에 그대로 돌려주고 회원가입 때 다시 받으면, 그 값을
 * 어디서든 손에 넣은 사람이 본인 인증 없이 "그 사람인 척" 가입할 수 있다(재사용 공격). 값
 * 대신 토큰만 오가게 하고, 토큰은 짧게 살고 한 번 쓰면 사라지게 만들어 이 경로를 막는다.
 */
@Service
public class SignupVerificationStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignupVerificationStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(VerifiedIdentity identity) {
        String token = UUID.randomUUID().toString();
        try {
            String json = objectMapper.writeValueAsString(identity);
            redisTemplate.opsForValue().set(RedisKeys.signupVerification(token), json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize verified identity", e);
        }
        return token;
    }

    // get과 delete 사이에 아주 짧은 레이스 윈도가 있지만(동시에 같은 토큰을 두 번 제출),
    // 최종 유일성은 users.di UNIQUE 제약이 지킨다. 여기서 막으려는 건 "인증 없이 가입"이지
    // 동시성 제어가 아니라서 Lua 스크립트까지는 안 썼다.
    public Optional<VerifiedIdentity> redeem(String token) {
        String key = RedisKeys.signupVerification(token);
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        try {
            return Optional.of(objectMapper.readValue(raw, VerifiedIdentity.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to deserialize verified identity", e);
        }
    }

    public static class VerifiedIdentity {
        public String name;
        public String phoneNumber;
        public String birthDate;
        public String ciEncrypted;
        public String diHash;

        public VerifiedIdentity() {
        }

        public VerifiedIdentity(String name, String phoneNumber, String birthDate, String ciEncrypted, String diHash) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.birthDate = birthDate;
            this.ciEncrypted = ciEncrypted;
            this.diHash = diHash;
        }
    }
}
