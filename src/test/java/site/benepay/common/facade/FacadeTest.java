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
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@ExtendWith(MockitoExtension.class)
class FacadeTest {

	private static final Long USER_ID = 1L;

	@Mock
	private CardService cardService;

	@Mock
	private RecommendationService recommendationService;

	private Facade facade;

	@BeforeEach
	void setUp() {
		facade = new Facade(cardService, recommendationService);
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
}
