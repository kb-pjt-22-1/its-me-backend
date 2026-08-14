package site.benepay.integration.kbcard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
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

/**
 * KB카드 신규 발급 Webhook을 처리한다.
 *
 * <p>
 * 중복 이벤트를 방지하고 사용자를 식별한 뒤,
 * Mock Server에서 발급 카드 상세 정보를 조회해 user_cards에 등록한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class KbCardWebhookService {

	private final CardIssuanceEventMapper eventMapper;
	private final CardIssuanceEventService cardIssuanceEventService;
	private final UserMapper userMapper;
	private final KbCardClient kbCardClient;
	private final CardRegistrationService cardRegistrationService;

	public void processCardIssued(CardIssuedWebhookRequestDto request) {

		if (eventMapper.existsByEventId(request.getEventId())) {
			return;
		}

		CardIssuanceEventVO event =
			CardIssuanceEventVO.builder()
				.eventId(request.getEventId())
				.ciHash(request.getCiHash())
				.cardReferenceId(request.getCardReferenceId())
				.issuerProductCode(request.getIssuerProductCode())
				.cardLast4(request.getCardLast4())
				.cardType(request.getCardType())
				.cardStatus(request.getCardStatus())
				.processingStatus("RECEIVED")
				.build();

		// 수신 기록은 먼저 독립적으로 확정
		cardIssuanceEventService.recordReceived(event);

		try {
			User user = userMapper
				.findByCiHash(request.getCiHash())
				.orElseThrow(() ->
					new KbCardWebhookUserNotFoundException(
						"Webhook 대상 BenePay 사용자를 찾을 수 없습니다."
					)
				);

			KbCardResponseDto card =
				kbCardClient.findCardByReferenceId(
					request.getCardReferenceId()
				);

			cardRegistrationService.registerCards(
				user.getUserId(),
				List.of(card)
			);

			cardIssuanceEventService.markProcessed(
				request.getEventId()
			);

		} catch (RuntimeException e) {
			cardIssuanceEventService.markFailed(
				request.getEventId(),
				e.getMessage()
			);
			throw e;
		}
	}
}
