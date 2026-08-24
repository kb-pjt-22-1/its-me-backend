package site.benepay.domain.notification.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.notification.dto.LocationUpdateRequestDto;
import site.benepay.domain.notification.service.LocationService;

// @AuthenticationPrincipal은 standalone MockMvc가 못 풀어주므로(MerchantControllerTest와
// 동일한 이유), 컨트롤러 메서드를 직접 호출한다.
@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

	private static final Long USER_ID = 1L;

	@Mock
	private LocationService locationService;

	@Test
	void passesUserIdAndCoordinatesToServiceAndReturnsNoContent() {
		LocationController controller = new LocationController(locationService);
		LocationUpdateRequestDto request = new LocationUpdateRequestDto(37.5, 127.0);

		ResponseEntity<Void> response = controller.updateLocation(USER_ID, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(locationService).updateLocation(USER_ID, 37.5, 127.0);
	}
}
