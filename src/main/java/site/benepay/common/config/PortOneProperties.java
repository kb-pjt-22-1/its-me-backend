package site.benepay.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PortOneProperties {

    private final String baseUrl;
    private final String impKey;
    private final String impSecret;

    public PortOneProperties(@Value("${portone.base-url}") String baseUrl,
                              @Value("${portone.imp-key}") String impKey,
                              @Value("${portone.imp-secret}") String impSecret) {
        this.baseUrl = baseUrl;
        this.impKey = impKey;
        this.impSecret = impSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getImpKey() {
        return impKey;
    }

    public String getImpSecret() {
        return impSecret;
    }
}
