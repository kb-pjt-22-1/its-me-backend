package site.benepay.domain.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import site.benepay.common.crypto.Encryptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import site.benepay.common.config.PortOneProperties;
import site.benepay.common.exception.PortOneVerificationException;
import site.benepay.common.util.Sha256Util;
import site.benepay.domain.user.dto.PortOneVerifyResponseDto;

import java.util.HashMap;
import java.util.Map;

@Service
public class PortOneServiceImpl implements PortOneService {

    private final RestTemplate restTemplate;
    private final PortOneProperties portOneProperties;
    private final Encryptor encryptor;
    private final String diHashSalt;

    public PortOneServiceImpl(RestTemplate restTemplate, PortOneProperties portOneProperties,
                               Encryptor encryptor,
                               @Value("${security.di-hash-salt}") String diHashSalt) {
        this.restTemplate = restTemplate;
        this.portOneProperties = portOneProperties;
        this.encryptor = encryptor;
        this.diHashSalt = diHashSalt;
    }

    @Override
    public PortOneVerifyResponseDto verify(String impUid) {
        String accessToken = requestAccessToken();
        CertificationResponseBody certification = fetchCertification(impUid, accessToken);

        if (certification.uniqueKey == null || certification.uniqueInSite == null) {
            throw new PortOneVerificationException("identity verification result is missing required identifiers");
        }

        // CI는 암호화한다. 나중에 본인 확인이나 기관 연동에서 원본이 필요할 수 있어 되돌릴 수
        // 있어야 한다. 반대로 DI는 중복 가입 판별에만 쓰이고 같은 입력이 같은 값이어야 조회가
        // 되므로, 복호화가 필요 없는 결정적 해시로 남긴다.
        String ciEncrypted = encryptor.encrypt(certification.uniqueKey);
        String diHash = Sha256Util.hashWithSalt(certification.uniqueInSite, diHashSalt);
        return new PortOneVerifyResponseDto(ciEncrypted, diHash);
    }

    private String requestAccessToken() {
        Map<String, String> body = new HashMap<>();
        body.put("imp_key", portOneProperties.getImpKey());
        body.put("imp_secret", portOneProperties.getImpSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    portOneProperties.getBaseUrl() + "/users/getToken",
                    new HttpEntity<>(body, headers),
                    TokenResponse.class);

            TokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || tokenResponse.response == null || tokenResponse.response.accessToken == null) {
                throw new PortOneVerificationException("failed to obtain PortOne access token");
            }
            return tokenResponse.response.accessToken;
        } catch (RestClientException e) {
            throw new PortOneVerificationException("failed to reach PortOne API for token issuance");
        }
    }

    private CertificationResponseBody fetchCertification(String impUid, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        try {
            ResponseEntity<CertificationResponse> response = restTemplate.exchange(
                    portOneProperties.getBaseUrl() + "/certifications/" + impUid,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    CertificationResponse.class);

            CertificationResponse certificationResponse = response.getBody();
            if (certificationResponse == null || certificationResponse.code != 0 || certificationResponse.response == null) {
                throw new PortOneVerificationException("identity verification failed or was not found");
            }
            return certificationResponse.response;
        } catch (RestClientException e) {
            throw new PortOneVerificationException("failed to reach PortOne API for certification lookup");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        public int code;
        public String message;
        public TokenResponseBody response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponseBody {
        @JsonProperty("access_token")
        public String accessToken;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CertificationResponse {
        public int code;
        public String message;
        public CertificationResponseBody response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CertificationResponseBody {
        // unique_key: CI-equivalent, stable across all merchants
        @JsonProperty("unique_key")
        public String uniqueKey;
        // unique_in_site: DI-equivalent, scoped to this merchant
        @JsonProperty("unique_in_site")
        public String uniqueInSite;
    }
}
