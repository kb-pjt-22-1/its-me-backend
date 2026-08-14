package site.benepay.domain.card.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * BenePay user_cards 테이블의 한 행을 표현한다.
 *
 * 카드 동기화와 Webhook 처리에서 INSERT 파라미터 객체로 사용한다.
 * 객체 하나로 Mapper에 전달하므로 Sonar java:S107의 과도한 파라미터 경고도 피할 수 있다.
 */
@Getter
@Setter
@Builder
public class UserCardVO {

	private Long userCardId;
	private Long userId;
	private Long cardId;

	private String issuerCardReferenceId;
	private String issuerTokenReferenceId;
	private String paymentToken;

	private String cardExpiryYearMonth;
	private LocalDate tokenExpiryDate;
	private String panLast4;

	private String status;
	private Boolean primaryCard;
	private Boolean recommendationEnabled;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
