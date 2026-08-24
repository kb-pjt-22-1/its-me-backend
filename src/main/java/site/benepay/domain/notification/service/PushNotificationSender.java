package site.benepay.domain.notification.service;

import site.benepay.domain.notification.dto.PushNotificationMessage;

/**
 * 실제 푸시 전송 채널(FCM 등)을 감춘다. 알림을 트리거하는 쪽(이벤트 리스너, 배치 Job)은
 * 이 인터페이스만 알면 되고, 전송 수단이 바뀌어도 트리거 쪽 코드는 바뀌지 않는다.
 */
public interface PushNotificationSender {

	/**
	 * 전송 실패 시 unchecked 예외를 던진다. 실패를 어떻게 처리할지(로그만 남기고 계속 진행할지,
	 * 재시도할지)는 호출부(이벤트 리스너 등)가 맥락에 맞게 결정한다.
	 */
	void send(PushNotificationMessage message);
}
