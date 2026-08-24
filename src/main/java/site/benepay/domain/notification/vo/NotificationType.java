package site.benepay.domain.notification.vo;

/**
 * 알림 이력 API 응답의 type 필드 값. 프론트가 이 문자열(name())로 클릭 시 라우팅 대상을
 * 정하므로(PAYMENT_APPROVED -> /payments/{relatedId}, NEARBY_MERCHANT -> /stores/{relatedId}),
 * 이름을 바꾸면 계약이 깨진다.
 *
 * <p>SESSION_DISPLACED는 relatedId가 항상 null이다 - 라우팅 대상이 없고(로그인 화면으로
 * 돌아가는 것 외에 이동할 곳이 없음), 알림을 받은 그 기기 자신이 곧 로그아웃 처리 대상이다.
 */
public enum NotificationType {
	PAYMENT_APPROVED,
	NEARBY_MERCHANT,
	SESSION_DISPLACED
}
