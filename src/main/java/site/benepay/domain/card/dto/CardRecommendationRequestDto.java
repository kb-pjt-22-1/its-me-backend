package site.benepay.domain.card.dto;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardRecommendationRequestDto {

	@NotNull(message = "추천 포함 여부는 필수입니다.")
	private Boolean recommendationEnabled;
}
