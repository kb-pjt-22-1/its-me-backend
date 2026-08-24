package site.benepay.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.notification.dto.NotificationResponseDto;
import site.benepay.domain.notification.service.NotificationService;

// @AuthenticationPrincipal은 standalone MockMvc가 못 풀어주므로(LocationControllerTest와 동일한 이유),
// 컨트롤러 메서드를 직접 호출한다.
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

	private static final Long USER_ID = 1L;

	@Mock
	private NotificationService notificationService;

	@Test
	void returnsTheUsersNotificationsFromTheService() {
		NotificationController controller = new NotificationController(notificationService);
		NotificationResponseDto notification = NotificationResponseDto.builder()
			.notificationId("11111111-1111-1111-1111-111111111111")
			.type("PAYMENT_APPROVED")
			.title("결제가 완료됐어요")
			.body("스타벅스 강남점에서 15,000원 결제했어요.")
			.relatedId(100L)
			.read(false)
			.build();
		when(notificationService.getMyNotifications(USER_ID)).thenReturn(List.of(notification));

		ResponseEntity<List<NotificationResponseDto>> response = controller.getMyNotifications(USER_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsExactly(notification);
	}

	@Test
	void marksANotificationAsReadAndReturnsNoContent() {
		NotificationController controller = new NotificationController(notificationService);
		String notificationId = "11111111-1111-1111-1111-111111111111";

		ResponseEntity<Void> response = controller.markAsRead(USER_ID, notificationId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(notificationService).markAsRead(USER_ID, notificationId);
	}
}
