package site.benepay.domain.notification.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 이력 Redis 저장 포맷. NotificationHistoryStore가 다루는 로컬 ObjectMapper에는
 * JavaTimeModule이 등록돼 있지 않아(JacksonConfig 빈과 별개), createdAt을 LocalDateTime이
 * 아니라 ISO-8601 문자열로 둔다 - PaymentTokenVO.issuedAt과 동일한 이유.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryVO {

	private String notificationId;
	private String type;
	private String title;
	private String body;
	private Long relatedId;
	private String createdAt;
	private boolean read;
}
