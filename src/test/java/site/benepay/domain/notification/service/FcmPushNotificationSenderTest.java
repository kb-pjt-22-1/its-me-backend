package site.benepay.domain.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.vo.User;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationSenderTest {

	private static final Long USER_ID = 1L;

	@Mock
	private FirebaseMessaging firebaseMessaging;

	@Mock
	private UserMapper userMapper;

	private User userWithToken(String fcmToken) {
		return User.builder().userId(USER_ID).fcmToken(fcmToken).build();
	}

	private PushNotificationMessage message() {
		return new PushNotificationMessage(USER_ID, "제목", "본문", Map.of("k", "v"));
	}

	@Test
	void throwsWhenFirebaseIsNotConfigured() {
		FcmPushNotificationSender sender = new FcmPushNotificationSender(Optional.empty(), userMapper);

		assertThatThrownBy(() -> sender.send(message()))
			.isInstanceOf(IllegalStateException.class);

		verifyNoInteractions(userMapper);
	}

	@Test
	void skipsSendingWhenUserHasNoFcmToken() throws Exception {
		FcmPushNotificationSender sender = new FcmPushNotificationSender(Optional.of(firebaseMessaging), userMapper);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(userWithToken(null)));

		sender.send(message());

		verify(firebaseMessaging, never()).send(any());
	}

	@Test
	void skipsSendingWhenUserDoesNotExist() throws Exception {
		FcmPushNotificationSender sender = new FcmPushNotificationSender(Optional.of(firebaseMessaging), userMapper);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.empty());

		sender.send(message());

		verify(firebaseMessaging, never()).send(any());
	}

	@Test
	void sendsFcmMessageWithTitleBodyAndDataWhenTokenExists() throws Exception {
		FcmPushNotificationSender sender = new FcmPushNotificationSender(Optional.of(firebaseMessaging), userMapper);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(userWithToken("token-abc")));

		sender.send(message());

		verify(firebaseMessaging).send(any(Message.class));
	}

	@Test
	void wrapsFirebaseMessagingExceptionAsIllegalStateException() throws Exception {
		FcmPushNotificationSender sender = new FcmPushNotificationSender(Optional.of(firebaseMessaging), userMapper);
		when(userMapper.findByUserId(USER_ID)).thenReturn(Optional.of(userWithToken("token-abc")));
		when(firebaseMessaging.send(any(Message.class))).thenThrow(mock(FirebaseMessagingException.class));

		assertThatThrownBy(() -> sender.send(message()))
			.isInstanceOf(IllegalStateException.class)
			.hasCauseInstanceOf(FirebaseMessagingException.class);
	}
}
