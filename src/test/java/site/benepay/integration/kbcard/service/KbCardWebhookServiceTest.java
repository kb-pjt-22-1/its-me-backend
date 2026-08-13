package site.benepay.integration.kbcard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.exception.KbCardWebhookUserNotFoundException;
import site.benepay.domain.card.mapper.CardIssuanceEventMapper;
import site.benepay.domain.card.service.CardRegistrationService;
import site.benepay.domain.card.vo.CardIssuanceEventVO;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.vo.User;
import site.benepay.integration.kbcard.client.KbCardClient;
import site.benepay.integration.kbcard.dto.CardIssuedWebhookRequestDto;
import site.benepay.integration.kbcard.dto.KbCardResponseDto;

@ExtendWith(MockitoExtension.class)
class KbCardWebhookServiceTest {

	private static final Long USER_ID = 10L;
	private static final String EVENT_ID = "event-001";
	private static final String CI_HASH = "ci-hash-001";
	private static final String CARD_REFERENCE_ID = "card-ref-001";

	@Mock
	private CardIssuanceEventMapper eventMapper;
	@Mock
	private UserMapper userMapper;
	@Mock
	private KbCardClient kbCardClient;
	@Mock
	private CardRegistrationService cardRegistrationService;

	@InjectMocks
	private KbCardWebhookService webhookService;

	@Test
	@DisplayName("정상 Webhook을 저장하고 사용자 카드 등록 후 처리 완료로 변경한다")
	void processCardIssuedProcessesWebhookNormally() throws Exception {
		CardIssuedWebhookRequestDto request = request();
		User user = User.builder().userId(USER_ID).ciHash(CI_HASH).build();
		KbCardResponseDto card = new KbCardResponseDto();
		card.setCardReferenceId(CARD_REFERENCE_ID);
		when(eventMapper.existsByEventId(EVENT_ID)).thenReturn(false);
		when(userMapper.findByCiHash(CI_HASH)).thenReturn(Optional.of(user));
		when(kbCardClient.findCardByReferenceId(CARD_REFERENCE_ID)).thenReturn(card);

		webhookService.processCardIssued(request);

		ArgumentCaptor<CardIssuanceEventVO> eventCaptor = ArgumentCaptor.forClass(CardIssuanceEventVO.class);
		verify(eventMapper).insertReceived(eventCaptor.capture());
		CardIssuanceEventVO event = eventCaptor.getValue();
		assertThat(event.getEventId()).isEqualTo(EVENT_ID);
		assertThat(event.getCiHash()).isEqualTo(CI_HASH);
		assertThat(event.getCardReferenceId()).isEqualTo(CARD_REFERENCE_ID);
		assertThat(event.getIssuerProductCode()).isEqualTo("product-001");
		assertThat(event.getCardLast4()).isEqualTo("1234");
		assertThat(event.getCardType()).isEqualTo("CREDIT");
		assertThat(event.getCardStatus()).isEqualTo("ACTIVE");
		assertThat(event.getProcessingStatus()).isEqualTo("RECEIVED");

		verify(kbCardClient).findCardByReferenceId(CARD_REFERENCE_ID);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<KbCardResponseDto>> cardsCaptor = ArgumentCaptor.forClass(List.class);
		verify(cardRegistrationService).registerCards(org.mockito.ArgumentMatchers.eq(USER_ID), cardsCaptor.capture());
		assertThat(cardsCaptor.getValue()).containsExactly(card);
		verify(eventMapper).markProcessed(EVENT_ID);
	}

	@Test
	@DisplayName("이미 처리 이력이 있는 eventId면 후속 작업 없이 종료한다")
	void processCardIssuedStopsForDuplicateEventId() throws Exception {
		when(eventMapper.existsByEventId(EVENT_ID)).thenReturn(true);

		webhookService.processCardIssued(request());

		verify(eventMapper, never()).insertReceived(any(CardIssuanceEventVO.class));
		verify(userMapper, never()).findByCiHash(any());
		verify(kbCardClient, never()).findCardByReferenceId(any());
		verify(cardRegistrationService, never()).registerCards(any(), any());
		verify(eventMapper, never()).markProcessed(any());
	}

	@Test
	@DisplayName("CI 해시에 해당하는 사용자가 없으면 커스텀 예외를 발생시킨다")
	void processCardIssuedThrowsWhenUserDoesNotExist() throws Exception {
		when(eventMapper.existsByEventId(EVENT_ID)).thenReturn(false);
		when(userMapper.findByCiHash(CI_HASH)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> webhookService.processCardIssued(request()))
			.isInstanceOf(KbCardWebhookUserNotFoundException.class);

		verify(eventMapper).insertReceived(any(CardIssuanceEventVO.class));
		verify(kbCardClient, never()).findCardByReferenceId(any());
		verify(cardRegistrationService, never()).registerCards(any(), any());
		verify(eventMapper, never()).markProcessed(any());
	}

	private CardIssuedWebhookRequestDto request() throws Exception {
		String json = """
			{
			  "eventId": "event-001",
			  "ciHash": "ci-hash-001",
			  "cardReferenceId": "card-ref-001",
			  "issuerProductCode": "product-001",
			  "cardLast4": "1234",
			  "cardType": "CREDIT",
			  "cardStatus": "ACTIVE"
			}
			""";
		return new ObjectMapper().readValue(json, CardIssuedWebhookRequestDto.class);
	}
}
