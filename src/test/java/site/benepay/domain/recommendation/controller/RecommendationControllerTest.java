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

import site.benepay.common.facade.Facade;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 100L;

	@Mock
	private Facade facade;

	private RecommendationController controller;

	@BeforeEach
	void setUp() {
		controller = new RecommendationController(facade);

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
	void getCardRecommendationsReturnsOkWithTheServiceResultForTheAuthenticatedUser() {
		MerchantCardRecommendationResponseDto response = MerchantCardRecommendationResponseDto.builder()
			.merchantId(MERCHANT_ID)
			.merchantName("스타벅스 강남점")
			.categoryCode("5813")
			.cards(List.of())
			.build();
		when(facade.getCardRecommendations(USER_ID, MERCHANT_ID)).thenReturn(response);

		ResponseEntity<MerchantCardRecommendationResponseDto> result = controller.getCardRecommendations(MERCHANT_ID);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		verify(facade).getCardRecommendations(USER_ID, MERCHANT_ID);
	}

	@Test
	void getTodayCardRecommendationDelegatesToTheFacadeWithTheAuthenticatedUserAndCoordinates() {
		double lat = 37.5;
		double lng = 127.0;
		TodayCardRecommendationResponseDto response = TodayCardRecommendationResponseDto.builder()
			.userCardId(1L)
			.cardName("KB국민 My WE:SH카드")
			.categoryName("카페")
			.benefitLabel("이번 달 5% 할인")
			.nearbyMerchants(List.of())
			.build();
		when(facade.getTodayCardRecommendation(USER_ID, lat, lng)).thenReturn(response);

		ResponseEntity<TodayCardRecommendationResponseDto> result = controller.getTodayCardRecommendation(lat, lng);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
		verify(facade).getTodayCardRecommendation(USER_ID, lat, lng);
	}
}
