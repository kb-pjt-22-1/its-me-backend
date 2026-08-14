package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardRecommendationVO {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String benefitsInfo;
}
