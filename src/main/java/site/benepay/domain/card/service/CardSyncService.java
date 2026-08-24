package site.benepay.domain.card.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.UserNotFoundException;
import site.benepay.domain.user.mapper.UserMapper;
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
	private final UserMapper userMapper;

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

	/**
	 * 사용자가 앱에서 직접 누르는 수동 재동기화용 진입점이다. 회원가입 직후 자동 연동
	 * (UserSignedUpCardSyncHandler)이 목서버 순간 장애 등으로 실패해도 재시도 수단이
	 * 없었던 문제를 보완한다 - ciHash를 이벤트로 안 받고 userId로 직접 조회한다.
	 */
	public int syncCards(Long userId) {
		String ciHash = userMapper.findByUserId(userId)
			.orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."))
			.getCiHash();

		return syncCards(userId, ciHash);
	}
}
