package site.benepay.domain.notification.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.notification.dto.NotificationResponseDto;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationHistoryStore notificationHistoryStore;

	@Override
	public List<NotificationResponseDto> getMyNotifications(Long userId) {
		return notificationHistoryStore.findRecent(userId).stream()
			.map(NotificationResponseDto::from)
			.collect(Collectors.toList());
	}

	@Override
	public void markAsRead(Long userId, String notificationId) {
		notificationHistoryStore.markRead(userId, notificationId);
	}
}
