package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

//목록 조회 결과용 객체
public class UserCardListVO {

	private Long userCardId;
	private Long cardId;

	private String cardName;
	private String cardType;
	private String cardImageUrl;

	private String cardNetwork;
	private Long annualFee;

	private String panLast4;
	private String status;

	private Boolean primaryCard;
	private Boolean recommendationEnabled;
}
