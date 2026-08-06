package site.benepay.common.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    void configureMessageConvertersAddsAJacksonConverter() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();

        webConfig.configureMessageConverters(converters);

        assertThat(converters).hasSize(1);
        assertThat(converters.get(0)).isInstanceOf(MappingJackson2HttpMessageConverter.class);
    }

    @Test
    void theRegisteredConverterWritesLocalDateTimeAsAnIsoStringNotATimestampArray() throws IOException {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        webConfig.configureMessageConverters(converters);
        MappingJackson2HttpMessageConverter jacksonConverter =
                (MappingJackson2HttpMessageConverter) converters.get(0);

        String json = jacksonConverter.getObjectMapper()
                .writeValueAsString(LocalDateTime.of(2026, 8, 6, 12, 30, 0));

        assertThat(json).isEqualTo("\"2026-08-06T12:30:00\"");
    }

    @Test
    void theRegisteredConverterIgnoresUnknownPropertiesWhenReading() throws IOException {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        webConfig.configureMessageConverters(converters);
        MappingJackson2HttpMessageConverter jacksonConverter =
                (MappingJackson2HttpMessageConverter) converters.get(0);

        KnownFieldOnly result = jacksonConverter.getObjectMapper()
                .readValue("{\"knownField\":\"value\",\"unknownField\":\"whatever\"}", KnownFieldOnly.class);

        assertThat(result.knownField).isEqualTo("value");
    }

    private static class KnownFieldOnly {
        public String knownField;
    }

    @Test
    void validatorBeanReturnsALocalValidatorFactoryBean() {
        LocalValidatorFactoryBean validator = webConfig.validator();

        assertThat(validator).isNotNull();
    }
}
