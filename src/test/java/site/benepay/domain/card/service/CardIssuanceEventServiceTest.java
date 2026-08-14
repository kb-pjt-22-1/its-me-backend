package site.benepay.domain.card.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.card.mapper.CardIssuanceEventMapper;
import site.benepay.domain.card.vo.CardIssuanceEventVO;

@ExtendWith(MockitoExtension.class)
class CardIssuanceEventServiceTest {

	private static final String EVENT_ID = "event-001";

	@Mock
	private CardIssuanceEventMapper eventMapper;

	@InjectMocks
	private CardIssuanceEventService service;

	@Test
	@DisplayName("Webhook 수신 이력을 저장한다")
	void recordReceivedInsertsEvent() {
		CardIssuanceEventVO event =
			CardIssuanceEventVO.builder()
				.eventId(EVENT_ID)
				.processingStatus("RECEIVED")
				.build();

		service.recordReceived(event);

		verify(eventMapper)
			.insertReceived(event);
	}

	@Test
	@DisplayName("Webhook 처리 성공 시 PROCESSED 상태로 변경한다")
	void markProcessedUpdatesEvent() {
		service.markProcessed(EVENT_ID);

		verify(eventMapper)
			.markProcessed(EVENT_ID);
	}

	@Test
	@DisplayName("Webhook 처리 실패 시 FAILED 상태와 실패 사유를 저장한다")
	void markFailedUpdatesEventWithReason() {
		String failReason =
			"KB카드 상세 정보 조회에 실패했습니다.";

		service.markFailed(
			EVENT_ID,
			failReason
		);

		verify(eventMapper)
			.markFailed(
				EVENT_ID,
				failReason
			);
	}
}
