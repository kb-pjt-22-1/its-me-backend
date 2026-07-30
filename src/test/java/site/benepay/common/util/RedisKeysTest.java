package site.benepay.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeysTest {

    @Test
    void keyPatternsMatchTheDocumentedFormat() {
        assertThat(RedisKeys.loginFailure(42L)).isEqualTo("login:failure:42");
        assertThat(RedisKeys.loginLock(42L)).isEqualTo("login:lock:42");
        assertThat(RedisKeys.refresh(42L)).isEqualTo("refresh:42");
        assertThat(RedisKeys.pinFailure(42L)).isEqualTo("pin:failure:42");
        assertThat(RedisKeys.pinLock(42L)).isEqualTo("pin:lock:42");
        assertThat(RedisKeys.blacklist("some-jti")).isEqualTo("blacklist:some-jti");
        assertThat(RedisKeys.alert(42L)).isEqualTo("alert:42");
    }
}
