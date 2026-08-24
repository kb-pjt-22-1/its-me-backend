package site.benepay.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardSyncResponseDto {

	private int syncedCount;
}
