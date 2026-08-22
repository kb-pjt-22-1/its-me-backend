package site.benepay.domain.notification.event;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.NotificationHistoryStore;
import site.benepay.domain.notification.service.PushNotificationSender;
import site.benepay.domain.notification.vo.NotificationType;
import site.benepay.domain.user.event.SessionDisplacedEvent;

/**
 * 다른 기기에서 같은 계정으로 로그인해 기존 세션이 강제로 로그인 해제됐을 때, 해제된
 * 기기(=알림을 받는 그 기기 자신)에 푸시 알림을 보낸다.
 *
 * <p>AFTER_COMMIT을 쓰는 이유: 이벤트를 발행하는 TokenServiceImpl.issueTokenPair는
 * AuthServiceImpl.login()의 @Transactional(readOnly = true) 트랜잭션 안에서 호출된다.
 * 로그인 자체가 실패(예: 락 처리 중 예외)해서 롤백되는 경우까지 포함해, 로그인이 실제로
 * 확정된 뒤에만 "다른 기기에서 로그인했다"는 알림을 보내야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionDisplacedPushHandler {

	private final PushNotificationSender pushNotificationSender;
	private final NotificationHistoryStore notificationHistoryStore;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(SessionDisplacedEvent event) {
		String title = "다른 기기에서 로그인됐어요";
		String body = "다른 기기에서 로그인되어 이 기기는 로그아웃됐어요. 본인이 아니라면 비밀번호를 변경해주세요.";

		try {
			pushNotificationSender.send(new PushNotificationMessage(
				event.userId(),
				title,
				body,
				Map.of()
			));
		} catch (RuntimeException e) {
			// 로그인 자체는 이미 정상적으로 끝난 뒤라, 알림 전송 실패가 그 결과에 영향을 주면 안 된다.
			log.warn("로그인 해제 푸시 알림 전송 실패. userId={}", event.userId(), e);
		}

		try {
			notificationHistoryStore.record(event.userId(), NotificationType.SESSION_DISPLACED, title, body, null);
		} catch (RuntimeException e) {
			// 이력 저장 실패가 푸시 전송이나 로그인 처리 결과에 영향을 주면 안 된다 - 위와 같은 격리 원칙.
			log.warn("로그인 해제 알림 이력 저장 실패. userId={}", event.userId(), e);
		}
	}
}
