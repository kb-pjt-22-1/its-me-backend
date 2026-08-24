package site.benepay.domain.payment.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 결제 완료 시점에 어느 혜택을 적용할지 계산하는 데 필요한 카드 정보. benefits_info 파싱
 * 재료 + 전월 실적(BenefitEngine.activeTier의 기준)을 한 번에 담는다 - benefit 도메인의
 * HeldCardBenefitVO와 모양이 같지만, 도메인 경계를 지키기 위해 payment 도메인 자체 VO로 둔다
 * (PaymentMapper가 이미 user_cards를 직접 조회하는 관례를 따름 - findActiveCardPaymentToken 참고).
 */
@Getter
@Setter
public class CardBenefitContextVO {

	private String benefitsInfo;
	private Long previousMonthSpendingAmount;
}
