package site.benepay.domain.card.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardVO {

	private Long userCardId;
	private Long userId;
	private Long cardId;

	private String tokenId;
	private String paymentToken;
	private LocalDate tokenExpiryDate;

	private String panHash;
	private String panLast4;
	private String par;

	private String status;
	private Boolean primaryCard;
	private Boolean recommendationEnabled;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
