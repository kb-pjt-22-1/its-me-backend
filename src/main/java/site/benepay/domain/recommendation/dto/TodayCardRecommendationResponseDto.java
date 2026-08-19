package site.benepay.domain.recommendation.dto;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 홈 화면 "오늘의 카드 추천" - 사용자 현재 위치 기준으로 지금 혜택받을 수 있는 매장 중 가장
 * 가까운 곳의 최적 카드를 대표로 보여준다. 근처에 혜택 매장이 하나도 없으면 필드가 전부
 * null/빈 리스트인 채로 반환된다(빈 추천).
 */
@Getter
@Builder
public class TodayCardRecommendationResponseDto {

	private Long userCardId;
	private String cardName;
	private String categoryName;
	private String benefitLabel;
	private List<NearbyMerchant> nearbyMerchants;

	public static TodayCardRecommendationResponseDto empty() {
		return TodayCardRecommendationResponseDto.builder()
			.nearbyMerchants(Collections.emptyList())
			.build();
	}

	@Getter
	@Builder
	public static class NearbyMerchant {

		private Long merchantId;
		private String merchantName;
		private Long distanceMeters;
		private String benefitLabel;
	}
}
