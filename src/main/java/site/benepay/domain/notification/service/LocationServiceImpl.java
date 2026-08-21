package site.benepay.domain.notification.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.notification.event.UserLocationUpdatedEvent;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public void updateLocation(Long userId, double latitude, double longitude) {
		eventPublisher.publishEvent(new UserLocationUpdatedEvent(userId, latitude, longitude));
	}
}
