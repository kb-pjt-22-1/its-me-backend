package site.benepay.integration.kbcard.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import site.benepay.common.exception.KbCardIntegrationException;
import site.benepay.integration.kbcard.dto.KbCardResponseDto;
import site.benepay.integration.kbcard.dto.KbCustomerCardsResponseDto;
import site.benepay.integration.kbcard.dto.KbCustomerVerifyRequestDto;
import site.benepay.integration.kbcard.dto.KbCustomerVerifyResponseDto;

@ExtendWith(MockitoExtension.class)
class KbCardClientTest {

	private static final String BASE_URL = "http://kb-card-mock";
	private static final String CI_HASH = "ci-hash-001";
	private static final String CARD_REFERENCE_ID = "card-ref-001";

	@Mock
	private RestTemplate restTemplate;

	private KbCardClient kbCardClient;

	@BeforeEach
	void setUp() {
		kbCardClient = new KbCardClient(restTemplate);
		ReflectionTestUtils.setField(kbCardClient, "baseUrl", BASE_URL);
	}

	@Test
	@DisplayName("CI 해시를 쿼리 파라미터로 전달해 보유 카드 목록을 조회한다")
	void findCardsByCiHashUsesExpectedUrlAndReturnsResponse() {
		String url = BASE_URL + "/api/v1/customers/cards?ciHash={ciHash}";
		KbCustomerCardsResponseDto expected = new KbCustomerCardsResponseDto();
		when(restTemplate.getForObject(url, KbCustomerCardsResponseDto.class, CI_HASH))
			.thenReturn(expected);

		assertThat(kbCardClient.findCardsByCiHash(CI_HASH)).isSameAs(expected);
		verify(restTemplate).getForObject(url, KbCustomerCardsResponseDto.class, CI_HASH);
	}

	@Test
	@DisplayName("보유 카드 조회 HTTP 응답이 null이면 빈 응답 DTO를 반환한다")
	void findCardsByCiHashReturnsEmptyDtoForNullResponse() {
		String url = BASE_URL + "/api/v1/customers/cards?ciHash={ciHash}";
		when(restTemplate.getForObject(url, KbCustomerCardsResponseDto.class, CI_HASH)).thenReturn(null);

		KbCustomerCardsResponseDto result = kbCardClient.findCardsByCiHash(CI_HASH);

		assertThat(result).isNotNull();
		assertThat(result.getCards()).isEmpty();
	}

	@Test
	@DisplayName("카드 참조 ID를 path variable로 전달해 카드 상세를 조회한다")
	void findCardByReferenceIdUsesExpectedUrlAndReturnsResponse() {
		String url = BASE_URL + "/api/v1/cards/{cardReferenceId}";
		KbCardResponseDto expected = new KbCardResponseDto();
		when(restTemplate.getForObject(url, KbCardResponseDto.class, CARD_REFERENCE_ID))
			.thenReturn(expected);

		assertThat(kbCardClient.findCardByReferenceId(CARD_REFERENCE_ID)).isSameAs(expected);
		verify(restTemplate).getForObject(url, KbCardResponseDto.class, CARD_REFERENCE_ID);
	}

	@Test
	@DisplayName("보유 카드 조회 RestClientException을 KB 연동 예외로 변환한다")
	void findCardsByCiHashWrapsRestClientException() {
		String url = BASE_URL + "/api/v1/customers/cards?ciHash={ciHash}";
		RestClientException cause = new RestClientException("connection failed");
		when(restTemplate.getForObject(url, KbCustomerCardsResponseDto.class, CI_HASH))
			.thenThrow(cause);

		assertThatThrownBy(() -> kbCardClient.findCardsByCiHash(CI_HASH))
			.isInstanceOf(KbCardIntegrationException.class)
			.hasCause(cause);
	}

	@Test
	@DisplayName("카드 상세 조회 RestClientException을 KB 연동 예외로 변환한다")
	void findCardByReferenceIdWrapsRestClientException() {
		String url = BASE_URL + "/api/v1/cards/{cardReferenceId}";
		RestClientException cause = new RestClientException("connection failed");
		when(restTemplate.getForObject(url, KbCardResponseDto.class, CARD_REFERENCE_ID))
			.thenThrow(cause);

		assertThatThrownBy(() -> kbCardClient.findCardByReferenceId(CARD_REFERENCE_ID))
			.isInstanceOf(KbCardIntegrationException.class)
			.hasCause(cause);
	}

	@Test
	@DisplayName("이름/생년월일/휴대폰번호를 POST 바디로 보내 KB 실명 회원 여부를 확인한다")
	void verifyCustomerPostsIdentityAndReturnsResponse() {
		String url = BASE_URL + "/api/v1/customers/verify";
		KbCustomerVerifyResponseDto expected = new KbCustomerVerifyResponseDto();
		expected.setRegistered(true);
		expected.setCustomerReferenceId("kb-customer-001");
		when(restTemplate.postForObject(eq(url), any(KbCustomerVerifyRequestDto.class),
			eq(KbCustomerVerifyResponseDto.class))).thenReturn(expected);

		KbCustomerVerifyResponseDto result = kbCardClient.verifyCustomer("홍길동", "19900101", "010-1111-2222");

		assertThat(result).isSameAs(expected);
		ArgumentCaptor<KbCustomerVerifyRequestDto> captor = ArgumentCaptor.forClass(KbCustomerVerifyRequestDto.class);
		verify(restTemplate).postForObject(eq(url), captor.capture(), eq(KbCustomerVerifyResponseDto.class));
		assertThat(captor.getValue().getName()).isEqualTo("홍길동");
		assertThat(captor.getValue().getBirthDate()).isEqualTo("19900101");
		assertThat(captor.getValue().getPhoneNumber()).isEqualTo("010-1111-2222");
	}

	@Test
	@DisplayName("회원 확인 HTTP 응답이 null이면 registered=false인 빈 응답 DTO를 반환한다")
	void verifyCustomerReturnsUnregisteredDtoForNullResponse() {
		String url = BASE_URL + "/api/v1/customers/verify";
		when(restTemplate.postForObject(eq(url), any(KbCustomerVerifyRequestDto.class),
			eq(KbCustomerVerifyResponseDto.class))).thenReturn(null);

		KbCustomerVerifyResponseDto result = kbCardClient.verifyCustomer("홍길동", "19900101", "010-1111-2222");

		assertThat(result).isNotNull();
		assertThat(result.isRegistered()).isFalse();
	}

	@Test
	@DisplayName("회원 확인 RestClientException을 KB 연동 예외로 변환한다")
	void verifyCustomerWrapsRestClientException() {
		String url = BASE_URL + "/api/v1/customers/verify";
		RestClientException cause = new RestClientException("connection failed");
		when(restTemplate.postForObject(eq(url), any(KbCustomerVerifyRequestDto.class),
			eq(KbCustomerVerifyResponseDto.class))).thenThrow(cause);

		assertThatThrownBy(() -> kbCardClient.verifyCustomer("홍길동", "19900101", "010-1111-2222"))
			.isInstanceOf(KbCardIntegrationException.class)
			.hasCause(cause);
	}
}
