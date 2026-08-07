package site.benepay.domain.recommendation.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * '카페' 같은 대분류 검색에 대한 보유 카드 순위 응답. 상태별(즉시할인 -> 조건부할인 -> ...)로
 * 묶이고 그룹 안에서는 할인율 내림차순으로 정렬돼 있다.
 */
@Getter
@Builder
public class CategoryCardRecommendationResponseDto {

	private String categoryName;
	private long typicalPaymentAmount;
	private List<CardBenefitScoreDto> cards;
}
