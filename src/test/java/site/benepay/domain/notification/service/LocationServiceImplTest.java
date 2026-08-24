package site.benepay.domain.notification.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import site.benepay.domain.notification.event.UserLocationUpdatedEvent;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Test
	void publishesUserLocationUpdatedEventWithGivenCoordinates() {
		LocationServiceImpl service = new LocationServiceImpl(eventPublisher);

		service.updateLocation(1L, 37.5, 127.0);

		verify(eventPublisher).publishEvent(new UserLocationUpdatedEvent(1L, 37.5, 127.0));
	}
}
