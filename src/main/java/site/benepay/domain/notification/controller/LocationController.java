package site.benepay.domain.notification.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.notification.dto.LocationUpdateRequestDto;
import site.benepay.domain.notification.service.LocationService;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class LocationController {

	private final LocationService locationService;

	/**
	 * 클라이언트가 주기적으로(예: 위치 유의미하게 변할 때) 현재 위치를 보고한다. 위치는
	 * 저장하지 않고 그 순간 판단(예: 저장한 매장 근처 알림)에만 쓰인다.
	 */
	@PostMapping("/location")
	public ResponseEntity<Void> updateLocation(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody LocationUpdateRequestDto request
	) {
		locationService.updateLocation(userId, request.getLatitude(), request.getLongitude());
		return ResponseEntity.noContent().build();
	}
}
