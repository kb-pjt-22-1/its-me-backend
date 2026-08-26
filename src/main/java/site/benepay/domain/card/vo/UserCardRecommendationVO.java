package site.benepay.domain.card.vo;

import java.time.LocalDateTime;

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
	// 카드 등록 일시. 신규 카드 실적 유예기간(gracePeriod) 대상인지 판단하는 기준이다 -
	// BenefitEngine.activeTierWithGracePeriod 참고.
	private LocalDateTime userCardCreatedAt;
}
