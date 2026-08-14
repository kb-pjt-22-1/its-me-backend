package site.benepay.domain.card.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.integration.kbcard.client.KbCardClient;
import site.benepay.integration.kbcard.dto.KbCustomerCardsResponseDto;

/**
 * 최초 회원가입 또는 수동 재동기화 시
 * Mock Server에서 사용자의 전체 보유 카드를 가져오는 서비스다.
 *
 * 외부 조회는 KbCardClient에 맡기고,
 * 실제 DB 등록은 CardRegistrationService에 맡긴다.
 */
@Service
@RequiredArgsConstructor
public class CardSyncService {

	private final KbCardClient kbCardClient;
	private final CardRegistrationService cardRegistrationService;

	public int syncCards(Long userId, String ciHash) {
		KbCustomerCardsResponseDto response =
			kbCardClient.findCardsByCiHash(ciHash);

		/*
		 * Mock Server에 고객이 없거나 카드가 없으면
		 * 빈 목록이므로 정상적으로 0건 처리된다.
		 */
		return cardRegistrationService.registerCards(
			userId,
			response.getCards()
		);
	}
}
