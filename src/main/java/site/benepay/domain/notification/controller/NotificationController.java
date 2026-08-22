package site.benepay.domain.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.notification.dto.NotificationResponseDto;
import site.benepay.domain.notification.service.NotificationService;

@RestController
@RequestMapping("/api/users/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(notificationService.getMyNotifications(userId));
	}

	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<Void> markAsRead(
		@AuthenticationPrincipal Long userId,
		@PathVariable String notificationId
	) {
		notificationService.markAsRead(userId, notificationId);
		return ResponseEntity.noContent().build();
	}
}
