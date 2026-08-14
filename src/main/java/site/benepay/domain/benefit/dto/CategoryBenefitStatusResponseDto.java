package site.benepay.domain.benefit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryBenefitStatusResponseDto {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;

	/**
	 * 조회 기준 연월. 예: 2026-08
	 */
	private String yearMonth;

	private String categoryCode;
	private String categoryName;

	/**
	 * 이 카테고리에 적용 중인 혜택명(benefits_info의 serviceName)
	 */
	private String serviceName;

	/**
	 * 월 한도 금액. 한도가 없으면 null
	 */
	private Long amountLimit;

	/**
	 * 이번 달 실제로 소진한 혜택 금액
	 */
	private Long usedAmount;

	/**
	 * 한도까지 남은 금액. 한도가 없으면 null
	 */
	private Long remainingAmount;

	/**
	 * 금액 한도 소진 여부. 한도가 없으면 항상 false
	 */
	private boolean amountLimitReached;

	/**
	 * 월 한도 횟수. 한도가 없으면 null
	 */
	private Integer countLimit;

	/**
	 * 이번 달 혜택이 적용된 결제 횟수
	 */
	private Integer usedCount;

	/**
	 * 한도까지 남은 횟수. 한도가 없으면 null
	 */
	private Integer remainingCount;

	/**
	 * 횟수 한도 소진 여부. 한도가 없으면 항상 false
	 */
	private boolean countLimitReached;
}
