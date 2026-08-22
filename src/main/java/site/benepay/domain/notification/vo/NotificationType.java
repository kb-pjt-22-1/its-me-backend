package site.benepay.domain.notification.vo;

/**
 * 알림 이력 API 응답의 type 필드 값. 프론트가 이 문자열(name())로 클릭 시 라우팅 대상을
 * 정하므로(PAYMENT_APPROVED -> /payments/{relatedId}, NEARBY_MERCHANT -> /stores/{relatedId}),
 * 이름을 바꾸면 계약이 깨진다.
 */
public enum NotificationType {
	PAYMENT_APPROVED,
	NEARBY_MERCHANT
}
