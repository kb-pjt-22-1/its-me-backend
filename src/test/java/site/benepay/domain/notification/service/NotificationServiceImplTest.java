package site.benepay.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.notification.dto.NotificationResponseDto;
import site.benepay.domain.notification.vo.NotificationHistoryVO;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	private static final Long USER_ID = 1L;

	@Mock
	private NotificationHistoryStore notificationHistoryStore;

	private NotificationServiceImpl service;

	private NotificationHistoryVO history() {
		return new NotificationHistoryVO(
			"11111111-1111-1111-1111-111111111111",
			"PAYMENT_APPROVED",
			"결제가 완료됐어요",
			"스타벅스 강남점에서 15,000원 결제했어요.",
			100L,
			"2026-08-21T10:00:00",
			false
		);
	}

	@Test
	void mapsStoredHistoryToResponseDtosInTheStoresOrder() {
		service = new NotificationServiceImpl(notificationHistoryStore);
		when(notificationHistoryStore.findRecent(USER_ID)).thenReturn(List.of(history()));

		List<NotificationResponseDto> result = service.getMyNotifications(USER_ID);

		assertThat(result).hasSize(1);
		NotificationResponseDto dto = result.get(0);
		assertThat(dto.getNotificationId()).isEqualTo("11111111-1111-1111-1111-111111111111");
		assertThat(dto.getType()).isEqualTo("PAYMENT_APPROVED");
		assertThat(dto.getRelatedId()).isEqualTo(100L);
		assertThat(dto.isRead()).isFalse();
	}

	@Test
	void delegatesMarkAsReadToTheStore() {
		service = new NotificationServiceImpl(notificationHistoryStore);

		service.markAsRead(USER_ID, "11111111-1111-1111-1111-111111111111");

		verify(notificationHistoryStore).markRead(USER_ID, "11111111-1111-1111-1111-111111111111");
	}
}
