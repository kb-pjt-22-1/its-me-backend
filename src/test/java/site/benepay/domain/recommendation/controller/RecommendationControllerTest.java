package site.benepay.domain.recommendation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import site.benepay.domain.recommendation.dto.CategoryCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.service.RecommendationService;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

	private static final Long USER_ID = 1L;

	@Mock
	private RecommendationService recommendationService;

	private RecommendationController controller;

	@BeforeEach
	void setUp() {
		controller = new RecommendationController(recommendationService);

		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(USER_ID);
		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getCardRecommendationsByCategoryReturnsOkWithTheServiceResultForTheAuthenticatedUser() {
		CategoryCardRecommendationResponseDto response = CategoryCardRecommendationResponseDto.builder()
			.categoryName("카페")
			.typicalPaymentAmount(10_000L)
			.cards(List.of())
			.build();
		when(recommendationService.getCardRecommendationsByCategory(USER_ID, "카페")).thenReturn(response);

		ResponseEntity<CategoryCardRecommendationResponseDto> result = controller.getCardRecommendationsByCategory("카페");

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		verify(recommendationService).getCardRecommendationsByCategory(USER_ID, "카페");
	}

	@Test
	void getNearbyMerchantsReturnsOkWithTheServiceResultForTheAuthenticatedUser() {
		List<NearbyMerchantRecommendationResponseDto> response = List.of(
			NearbyMerchantRecommendationResponseDto.builder().merchantId(100L).recommendedCardName("청춘대로 톡톡카드").build()
		);
		when(recommendationService.getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1)).thenReturn(response);

		ResponseEntity<List<NearbyMerchantRecommendationResponseDto>> result =
			controller.getNearbyMerchants(37.4, 126.9, 37.6, 127.1);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		verify(recommendationService).getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1);
	}
}
