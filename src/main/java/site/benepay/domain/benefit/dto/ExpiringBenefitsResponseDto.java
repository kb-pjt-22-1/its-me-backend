package site.benepay.domain.benefit.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpiringBenefitsResponseDto {

	/**
	 * 이번 달 마감까지 남은 일수(오늘 포함)
	 */
	private int daysRemaining;

	/**
	 * 아직 안 쓴 혜택 중 금액 큰 순 상위 3개
	 */
	private List<ExpiringBenefitResponseDto> benefits;

	/**
	 * 가장 최근 결제한 곳 반경 2km 이내에서 혜택 받을 수 있는 매장, 가까운 순 최대 3곳.
	 * 결제 이력이 없거나 반경 내에 혜택 가능한 매장이 없으면 빈 리스트.
	 */
	private List<NearbyBenefitResponseDto> nearbyMerchantBenefits;
}