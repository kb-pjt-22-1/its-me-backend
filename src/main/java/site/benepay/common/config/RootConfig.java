package site.benepay.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
@PropertySource("classpath:application.properties")
@ComponentScan(
	basePackages = "site.benepay",
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, classes = RestController.class),
		@ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class),
		@ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
	})
@Import({DataSourceConfig.class, MyBatisConfig.class, SecurityConfig.class, RedisConfig.class, JacksonConfig.class,
	FirebaseConfig.class})
public class RootConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	// KB Card Mock Server 호출(KbCardClient)의 유일한 클라이언트다. 타임아웃 없이 두면 mock
	// 서버가 응답을 멈췄을 때 요청 스레드가 무기한 블록된다 - Redis/OpenAI처럼 여기도 명시적으로
	// 설정한다(application.properties).
	@Bean
	public RestTemplate restTemplate(
		@Value("${kb-card.connect-timeout-ms}") int connectTimeoutMs,
		@Value("${kb-card.read-timeout-ms}") int readTimeoutMs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		return new RestTemplate(factory);
	}
}
