package site.benepay.domain.notification.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.user.mapper.UserMapper;
import site.benepay.domain.user.vo.User;

@Component
@RequiredArgsConstructor
public class FcmPushNotificationSender implements PushNotificationSender {

	// FirebaseConfig가 자격증명 미설정 시 빈을 등록하지 않으므로 Optional로 받는다 -
	// 없으면 이 클래스가 아니라 호출 시점(send)에만 실패한다(로컬 개발 환경 기동 자체는 막지 않음).
	private final Optional<FirebaseMessaging> firebaseMessaging;
	private final UserMapper userMapper;

	@Override
	public void send(PushNotificationMessage message) {
		FirebaseMessaging messaging = firebaseMessaging.orElseThrow(
			() -> new IllegalStateException("FCM이 설정되지 않아 푸시 알림을 보낼 수 없습니다."));

		String fcmToken = userMapper.findByUserId(message.userId())
			.map(User::getFcmToken)
			.orElse(null);

		if (fcmToken == null || fcmToken.isBlank()) {
			// fcmToken이 없는 건 정상적인 상태다(로그인 안 한 기기, 구버전 클라이언트 등) -
			// 예외가 아니라 조용히 스킵한다.
			return;
		}

		// setToken()이 firebase-admin 9.10.0부터 deprecated다 - FCM이 등록 토큰 대신 Firebase
		// Installation ID(FID)를 쓰는 쪽으로 옮겨가는 중이다. 마이그레이션 기간 동안은 token
		// 필드가 FID도 그대로 받아주므로 지금 당장 깨지진 않지만, 클라이언트(앱)가 FID를
		// 발급/전송하도록 바뀌기 전까지는 이 필드명을 그대로 쓴다 - users.fcm_token 컬럼과
		// 회원가입 시 클라이언트가 보내는 값의 의미가 그대로이기 때문. 클라이언트가 FID 수집으로
		// 넘어가면 setFid()로 교체해야 한다.
		Message fcmMessage = Message.builder()
			.setToken(fcmToken)
			.setNotification(Notification.builder()
				.setTitle(message.title())
				.setBody(message.body())
				.build())
			.putAllData(message.data())
			.build();

		try {
			messaging.send(fcmMessage);
		} catch (FirebaseMessagingException e) {
			throw new IllegalStateException("푸시 알림 전송에 실패했습니다. userId=" + message.userId(), e);
		}
	}
}
