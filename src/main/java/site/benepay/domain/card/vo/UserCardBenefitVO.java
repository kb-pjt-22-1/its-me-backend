package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardBenefitVO {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private Long minBenefitAmount;

	// MySQL JSON 컬럼은 우선 문자열로 조회
	private String benefitsInfo;
}
