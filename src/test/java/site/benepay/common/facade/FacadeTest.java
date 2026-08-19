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
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.RecommendedCardResponseDto;
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
			.merchantId(1L).benefitAvailable(true).distanceMeters(500.0).build();
		NearbyMerchantRecommendationResponseDto nearNoBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(2L).benefitAvailable(false).distanceMeters(50.0).build();
		NearbyMerchantRecommendationResponseDto nearBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(3L).benefitAvailable(true).distanceMeters(100.0).build();

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
			.merchantId(1L).benefitAvailable(true).distanceMeters(300.0).build();
		NearbyMerchantRecommendationResponseDto nearest = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(2L).benefitAvailable(false).distanceMeters(20.0).build();
		NearbyMerchantRecommendationResponseDto farther = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(3L).benefitAvailable(false).distanceMeters(999.0).build();

		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, candidates))
			.thenReturn(List.of(farther, onlyBenefit, nearest));

		List<NearbyMerchantRecommendationResponseDto> result =
			facade.getTodayRecommendedMerchants(USER_ID, candidates, 2);

		assertThat(result).extracting(NearbyMerchantRecommendationResponseDto::getMerchantId)
			.containsExactly(1L, 2L);
	}

	// ---- getTodayCardRecommendation(userId, lat, lng) ----

	@Test
	void getTodayCardRecommendationLooksUpNearbyMerchantsItselfThenPicksTheNearestBenefitMerchantAsFeatured() {
		double lat = 37.5;
		double lng = 127.0;
		List<MerchantResponseDto> candidates = List.of(MerchantResponseDto.builder().merchantId(1L).build());
		List<RecommendationCardCandidateVO> heldCards = List.of();

		RecommendedCardResponseDto farCard = RecommendedCardResponseDto.builder()
			.userCardId(10L).cardName("먼 카드").benefitSummary("5% 할인").build();
		RecommendedCardResponseDto nearCard = RecommendedCardResponseDto.builder()
			.userCardId(20L).cardName("가까운 카드").benefitSummary("10% 할인").build();
		NearbyMerchantRecommendationResponseDto farBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(1L).merchantName("먼 매장").categoryName("카페").benefitAvailable(true)
			.distanceMeters(500.0).recommendedCards(List.of(farCard)).build();
		NearbyMerchantRecommendationResponseDto nearNoBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(2L).merchantName("혜택 없는 매장").benefitAvailable(false).distanceMeters(50.0).build();
		NearbyMerchantRecommendationResponseDto nearBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(3L).merchantName("가까운 매장").categoryName("편의점").benefitAvailable(true)
			.distanceMeters(100.0).recommendedCards(List.of(nearCard)).build();

		when(merchantService.getNearbyMerchants(lat, lng, null, 20)).thenReturn(candidates);
		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, candidates))
			.thenReturn(List.of(farBenefit, nearNoBenefit, nearBenefit));

		TodayCardRecommendationResponseDto result = facade.getTodayCardRecommendation(USER_ID, lat, lng);

		assertThat(result.getUserCardId()).isEqualTo(20L);
		assertThat(result.getCardName()).isEqualTo("가까운 카드");
		assertThat(result.getCategoryName()).isEqualTo("편의점");
		assertThat(result.getBenefitLabel()).isEqualTo("10% 할인");
		assertThat(result.getNearbyMerchants()).extracting(TodayCardRecommendationResponseDto.NearbyMerchant::getMerchantId)
			.containsExactly(1L);
		verify(merchantService).getNearbyMerchants(lat, lng, null, 20);
	}

	@Test
	void getTodayCardRecommendationReturnsEmptyWhenNoMerchantHasAnyBenefit() {
		double lat = 37.5;
		double lng = 127.0;
		List<MerchantResponseDto> candidates = List.of(MerchantResponseDto.builder().merchantId(1L).build());
		List<RecommendationCardCandidateVO> heldCards = List.of();
		NearbyMerchantRecommendationResponseDto noBenefit = NearbyMerchantRecommendationResponseDto.builder()
			.merchantId(1L).benefitAvailable(false).distanceMeters(50.0).build();

		when(merchantService.getNearbyMerchants(lat, lng, null, 20)).thenReturn(candidates);
		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, candidates)).thenReturn(List.of(noBenefit));

		TodayCardRecommendationResponseDto result = facade.getTodayCardRecommendation(USER_ID, lat, lng);

		assertThat(result.getUserCardId()).isNull();
		assertThat(result.getCardName()).isNull();
		assertThat(result.getNearbyMerchants()).isEmpty();
	}
}
