package site.benepay.common.event;

import java.util.List;

import lombok.Getter;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

/**
 * 매장 도메인이 "유저 아이디 + 위치정보"로 진입점을 받아, 카드 도메인에서 받은 유저 보유 카드와
 * 자신이 조회한 위치 기반 매장 리스트를 실어 발행하는 이벤트(가정) - 추천 도메인은 이 이벤트의
 * 리스너만 구현한다. Spring 이벤트는 기본적으로 같은 스레드에서 동기 실행되므로, 리스너가
 * {@link #completeWith}로 결과를 채우면 이 이벤트를 발행한 쪽은 publishEvent() 호출이 끝난
 * 직후 바로 그 결과를 읽어 프론트로 응답할 수 있다.
 */
@Getter
public class MerchantRecommendationRequestedEvent {

	private final Long userId;
	private final List<RecommendationCardCandidateVO> heldCards;
	private final List<NearbyMerchantResponseDto> merchants;

	private List<NearbyMerchantRecommendationResponseDto> result;

	public MerchantRecommendationRequestedEvent(
		Long userId,
		List<RecommendationCardCandidateVO> heldCards,
		List<NearbyMerchantResponseDto> merchants
	) {
		this.userId = userId;
		this.heldCards = heldCards;
		this.merchants = merchants;
	}

	public void completeWith(List<NearbyMerchantRecommendationResponseDto> result) {
		this.result = result;
	}
}
