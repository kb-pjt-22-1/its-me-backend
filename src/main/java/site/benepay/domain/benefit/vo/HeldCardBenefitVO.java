package site.benepay.domain.benefit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 카테고리별 혜택 현황(한도 소진 현황) 계산에 필요한 보유 카드 read model.
 * benefits_info 파싱 재료 + 전월 실적(이번 달 적용 중인 구간을 정하는 기준)을 한 번에 담는다.
 */
@Getter
@Setter
public class HeldCardBenefitVO {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String benefitsInfo;

	/**
	 * 전월 실적. BenefitEngine.activeTier의 prevMonthSpend로 그대로 넘겨
	 * 이번 달 적용 중인 혜택 구간을 정한다(추천 도메인 모드 3과 동일 규약).
	 */
	private Long previousMonthSpendingAmount;
}
