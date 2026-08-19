package site.benepay.common.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.card.service.CardService;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@ExtendWith(MockitoExtension.class)
class FacadeTest {

	private static final Long USER_ID = 1L;

	@Mock
	private CardService cardService;

	@Mock
	private MerchantService merchantService;

	@Mock
	private RecommendationService recommendationService;

	private Facade facade;

	@BeforeEach
	void setUp() {
		facade = new Facade(cardService, merchantService, recommendationService);
	}

	@Test
	void getRecommendedMerchantsAsksCardServiceForHeldCardsThenDelegatesToRecommendationService() {
		MerchantResponseDto merchant = MerchantResponseDto.builder()
			.merchantId(7L)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.build();
		List<MerchantResponseDto> merchants = List.of(merchant);
		List<RecommendationCardCandidateVO> heldCards = List.of(new RecommendationCardCandidateVO());
		List<NearbyMerchantRecommendationResponseDto> recommended = List.of(
			NearbyMerchantRecommendationResponseDto.builder()
				.merchantId(7L)
				.merchantCode("M001")
				.merchantName("테스트 식당")
				.benefitAvailable(false)
				.build()
		);

		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, merchants)).thenReturn(recommended);

		List<NearbyMerchantRecommendationResponseDto> result = facade.getRecommendedMerchants(USER_ID, merchants);

		assertThat(result).isEqualTo(recommended);
		verify(cardService).getRecommendationCandidates(USER_ID);
		verify(recommendationService).recommendMerchants(USER_ID, heldCards, merchants);
	}

	@Test
	void getCardRecommendationsAsksCardServiceForHeldCardsThenDelegatesToRecommendationService() {
		Long merchantId = 7L;
		List<RecommendationCardCandidateVO> heldCards = List.of(new RecommendationCardCandidateVO());
		MerchantCardRecommendationResponseDto response = MerchantCardRecommendationResponseDto.builder()
			.merchantId(merchantId)
			.merchantName("테스트 식당")
			.cards(List.of())
			.build();

		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.getCardRecommendations(USER_ID, merchantId, heldCards)).thenReturn(response);

		MerchantCardRecommendationResponseDto result = facade.getCardRecommendations(USER_ID, merchantId);

		assertThat(result).isEqualTo(response);
		verify(cardService).getRecommendationCandidates(USER_ID);
		verify(recommendationService).getCardRecommendations(USER_ID, merchantId, heldCards);
	}

	@Test
	void getRecommendedMerchantsReturnsEmptyListForEmptyInput() {
		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(List.of());
		when(recommendationService.recommendMerchants(USER_ID, List.of(), List.of())).thenReturn(List.of());

		assertThat(facade.getRecommendedMerchants(USER_ID, List.of())).isEmpty();
	}

	// ---- getTodayRecommendedMerchants(userId, candidates, limit) ----

	@Test
	void getTodayRecommendedMerchantsPrefersBenefitAvailableThenNearestAndCapsAtLimit() {
		List<MerchantResponseDto> candidates = List.of(MerchantResponseDto.builder().merchantId(1L).build());
		List<RecommendationCardCandidateVO> heldCards = List.of();
		// 정렬 전 순서를 일부러 뒤섞는다: 먼 혜택 매장 > 가까운 비혜택 매장 > 가까운 혜택 매장.
		NearbyMerchantRecommendationResponseDto farBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(1L).benefitAvailable(true).distanceMeters(500L).build();
		NearbyMerchantRecommendationResponseDto nearNoBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(2L).benefitAvailable(false).distanceMeters(50L).build();
		NearbyMerchantRecommendationResponseDto nearBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(3L).benefitAvailable(true).distanceMeters(100L).build();

		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, candidates))
			.thenReturn(List.of(farBenefit, nearNoBenefit, nearBenefit));

		List<NearbyMerchantRecommendationResponseDto> result =
			facade.getTodayRecommendedMerchants(USER_ID, candidates, 2);

		assertThat(result).extracting(NearbyMerchantRecommendationResponseDto::getMerchantId)
			.containsExactly(3L, 1L);
	}

	@Test
	void getTodayRecommendedMerchantsFillsRemainingSlotsWithNearestWhenNotEnoughBenefitMerchants() {
		List<MerchantResponseDto> candidates = List.of(MerchantResponseDto.builder().merchantId(1L).build());
		List<RecommendationCardCandidateVO> heldCards = List.of();
		NearbyMerchantRecommendationResponseDto onlyBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(1L).benefitAvailable(true).distanceMeters(300L).build();
		NearbyMerchantRecommendationResponseDto nearest = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(2L).benefitAvailable(false).distanceMeters(20L).build();
		NearbyMerchantRecommendationResponseDto farther = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(3L).benefitAvailable(false).distanceMeters(999L).build();

		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, candidates))
			.thenReturn(List.of(farther, onlyBenefit, nearest));

		List<NearbyMerchantRecommendationResponseDto> result =
			facade.getTodayRecommendedMerchants(USER_ID, candidates, 2);

		assertThat(result).extracting(NearbyMerchantRecommendationResponseDto::getMerchantId)
			.containsExactly(1L, 2L);
	}

	// ---- getTodayCardRecommendation(userId, lat, lng) ----
	// 실제 "지갑 전체 기준 최적 카드" 산정 로직은 recommendationService 안으로 옮겨졌으므로
	// (RecommendationServiceImplTest 참고), 여기서는 Facade가 카드/매장 두 도메인을 모아서
	// 그대로 넘기는지만 검증한다.

	@Test
	void getTodayCardRecommendationAsksCardAndMerchantServicesThenDelegatesToRecommendationService() {
		double lat = 37.5;
		double lng = 127.0;
		List<RecommendationCardCandidateVO> heldCards = List.of(new RecommendationCardCandidateVO());
		List<MerchantResponseDto> candidates = List.of(MerchantResponseDto.builder().merchantId(1L).build());
		TodayCardRecommendationResponseDto response = TodayCardRecommendationResponseDto.builder()
			.userCardId(20L).cardName("가까운 카드").categoryName("편의점").benefitLabel("10% 할인")
			.nearbyMerchants(List.of())
			.build();

		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(merchantService.getNearbyMerchants(lat, lng, null, 20)).thenReturn(candidates);
		when(recommendationService.getTodayCardRecommendation(USER_ID, heldCards, candidates)).thenReturn(response);

		TodayCardRecommendationResponseDto result = facade.getTodayCardRecommendation(USER_ID, lat, lng);

		assertThat(result).isEqualTo(response);
		verify(cardService).getRecommendationCandidates(USER_ID);
		verify(merchantService).getNearbyMerchants(lat, lng, null, 20);
		verify(recommendationService).getTodayCardRecommendation(USER_ID, heldCards, candidates);
	}
}
