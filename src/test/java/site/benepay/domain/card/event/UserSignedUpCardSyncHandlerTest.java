package site.benepay.domain.card.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.common.event.UserSignedUpEvent;
import site.benepay.domain.card.service.CardSyncService;

@ExtendWith(MockitoExtension.class)
class UserSignedUpCardSyncHandlerTest {

	private static final Long USER_ID = 10L;
	private static final String CI_HASH = "ci-hash-001";

	@Mock
	private CardSyncService cardSyncService;

	@InjectMocks
	private UserSignedUpCardSyncHandler handler;

	@Test
	@DisplayName("회원가입 이벤트의 사용자 ID와 CI 해시로 카드를 정확히 한 번 동기화한다")
	void handleSyncsCardsOnceWithEventValues() {
		when(cardSyncService.syncCards(USER_ID, CI_HASH)).thenReturn(2);

		handler.handle(new UserSignedUpEvent(USER_ID, CI_HASH));

		verify(cardSyncService, times(1)).syncCards(USER_ID, CI_HASH);
	}

	@Test
	@DisplayName("카드 동기화 중 예외가 발생해도 이벤트 핸들러 밖으로 전파하지 않는다")
	void handleSwallowsCardSyncException() {
		when(cardSyncService.syncCards(USER_ID, CI_HASH))
			.thenThrow(new RuntimeException("KB 연동 실패"));

		assertThatCode(() -> handler.handle(new UserSignedUpEvent(USER_ID, CI_HASH)))
			.doesNotThrowAnyException();

		verify(cardSyncService, times(1)).syncCards(USER_ID, CI_HASH);
	}
}
