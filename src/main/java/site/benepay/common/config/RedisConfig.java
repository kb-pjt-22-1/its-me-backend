package site.benepay.common.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

/**
 * Redis 연결 설정.
 *
 * <p>Lettuce 기본값은 오래 살아 있는 배치 클라이언트를 상정하고 잡혀 있어서, 느려진 Redis를
 * 오래 기다려 주는 쪽이 이득인 상황을 가정한다. 이 앱은 정반대다. Redis 호출이 전부 로그인·
 * 토큰 갱신·PIN 검증을 처리 중인 요청 스레드 위에서 일어나므로 기다리는 시간이 곧 스레드
 * 점유 시간이고, Redis 한 대의 장애가 톰캣 스레드 풀 고갈로 번진다. 그 전파를 끊는 것이 이
 * 설정의 목적이다.
 */
@Configuration
public class RedisConfig {

	// Redis를 다른 서비스와 공유하면 CLIENT LIST에 정체불명의 커넥션이 섞인다. 커넥션 누수나
	// 이상 트래픽을 추적할 때 어느 앱 것인지 가려낼 수 있어야 한다.
	private static final String CLIENT_NAME = "benepay-backend";

	private final String host;
	private final int port;
	private final String password;
	private final Duration commandTimeout;
	private final Duration connectTimeout;
	private final Duration shutdownTimeout;

	public RedisConfig(@Value("${redis.host}") String host,
		@Value("${redis.port}") int port,
		@Value("${redis.password}") String password,
		@Value("${redis.command-timeout-ms}") long commandTimeoutMillis,
		@Value("${redis.connect-timeout-ms}") long connectTimeoutMillis,
		@Value("${redis.shutdown-timeout-ms}") long shutdownTimeoutMillis) {
		this.host = host;
		this.port = port;
		this.password = password;
		this.commandTimeout = Duration.ofMillis(commandTimeoutMillis);
		this.connectTimeout = Duration.ofMillis(connectTimeoutMillis);
		this.shutdownTimeout = Duration.ofMillis(shutdownTimeoutMillis);
	}

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
		if (StringUtils.hasText(password)) {
			configuration.setPassword(password);
		}
		return new LettuceConnectionFactory(configuration, lettuceClientConfiguration());
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}

	private LettuceClientConfiguration lettuceClientConfiguration() {
		SocketOptions socketOptions = SocketOptions.builder()
			.connectTimeout(connectTimeout)
			// 유휴 커넥션은 NAT·로드밸런서가 통보 없이 끊는다. 그대로 두면 다음 로그인 요청이
			// 이미 죽은 커넥션을 붙잡고 타임아웃을 다 채운 뒤에야 실패한다.
			.keepAlive(true)
			.build();

		ClientOptions clientOptions = ClientOptions.builder()
			.socketOptions(socketOptions)
			// 잠금 카운터와 refresh 토큰 상태가 Redis에만 있어 끊긴 동안은 인증 기능 전체가
			// 멎는다. Redis를 재시작할 때마다 앱까지 따라 재시작하는 운영은 감당할 수 없다.
			.autoReconnect(true)
			// 기본값은 단절 중에도 명령을 큐에 쌓아 둔다. Redis가 죽으면 모든 요청이
			// commandTimeout을 꽉 채우고서야 실패하므로, 장애 시간만큼 스레드가 그대로 묶인다.
			// 어차피 실패할 요청이라면 대기 없이 떨구는 편이 남은 용량을 지킨다.
			.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
			.build();

		return LettuceClientConfiguration.builder()
			.clientOptions(clientOptions)
			.commandTimeout(commandTimeout)
			.shutdownTimeout(shutdownTimeout)
			.clientName(CLIENT_NAME)
			.build();
	}
}
