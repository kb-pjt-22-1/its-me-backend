package site.benepay.domain.recommendation.vo;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카드 도메인이 Facade를 통해 넘기는 보유 카드 한 장 + 실적 정보. 모드 3(우선순위 비교)이
 * 전월 실적(activeTier)·이번 달 누적(baselineTier/nextTier)·과거 이력(P_이력)을 모두
 * 써야 해서 단일 월 값이 아니라 이력 전체를 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecommendationCardCandidateVO {

	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String benefitsInfo;

	// 완료된 과거 달 실적, yyyyMM -> 금액. 가장 최신 키의 값이 전월 실적(activeTier 기준)이고,
	// 전체가 P_이력(hits/months)·지갑 여력(daily_rate/cv) 계산의 재료다.
	private Map<String, Long> spendHistory;
	// 이번 달 누적 실적(진행 중) - baselineTier/nextTier(gap) 기준.
	private Long currentMonthSpend;
	// 카드 등록 일시. 신규 카드 실적 유예기간(gracePeriod) 대상인지 판단하는 기준이다 -
	// BenefitEngine.activeTierWithGracePeriod 참고. baselineTier/nextTier(다음 달 기준)에는
	// 적용하지 않는다 - 유예기간은 activeTier(now)에만 반영하기로 한 결정이다.
	private LocalDateTime userCardCreatedAt;
}