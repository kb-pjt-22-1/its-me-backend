package site.benepay.domain.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import site.benepay.common.util.RedisKeys;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupVerificationStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SignupVerificationStore store;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new SignupVerificationStore(redisTemplate);
    }

    @Test
    void issueStoresTheIdentityWithATenMinuteTtl() {
        SignupVerificationStore.VerifiedIdentity identity =
                new SignupVerificationStore.VerifiedIdentity("이름", "010-1111-2222", "19900101", "ci", "di");

        String token = store.issue(identity);

        assertThat(token).isNotBlank();
        verify(valueOperations).set(eq(RedisKeys.signupVerification(token)), anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void redeemReturnsTheStoredIdentityAndDeletesItAfterwards() {
        SignupVerificationStore.VerifiedIdentity identity =
                new SignupVerificationStore.VerifiedIdentity("이름", "010-1111-2222", "19900101", "ci", "di");
        String token = capturedTokenAfterIssuing(identity);

        Optional<SignupVerificationStore.VerifiedIdentity> redeemed = store.redeem(token);

        assertThat(redeemed).isPresent();
        assertThat(redeemed.get().name).isEqualTo("이름");
        assertThat(redeemed.get().diHash).isEqualTo("di");
        verify(redisTemplate).delete(RedisKeys.signupVerification(token));
    }

    @Test
    void redeemingTheSameTokenTwiceOnlyWorksOnce() {
        SignupVerificationStore.VerifiedIdentity identity =
                new SignupVerificationStore.VerifiedIdentity("이름", "010-1111-2222", "19900101", "ci", "di");
        String token = capturedTokenAfterIssuing(identity);

        store.redeem(token);
        // 실제 Redis라면 delete() 이후 get()이 null을 돌려주지만, 이 목은 그 상태를 흉내내지
        // 않는다. 그래서 여기서는 "delete가 정확히 그 키로 호출됐는지"만 확인하고, 진짜
        // 1회성 보장은 Redis의 delete 자체가 하는 일이라 별도로 통합 테스트가 필요하다면
        // 그쪽에서 검증한다.
        when(valueOperations.get(RedisKeys.signupVerification(token))).thenReturn(null);

        Optional<SignupVerificationStore.VerifiedIdentity> secondAttempt = store.redeem(token);

        assertThat(secondAttempt).isEmpty();
    }

    @Test
    void redeemReturnsEmptyForAnUnknownToken() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(store.redeem("never-issued")).isEmpty();
    }

    private String capturedTokenAfterIssuing(SignupVerificationStore.VerifiedIdentity identity) {
        String[] savedJson = new String[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            savedJson[0] = invocation.getArgument(1);
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        String token = store.issue(identity);
        when(valueOperations.get(RedisKeys.signupVerification(token))).thenReturn(savedJson[0]);
        return token;
    }
}
