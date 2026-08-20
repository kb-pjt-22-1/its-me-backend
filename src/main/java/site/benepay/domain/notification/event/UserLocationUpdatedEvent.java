package site.benepay.domain.notification.event;

/**
 * 유저가 현재 위치를 보고했을 때 발행된다. 위치 자체는 저장하지 않고(개인정보 최소 보관),
 * 이 이벤트를 구독하는 리스너들이 그 순간의 위치로 필요한 판단만 하고 버린다.
 *
 * <p>위치 기반으로 반응할 알림 종류가 늘어나도(예: "근처 혜택 매장 알림") 이 이벤트와 발행
 * 지점(LocationServiceImpl)은 그대로 두고, 새 리스너만 추가하면 된다.
 */
public record UserLocationUpdatedEvent(Long userId, double latitude, double longitude) {
}
