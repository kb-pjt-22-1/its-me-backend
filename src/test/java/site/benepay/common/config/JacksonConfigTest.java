package site.benepay.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final JacksonConfig jacksonConfig = new JacksonConfig();

    @Test
    void objectMapperBeanReturnsAUsableObjectMapper() throws Exception {
        ObjectMapper objectMapper = jacksonConfig.objectMapper();

        assertThat(objectMapper).isNotNull();
        assertThat(objectMapper.writeValueAsString("hello")).isEqualTo("\"hello\"");
    }
}
