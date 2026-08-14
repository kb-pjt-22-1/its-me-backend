package site.benepay.domain.card.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.mapper.CardIssuanceEventMapper;
import site.benepay.domain.card.vo.CardIssuanceEventVO;

/**
 * 카드 발급 Webhook의 수신 및 처리 이력을 별도 트랜잭션으로 관리한다.
 */
@Service
@RequiredArgsConstructor
public class CardIssuanceEventService {

	private final CardIssuanceEventMapper eventMapper;

	/**
	 * Webhook 수신 이력을 별도 트랜잭션으로 저장한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordReceived(CardIssuanceEventVO event) {
		eventMapper.insertReceived(event);
	}

	/**
	 * Webhook 처리가 정상 완료되었음을 기록한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markProcessed(String eventId) {
		eventMapper.markProcessed(eventId);
	}

	/**
	 * Webhook 처리 실패 사유를 기록한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(String eventId, String failReason) {
		eventMapper.markFailed(eventId, failReason);
	}
}
