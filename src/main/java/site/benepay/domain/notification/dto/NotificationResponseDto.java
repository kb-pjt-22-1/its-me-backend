package site.benepay.domain.notification.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.notification.vo.NotificationHistoryVO;

@Getter
@Builder
public class NotificationResponseDto {

	private String notificationId;
	private String type;
	private String title;
	private String body;
	private Long relatedId;
	private LocalDateTime createdAt;
	private boolean read;

	public static NotificationResponseDto from(NotificationHistoryVO history) {
		return NotificationResponseDto.builder()
			.notificationId(history.getNotificationId())
			.type(history.getType())
			.title(history.getTitle())
			.body(history.getBody())
			.relatedId(history.getRelatedId())
			.createdAt(LocalDateTime.parse(history.getCreatedAt()))
			.read(history.isRead())
			.build();
	}
}
