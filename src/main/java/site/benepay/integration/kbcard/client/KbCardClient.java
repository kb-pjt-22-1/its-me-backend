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
	 * 회원가입 1단계(휴대폰 본인인증) 전용 - ciHash로 KB에 실명 등록된 회원인지 확인한다.
	 * 호출부(SignupIdentityServiceImpl)가 이미 계산해 둔 ciHash를 그대로 받는다 - 원문
	 * 이름/생년월일/휴대폰번호를 다시 보낼 필요가 없다.
	 */
	public KbCustomerVerifyResponseDto verifyCustomer(String ciHash) {
		String url = baseUrl + "/api/v1/customers/verify";
		KbCustomerVerifyRequestDto request = new KbCustomerVerifyRequestDto(ciHash);

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

	/**
	 * (테스트용) 회원가입 1단계에서 verifyCustomer가 "등록된 회원이 아님"을 돌려줬을 때,
	 * Mock Server에 그 ciHash로 새 고객+카드를 즉석에서 만들어 달라고 요청한다. 실제
	 * 카드사 API에는 없는 흐름이다 - 실제 본인인증기관 연동이 없는 로컬/테스트 환경에서,
	 * 미리 심어둔 시드 신원(mock-test-identities.txt)이 아니어도 회원가입-카드 자동연동
	 * 흐름을 끝까지 테스트할 수 있게 하려는 편의 기능이다. 호출부(SignupIdentityServiceImpl)가
	 * dev-login.enabled일 때만 호출하므로 운영 환경에서는 실행되지 않는다.
	 */
	public KbCustomerVerifyResponseDto registerCustomer(String ciHash) {
		String url = baseUrl + "/api/v1/customers/register";
		KbCustomerVerifyRequestDto request = new KbCustomerVerifyRequestDto(ciHash);

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
				"KB카드 Mock Server 테스트 고객 등록에 실패했습니다.",
				e
			);
		}
	}
}
