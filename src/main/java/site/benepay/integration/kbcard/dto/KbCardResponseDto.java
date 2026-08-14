package site.benepay.integration.kbcard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KB카드 Mock Server가 반환한 카드 한 장의 정보다.
 *
 * 이 DTO는 외부 API 응답을 받는 용도이며,
 * BenePay DB의 user_cards와 정확히 같은 객체는 아니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class KbCardResponseDto {
	
	private String cardReferenceId;

	/**
	 * Mock Server의 mock_card_products.product_code.
	 * BenePay cards.issuer_product_code와 매칭한다.
	 */
	private String productCode;

	private String cardName;
	private String cardType;
	private String cardNetwork;

	private String panLast4;
	private String cardExpiryYearMonth;
	private String cardStatus;
	private LocalDateTime issuedAt;

	private String tokenReferenceId;
	private String paymentToken;
	private LocalDate tokenExpiryDate;
	private String tokenStatus;
}
