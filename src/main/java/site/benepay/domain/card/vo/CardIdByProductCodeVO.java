package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * CardMapper.findCardIdsByIssuerProductCodes 전용 - KB 상품 코드 하나를 BenePay card_id로
 * 매핑한 결과 한 줄.
 */
@Getter
@Setter
public class CardIdByProductCodeVO {

	private String issuerProductCode;
	private Long cardId;
}
