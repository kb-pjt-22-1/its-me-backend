package site.benepay.integration.kbcard.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.KbCardIntegrationException;
import site.benepay.integration.kbcard.dto.KbCardResponseDto;
import site.benepay.integration.kbcard.dto.KbCustomerCardsResponseDto;
import site.benepay.integration.kbcard.dto.KbCustomerVerifyRequestDto;
import site.benepay.integration.kbcard.dto.KbCustomerVerifyResponseDto;

/**
 * BenePay에서 KB카드 Mock Server로 HTTP 요청을 보내는 전용 Client다.
 *
 * Service가 URL이나 RestTemplate 사용법을 직접 알지 않게 하고,
 * 외부 시스템 호출 책임을 integration 패키지에 모은다.
 */
@Component
@RequiredArgsConstructor
public class KbCardClient {

	private final RestTemplate restTemplate;

	@Value("${kb-card.base-url}")
	private String baseUrl;

	/**
	 * CI 해시로 고객의 전체 보유 카드를 조회한다.
	 */
	public KbCustomerCardsResponseDto findCardsByCiHash(String ciHash) {
		String url = baseUrl + "/api/v1/customers/cards?ciHash={ciHash}";

		try {
			KbCustomerCardsResponseDto response = restTemplate.getForObject(
				url,
				KbCustomerCardsResponseDto.class,
				ciHash
			);

			return response == null
				? new KbCustomerCardsResponseDto()
				: response;
		} catch (RestClientException e) {
			throw new KbCardIntegrationException(
				"KB카드 Mock Server 보유 카드 조회에 실패했습니다.",
				e
			);
		}
	}

	/**
	 * Webhook 수신 후 cardReferenceId를 이용해
	 * 카드의 최신 상세 정보를 다시 조회할 때 사용한다.
	 */
	public KbCardResponseDto findCardByReferenceId(String cardReferenceId) {
		String url = baseUrl + "/api/v1/cards/{cardReferenceId}";

		try {
			KbCardResponseDto response = restTemplate.getForObject(
				url,
				KbCardResponseDto.class,
				cardReferenceId
			);

			if (response == null) {
				throw new KbCardIntegrationException(
					"KB카드 상세 정보 응답이 비어 있습니다."
				);
			}

			return response;

		} catch (RestClientException e) {
			throw new KbCardIntegrationException(
				"KB카드 상세 정보 조회에 실패했습니다.",
				e
			);
		}
	}

	/**
	 * 회원가입 1단계(휴대폰 본인인증) 전용 - 이름/생년월일/휴대폰번호로 KB에 실명 등록된
	 * 회원인지 확인한다. 카드 조회 API와 달리 원문 개인정보를 보내므로 POST+바디를 쓴다.
	 */
	public KbCustomerVerifyResponseDto verifyCustomer(String name, String birthDate, String phoneNumber) {
		String url = baseUrl + "/api/v1/customers/verify";
		KbCustomerVerifyRequestDto request = new KbCustomerVerifyRequestDto(name, birthDate, phoneNumber);

		try {
			KbCustomerVerifyResponseDto response = restTemplate.postForObject(
				url,
				request,
				KbCustomerVerifyResponseDto.class
			);

			return response == null
				? new KbCustomerVerifyResponseDto()
				: response;
		} catch (RestClientException e) {
			throw new KbCardIntegrationException(
				"KB카드 Mock Server 회원 확인에 실패했습니다.",
				e
			);
		}
	}
}
