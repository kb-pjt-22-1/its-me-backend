package site.benepay.common.config;

import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@Configuration
@EnableAspectJAutoProxy
@PropertySource("classpath:application.properties")
@ComponentScan(
        basePackages = "site.benepay",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = RestController.class),
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class),
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
        })
@Import({DataSourceConfig.class, MyBatisConfig.class, SecurityConfig.class, RedisConfig.class})
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
