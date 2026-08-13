package site.benepay.domain.card.mapper;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.card.vo.CardIssuanceEventVO;

/**
 * 카드 발급 Webhook 수신 이력의 저장과 상태 변경을 담당한다.
 */
public interface CardIssuanceEventMapper {

	boolean existsByEventId(
		@Param("eventId") String eventId
	);

	int insertReceived(
		CardIssuanceEventVO event
	);

	int markProcessed(
		@Param("eventId") String eventId
	);
}
