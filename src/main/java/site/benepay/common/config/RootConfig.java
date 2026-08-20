package site.benepay.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
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
@Import({DataSourceConfig.class, MyBatisConfig.class, SecurityConfig.class, RedisConfig.class, JacksonConfig.class})
public class RootConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
