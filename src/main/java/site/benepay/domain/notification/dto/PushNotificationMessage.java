package site.benepay.domain.notification.dto;

import java.util.Map;

/**
 * 알림 종류에 상관없이 공통으로 쓰는 발송 단위. 알림 종류가 늘어나도 이 레코드와
 * {@link site.benepay.domain.notification.service.PushNotificationSender}는 그대로 재사용한다 -
 * 새 종류를 추가할 때 여기를 바꿀 필요가 없다.
 */
public record PushNotificationMessage(Long userId, String title, String body, Map<String, String> data) {
}
