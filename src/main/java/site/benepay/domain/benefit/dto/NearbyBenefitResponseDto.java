package site.benepay.domain.benefit.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * "최근 결제한 곳 주변에서 받을 수 있는 혜택" 한 항목(#48 2번 섹션).
 */
@Getter
@Builder
public class NearbyBenefitResponseDto {

	private String merchantName;

	/**
	 * 이 매장에서 가장 유리한 보유 카드명. 혜택 가능한 카드가 없으면 이 항목 자체가
	 * 응답에 안 담긴다(항상 not-null).
	 */
	private String cardName;

	/**
	 * 카드 추천 엔진(모드 3)이 만든 설명 문장 - "이번 달 확정 N원 + 다음 달 기대 M원..."
	 * 형태라 다소 길다. 화면에 짧게 보여줘야 하면 프론트에서 가공이 필요할 수 있다.
	 */
	private String benefitSummary;

	private Long distanceMeters;
}
