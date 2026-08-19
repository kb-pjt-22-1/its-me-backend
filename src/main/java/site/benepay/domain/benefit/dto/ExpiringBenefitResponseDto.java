package site.benepay.domain.benefit.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 놓치기 쉬운 혜택(#48) 한 항목. 이번 달 적용 중인 구간의 혜택 중 아직 안 쓴 것만 대상이다.
 */
@Getter
@Builder
public class ExpiringBenefitResponseDto {

	private String cardName;

	/**
	 * benefits_info의 serviceName
	 */
	private String serviceName;

	/**
	 * 화면 표시용 금액. monthlyDiscountLimit(월 한도) > discountAmount(정액 할인) >
	 * maximumDiscountPerTransaction(건당 한도) 순으로 존재하는 첫 값을 쓴다.
	 */
	private Long amount;

	/**
	 * 특정 가맹점 한정 혜택이면 "교보문고 한정" 같은 안내 문구, 아니면 null
	 */
	private String merchantNote;
}
