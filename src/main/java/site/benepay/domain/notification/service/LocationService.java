package site.benepay.domain.notification.service;

public interface LocationService {

	void updateLocation(Long userId, double latitude, double longitude);
}
