package site.benepay.domain.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.integration.kbcard.client.KbCardClient;
import site.benepay.integration.kbcard.dto.KbCardResponseDto;
import site.benepay.integration.kbcard.dto.KbCustomerCardsResponseDto;

@ExtendWith(MockitoExtension.class)
class CardSyncServiceTest {

	private static final Long USER_ID = 10L;
	private static final String CI_HASH = "ci-hash-001";

	@Mock
	private KbCardClient kbCardClient;

	@Mock
	private CardRegistrationService cardRegistrationService;

	@InjectMocks
	private CardSyncService cardSyncService;

	@Test
	@DisplayName("CI 해시로 조회한 여러 카드 전체를 등록 서비스에 전달하고 등록 건수를 반환한다")
	void syncCardsPassesAllCardsAndReturnsRegisteredCount() {
		List<KbCardResponseDto> cards = List.of(card("card-1"), card("card-2"));
		KbCustomerCardsResponseDto response = response(cards);
		when(kbCardClient.findCardsByCiHash(CI_HASH)).thenReturn(response);
		when(cardRegistrationService.registerCards(USER_ID, cards)).thenReturn(2);

		int result = cardSyncService.syncCards(USER_ID, CI_HASH);

		assertThat(result).isEqualTo(2);
		verify(kbCardClient).findCardsByCiHash(CI_HASH);
		verify(cardRegistrationService).registerCards(USER_ID, cards);
	}

	@Test
	@DisplayName("카드 목록이 비어 있으면 등록 결과 0을 반환한다")
	void syncCardsReturnsZeroForEmptyCards() {
		List<KbCardResponseDto> cards = List.of();
		when(kbCardClient.findCardsByCiHash(CI_HASH)).thenReturn(response(cards));
		when(cardRegistrationService.registerCards(USER_ID, cards)).thenReturn(0);

		assertThat(cardSyncService.syncCards(USER_ID, CI_HASH)).isZero();
		verify(cardRegistrationService).registerCards(USER_ID, cards);
	}

	@Test
	@DisplayName("응답의 카드 목록이 null이어도 등록 서비스의 null 처리 결과를 그대로 반환한다")
	void syncCardsPassesNullCardsToRegistrationService() {
		KbCustomerCardsResponseDto response = response(null);
		when(kbCardClient.findCardsByCiHash(CI_HASH)).thenReturn(response);
		when(cardRegistrationService.registerCards(USER_ID, null)).thenReturn(0);

		assertThat(cardSyncService.syncCards(USER_ID, CI_HASH)).isZero();
		verify(cardRegistrationService).registerCards(USER_ID, null);
	}

	private KbCustomerCardsResponseDto response(List<KbCardResponseDto> cards) {
		KbCustomerCardsResponseDto response = new KbCustomerCardsResponseDto();
		response.setCards(cards);
		return response;
	}

	private KbCardResponseDto card(String referenceId) {
		KbCardResponseDto card = new KbCardResponseDto();
		card.setCardReferenceId(referenceId);
		return card;
	}
}
