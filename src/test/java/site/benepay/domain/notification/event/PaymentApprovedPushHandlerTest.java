package site.benepay.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.NotificationHistoryStore;
import site.benepay.domain.notification.service.PushNotificationSender;
import site.benepay.domain.notification.vo.NotificationType;
import site.benepay.domain.payment.event.PaymentApprovedEvent;

@ExtendWith(MockitoExtension.class)
class PaymentApprovedPushHandlerTest {

	private static final Long USER_ID = 1L;
	private static final Long PAYMENT_ID = 100L;

	@Mock
	private PushNotificationSender pushNotificationSender;

	@Mock
	private NotificationHistoryStore notificationHistoryStore;

	private PaymentApprovedPushHandler handler;

	private PaymentApprovedPushHandler newHandler() {
		return new PaymentApprovedPushHandler(pushNotificationSender, notificationHistoryStore);
	}

	@Test
	void sendsAPushWithMerchantAndAmountWhenNoBenefitWasApplied() {
		handler = newHandler();
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			PAYMENT_ID, 2L, "5813", LocalDateTime.of(2026, 8, 21, 12, 0),
			BigDecimal.valueOf(15000), BigDecimal.ZERO, null, USER_ID, "스타벅스 강남점"
		);

		handler.handle(event);

		ArgumentCaptor<PushNotificationMessage> captor = ArgumentCaptor.forClass(PushNotificationMessage.class);
		verify(pushNotificationSender).send(captor.capture());
		PushNotificationMessage message = captor.getValue();
		assertThat(message.userId()).isEqualTo(USER_ID);
		assertThat(message.body()).isEqualTo("스타벅스 강남점에서 15,000원 결제했어요.");
		assertThat(message.data()).containsEntry("paymentId", "100");

		verify(notificationHistoryStore).record(
			eq(USER_ID), eq(NotificationType.PAYMENT_APPROVED), anyString(), eq(message.body()), eq(PAYMENT_ID));
	}

	@Test
	void sendsAPushMentioningTheDiscountWhenABenefitWasApplied() {
		handler = newHandler();
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			PAYMENT_ID, 2L, "5813", LocalDateTime.of(2026, 8, 21, 12, 0),
			BigDecimal.valueOf(9000), BigDecimal.valueOf(1000), "카페 할인", USER_ID, "스타벅스 강남점"
		);

		handler.handle(event);

		ArgumentCaptor<PushNotificationMessage> captor = ArgumentCaptor.forClass(PushNotificationMessage.class);
		verify(pushNotificationSender).send(captor.capture());
		assertThat(captor.getValue().body())
			.isEqualTo("스타벅스 강남점에서 9,000원 결제했어요. 카페 할인 혜택으로 1,000원 할인받았어요.");
	}

	@Test
	void doesNotPropagateWhenSendingFails() {
		handler = newHandler();
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			PAYMENT_ID, 2L, "5813", LocalDateTime.of(2026, 8, 21, 12, 0),
			BigDecimal.valueOf(15000), BigDecimal.ZERO, null, USER_ID, "스타벅스 강남점"
		);
		doThrow(new IllegalStateException("FCM이 설정되지 않아 푸시 알림을 보낼 수 없습니다."))
			.when(pushNotificationSender).send(any());

		assertThatCode(() -> handler.handle(event)).doesNotThrowAnyException();

		verify(notificationHistoryStore).record(eq(USER_ID), eq(NotificationType.PAYMENT_APPROVED), anyString(),
			anyString(), eq(PAYMENT_ID));
	}

	@Test
	void doesNotPropagateWhenHistoryStorageFails() {
		handler = newHandler();
		PaymentApprovedEvent event = new PaymentApprovedEvent(
			PAYMENT_ID, 2L, "5813", LocalDateTime.of(2026, 8, 21, 12, 0),
			BigDecimal.valueOf(15000), BigDecimal.ZERO, null, USER_ID, "스타벅스 강남점"
		);
		doThrow(new IllegalStateException("Redis 다운")).when(notificationHistoryStore)
			.record(any(), any(), any(), any(), any());

		assertThatCode(() -> handler.handle(event)).doesNotThrowAnyException();

		verify(pushNotificationSender).send(any());
	}
}
