package site.benepay.domain.card.vo;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * KB카드 신규 발급 Webhook 수신 이력과 처리 결과를 저장하는 VO.
 */
@Getter
@Builder
public class CardIssuanceEventVO {

	private Long id;
	private String eventId;
	private String ciHash;
	private String cardReferenceId;
	private String issuerProductCode;
	private String cardLast4;
	private String cardType;
	private String cardStatus;

	private String processingStatus;
	private String failReason;

	private LocalDateTime receivedAt;
	private LocalDateTime processedAt;
}
