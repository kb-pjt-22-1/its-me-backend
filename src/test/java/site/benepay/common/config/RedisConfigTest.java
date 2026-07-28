package site.benepay.common.config;

import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig("redis-host", 6380, "", 2000L, 1000L, 100L);

    @Test
    void connectionFactoryPointsAtTheConfiguredEndpoint() {
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        assertThat(factory.getHostName()).isEqualTo("redis-host");
        assertThat(factory.getPort()).isEqualTo(6380);
    }

    @Test
    void commandAndShutdownTimeoutsComeFromProperties() {
        LettuceClientConfiguration clientConfiguration =
                redisConfig.redisConnectionFactory().getClientConfiguration();

        assertThat(clientConfiguration.getCommandTimeout()).isEqualTo(Duration.ofMillis(2000));
        assertThat(clientConfiguration.getShutdownTimeout()).isEqualTo(Duration.ofMillis(100));
    }

    @Test
    void clientRejectsCommandsWhileDisconnectedInsteadOfQueueingThem() {
        ClientOptions clientOptions = redisConfig.redisConnectionFactory()
                .getClientConfiguration()
                .getClientOptions()
                .orElseThrow();

        assertThat(clientOptions.getDisconnectedBehavior())
                .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
        assertThat(clientOptions.isAutoReconnect()).isTrue();
        assertThat(clientOptions.getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofMillis(1000));
    }
}
