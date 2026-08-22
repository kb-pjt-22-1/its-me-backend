package site.benepay.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.NotificationHistoryStore;
import site.benepay.domain.notification.service.PushNotificationSender;
import site.benepay.domain.notification.vo.NotificationType;
import site.benepay.domain.user.event.SessionDisplacedEvent;

@ExtendWith(MockitoExtension.class)
class SessionDisplacedPushHandlerTest {

	private static final Long USER_ID = 1L;

	@Mock
	private PushNotificationSender pushNotificationSender;

	@Mock
	private NotificationHistoryStore notificationHistoryStore;

	private SessionDisplacedPushHandler newHandler() {
		return new SessionDisplacedPushHandler(pushNotificationSender, notificationHistoryStore);
	}

	@Test
	void sendsAPushAndRecordsHistoryWithNoRelatedId() {
		SessionDisplacedPushHandler handler = newHandler();

		handler.handle(new SessionDisplacedEvent(USER_ID));

		ArgumentCaptor<PushNotificationMessage> captor = ArgumentCaptor.forClass(PushNotificationMessage.class);
		verify(pushNotificationSender).send(captor.capture());
		PushNotificationMessage message = captor.getValue();
		assertThat(message.userId()).isEqualTo(USER_ID);
		assertThat(message.data()).isEmpty();

		verify(notificationHistoryStore).record(
			eq(USER_ID), eq(NotificationType.SESSION_DISPLACED), anyString(), eq(message.body()), isNull());
	}

	@Test
	void doesNotPropagateWhenSendingFails() {
		SessionDisplacedPushHandler handler = newHandler();
		doThrow(new IllegalStateException("FCM이 설정되지 않아 푸시 알림을 보낼 수 없습니다."))
			.when(pushNotificationSender).send(any());

		assertThatCode(() -> handler.handle(new SessionDisplacedEvent(USER_ID))).doesNotThrowAnyException();

		verify(notificationHistoryStore).record(
			eq(USER_ID), eq(NotificationType.SESSION_DISPLACED), anyString(), anyString(), isNull());
	}

	@Test
	void doesNotPropagateWhenHistoryStorageFails() {
		SessionDisplacedPushHandler handler = newHandler();
		doThrow(new IllegalStateException("Redis 다운")).when(notificationHistoryStore)
			.record(any(), any(), any(), any(), any());

		assertThatCode(() -> handler.handle(new SessionDisplacedEvent(USER_ID))).doesNotThrowAnyException();

		verify(pushNotificationSender).send(any());
	}
}
