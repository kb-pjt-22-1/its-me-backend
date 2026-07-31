package site.benepay.domain.card.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardDetailVO {

	private Long userCardId;
	private Long cardId;

	private String cardName;
	private String cardType;
	private String cardImageUrl;
	private String description;

	private String cardNetwork;
	private Long annualFee;

	private String panLast4;
	private LocalDate tokenExpiryDate;
	private String status;

	private Boolean primaryCard;
	private Boolean recommendationEnabled;
	private Boolean supported;

	private Long minBenefitAmount;

	private LocalDateTime createdAt;
}
