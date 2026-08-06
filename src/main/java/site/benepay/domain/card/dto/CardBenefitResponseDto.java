package site.benepay.domain.card.dto;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBenefitResponseDto {
	private Long userCardId;
	private Long cardId;
	private String cardName;
	private Long minBenefitAmount;

	// JsonNode로 반환 시 DB에 저장된 JSON이 응답에서 문자열로 이중 변환되지 않고
	// 실제 JSON 객체로 반환된다.
	private JsonNode benefits;
}
