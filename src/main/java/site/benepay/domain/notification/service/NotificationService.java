package site.benepay.domain.notification.service;

import java.util.List;

import site.benepay.domain.notification.dto.NotificationResponseDto;

public interface NotificationService {

	List<NotificationResponseDto> getMyNotifications(Long userId);

	void markAsRead(Long userId, String notificationId);
}
