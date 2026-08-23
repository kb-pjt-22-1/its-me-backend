package site.benepay.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendedCardResponseDto {

	private Long userCardId;
	private String cardName;
	private String benefitSummary;
	// Mode3Result.now()(이번 달 확정 혜택, 원 단위) - total이 아니라 now를 쓴다. total은
	// 다음 달 기대치까지 섞여 있어 benefitAvailableNow와 기준이 어긋난다(클래스 상단 설명
	// 참고). 프론트가 typicalPaymentAmount와 나눠 실질 할인율로 정렬하는 용도.
	private Long discountAmount;
}
