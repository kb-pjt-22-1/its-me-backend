package site.benepay.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardRepresentativeResponseDto {

	private Long userCardId;
	private Boolean primary;
}
