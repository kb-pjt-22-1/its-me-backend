package site.benepay.common.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import site.benepay.domain.card.service.CardService;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

class FacadeTest {

	private static final Long USER_ID = 1L;

	private final CardService cardService = mock(CardService.class);
	private final RecommendationService recommendationService = mock(RecommendationService.class);
	private final Facade facade = new Facade(cardService, recommendationService);

	@Test
	void getRecommendedMerchantsMapsEveryMerchantWithoutMarkingAnyAsRecommendedYet() {
		MerchantResponseDto merchant = MerchantResponseDto.builder()
			.merchantId(7L)
			.categoryCode("5812")
			.brandId(1L)
			.merchantCode("M001")
			.merchantName("테스트 식당")
			.address("서울시 강남구")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.phone("02-000-0000")
			.build();

		List<RecommendationCardCandidateVO> heldCards = List.of();
		List<NearbyMerchantRecommendationResponseDto> recommendations = List.of(
			NearbyMerchantRecommendationResponseDto.from(merchant));
		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(heldCards);
		when(recommendationService.recommendMerchants(USER_ID, heldCards, List.of(merchant)))
			.thenReturn(recommendations);

		List<NearbyMerchantRecommendationResponseDto> result =
			facade.getRecommendedMerchants(USER_ID, List.of(merchant));

		assertThat(result).isEqualTo(recommendations);
		assertThat(result.get(0).getMerchantId()).isEqualTo(merchant.getMerchantId());
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		verify(cardService).getRecommendationCandidates(USER_ID);
		verify(recommendationService).recommendMerchants(USER_ID, heldCards, List.of(merchant));
	}

	@Test
	void getRecommendedMerchantsReturnsEmptyListForEmptyInput() {
		when(cardService.getRecommendationCandidates(USER_ID)).thenReturn(List.of());
		when(recommendationService.recommendMerchants(USER_ID, List.of(), List.of())).thenReturn(List.of());

		assertThat(facade.getRecommendedMerchants(USER_ID, List.of())).isEmpty();
	}
}
