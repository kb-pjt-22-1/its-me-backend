package site.benepay.common.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

/**
 * Firebase Cloud Messaging 연결 설정.
 *
 * <p>서비스 계정 자격증명(firebase.credentials-json)이 비어 있으면(로컬 개발 등) 빈을 등록하지
 * 않는다 - FcmPushNotificationSender가 Optional&lt;FirebaseMessaging&gt;로 주입받아 없으면
 * 호출 시점에 예외를 던지므로, 앱 기동 자체는 자격증명 없이도 항상 성공한다.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

	@Bean
	public FirebaseMessaging firebaseMessaging(
		@Value("${firebase.credentials-json:}") String credentialsJson
	) throws IOException {
		if (credentialsJson == null || credentialsJson.isBlank()) {
			log.warn("firebase.credentials-json이 비어 있어 FCM을 초기화하지 않습니다. 푸시 알림은 발송되지 않습니다.");
			return null;
		}

		GoogleCredentials credentials = GoogleCredentials.fromStream(
			new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));

		FirebaseOptions options = FirebaseOptions.builder()
			.setCredentials(credentials)
			.build();

		FirebaseApp app = FirebaseApp.getApps().isEmpty()
			? FirebaseApp.initializeApp(options)
			: FirebaseApp.getInstance();

		return FirebaseMessaging.getInstance(app);
	}
}
