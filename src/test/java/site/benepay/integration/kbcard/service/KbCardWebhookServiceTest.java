package site.benepay.integration.kbcard.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.KbCardIntegrationException;
import site.benepay.common.exception.KbCardWebhookUserNotFoundException;
import site.benepay.domain.card.mapper.CardIssuanceEventMapper;
import site.benepay.domain.card.service.CardIssuanceEventService;
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
	private CardIssuanceEventService cardIssuanceEventService;

	@Mock
	private UserMapper userMapper;

	@Mock
	private KbCardClient kbCardClient;

	@Mock
	private CardRegistrationService cardRegistrationService;

	@InjectMocks
	private KbCardWebhookService webhookService;

	@Test
	@DisplayName("정상 Webhook 수신 시 이력을 저장하고 사용자 카드를 등록한 뒤 PROCESSED 처리한다")
	void processCardIssuedProcessesWebhookNormally() throws Exception {
		CardIssuedWebhookRequestDto request = request();

		User user = User.builder()
			.userId(USER_ID)
			.ciHash(CI_HASH)
			.build();

		KbCardResponseDto card = new KbCardResponseDto();
		card.setCardReferenceId(CARD_REFERENCE_ID);

		when(eventMapper.existsByEventId(EVENT_ID))
			.thenReturn(false);

		when(userMapper.findByCiHash(CI_HASH))
			.thenReturn(Optional.of(user));

		when(kbCardClient.findCardByReferenceId(CARD_REFERENCE_ID))
			.thenReturn(card);

		webhookService.processCardIssued(request);

		ArgumentCaptor<CardIssuanceEventVO> eventCaptor =
			ArgumentCaptor.forClass(CardIssuanceEventVO.class);

		verify(cardIssuanceEventService)
			.recordReceived(eventCaptor.capture());

		CardIssuanceEventVO event = eventCaptor.getValue();

		assertThat(event.getEventId())
			.isEqualTo(EVENT_ID);

		assertThat(event.getCiHash())
			.isEqualTo(CI_HASH);

		assertThat(event.getCardReferenceId())
			.isEqualTo(CARD_REFERENCE_ID);

		assertThat(event.getIssuerProductCode())
			.isEqualTo("product-001");

		assertThat(event.getCardLast4())
			.isEqualTo("1234");

		assertThat(event.getCardType())
			.isEqualTo("CREDIT");

		assertThat(event.getCardStatus())
			.isEqualTo("ACTIVE");

		assertThat(event.getProcessingStatus())
			.isEqualTo("RECEIVED");

		verify(kbCardClient)
			.findCardByReferenceId(CARD_REFERENCE_ID);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<KbCardResponseDto>> cardsCaptor =
			ArgumentCaptor.forClass(List.class);

		verify(cardRegistrationService)
			.registerCards(
				eq(USER_ID),
				cardsCaptor.capture()
			);

		assertThat(cardsCaptor.getValue())
			.containsExactly(card);

		verify(cardIssuanceEventService)
			.markProcessed(EVENT_ID);

		verify(cardIssuanceEventService, never())
			.markFailed(any(), any());
	}

	@Test
	@DisplayName("이미 처리 이력이 있는 eventId면 후속 작업 없이 종료한다")
	void processCardIssuedStopsForDuplicateEventId() throws Exception {
		CardIssuedWebhookRequestDto request = request();

		when(eventMapper.existsByEventId(EVENT_ID))
			.thenReturn(true);

		webhookService.processCardIssued(request);

		verify(cardIssuanceEventService, never())
			.recordReceived(any(CardIssuanceEventVO.class));

		verify(userMapper, never())
			.findByCiHash(any());

		verify(kbCardClient, never())
			.findCardByReferenceId(any());

		verify(cardRegistrationService, never())
			.registerCards(any(), any());

		verify(cardIssuanceEventService, never())
			.markProcessed(any());

		verify(cardIssuanceEventService, never())
			.markFailed(any(), any());
	}

	@Test
	@DisplayName("CI 해시에 해당하는 사용자가 없으면 FAILED 처리 후 커스텀 예외를 발생시킨다")
	void processCardIssuedMarksFailedWhenUserDoesNotExist() throws Exception {
		CardIssuedWebhookRequestDto request = request();

		when(eventMapper.existsByEventId(EVENT_ID))
			.thenReturn(false);

		when(userMapper.findByCiHash(CI_HASH))
			.thenReturn(Optional.empty());

		assertThatThrownBy(
			() -> webhookService.processCardIssued(request)
		)
			.isInstanceOf(KbCardWebhookUserNotFoundException.class)
			.hasMessage(
				"Webhook 대상 BenePay 사용자를 찾을 수 없습니다."
			);

		verify(cardIssuanceEventService)
			.recordReceived(any(CardIssuanceEventVO.class));

		verify(cardIssuanceEventService)
			.markFailed(
				EVENT_ID,
				"Webhook 대상 BenePay 사용자를 찾을 수 없습니다."
			);

		verify(kbCardClient, never())
			.findCardByReferenceId(any());

		verify(cardRegistrationService, never())
			.registerCards(any(), any());

		verify(cardIssuanceEventService, never())
			.markProcessed(any());
	}

	@Test
	@DisplayName("KB카드 상세 조회 중 오류가 발생하면 FAILED 처리하고 예외를 다시 전파한다")
	void processCardIssuedMarksFailedWhenKbCardLookupFails() throws Exception {
		CardIssuedWebhookRequestDto request = request();

		User user = User.builder()
			.userId(USER_ID)
			.ciHash(CI_HASH)
			.build();

		when(eventMapper.existsByEventId(EVENT_ID))
			.thenReturn(false);

		when(userMapper.findByCiHash(CI_HASH))
			.thenReturn(Optional.of(user));

		when(kbCardClient.findCardByReferenceId(CARD_REFERENCE_ID))
			.thenThrow(
				new KbCardIntegrationException(
					"KB카드 상세 정보 조회에 실패했습니다."
				)
			);

		assertThatThrownBy(
			() -> webhookService.processCardIssued(request)
		)
			.isInstanceOf(KbCardIntegrationException.class)
			.hasMessage(
				"KB카드 상세 정보 조회에 실패했습니다."
			);

		verify(cardIssuanceEventService)
			.recordReceived(any(CardIssuanceEventVO.class));

		verify(cardIssuanceEventService)
			.markFailed(
				EVENT_ID,
				"KB카드 상세 정보 조회에 실패했습니다."
			);

		verify(cardRegistrationService, never())
			.registerCards(any(), any());

		verify(cardIssuanceEventService, never())
			.markProcessed(any());
	}

	@Test
	@DisplayName("카드 등록 중 오류가 발생하면 FAILED 처리하고 예외를 다시 전파한다")
	void processCardIssuedMarksFailedWhenCardRegistrationFails() throws Exception {
		CardIssuedWebhookRequestDto request = request();

		User user = User.builder()
			.userId(USER_ID)
			.ciHash(CI_HASH)
			.build();

		KbCardResponseDto card = new KbCardResponseDto();
		card.setCardReferenceId(CARD_REFERENCE_ID);

		when(eventMapper.existsByEventId(EVENT_ID))
			.thenReturn(false);

		when(userMapper.findByCiHash(CI_HASH))
			.thenReturn(Optional.of(user));

		when(kbCardClient.findCardByReferenceId(CARD_REFERENCE_ID))
			.thenReturn(card);

		when(
			cardRegistrationService.registerCards(
				eq(USER_ID),
				any()
			)
		).thenThrow(
			new RuntimeException("카드 등록 실패")
		);

		assertThatThrownBy(
			() -> webhookService.processCardIssued(request)
		)
			.isInstanceOf(RuntimeException.class)
			.hasMessage("카드 등록 실패");

		verify(cardIssuanceEventService)
			.recordReceived(any(CardIssuanceEventVO.class));

		verify(cardIssuanceEventService)
			.markFailed(
				EVENT_ID,
				"카드 등록 실패"
			);

		verify(cardIssuanceEventService, never())
			.markProcessed(any());
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

		return new ObjectMapper()
			.readValue(
				json,
				CardIssuedWebhookRequestDto.class
			);
	}
}
