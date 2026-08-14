package site.benepay.integration.kbcard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KB카드 Mock Server에서 전달하는 신규 카드 발급 Webhook 요청 DTO.
 *
 * <p>
 * eventId로 중복 이벤트를 확인하고,
 * ciHash와 cardReferenceId로 사용자와 발급 카드를 식별한다.
 * </p>
 */
@Getter
@NoArgsConstructor
public class CardIssuedWebhookRequestDto {

	private String eventId;

	private String ciHash;

	private String cardReferenceId;
	private String issuerProductCode;

	private String cardLast4;
	private String cardType;
	private String cardStatus;
}
