package site.benepay.integration.kbcard.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KB카드 Mock Server의 고객별 보유 카드 조회 응답 전체를 담는다.
 *
 * 고객이 없거나 카드가 없으면 cards가 빈 목록으로 반환된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class KbCustomerCardsResponseDto {

	private String customerReferenceId;
	private String customerName;
	private List<KbCardResponseDto> cards = new ArrayList<>();
}
