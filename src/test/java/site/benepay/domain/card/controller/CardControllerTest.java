package site.benepay.domain.card.controller;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import site.benepay.domain.card.dto.CardBenefitResponseDto;
import site.benepay.domain.card.dto.CardDetailResponseDto;
import site.benepay.domain.card.dto.CardListResponseDto;
import site.benepay.domain.card.dto.CardPerformanceResponseDto;
import site.benepay.domain.card.dto.CardRecommendationRequestDto;
import site.benepay.domain.card.dto.CardRecommendationResponseDto;
import site.benepay.domain.card.dto.CardRepresentativeResponseDto;
import site.benepay.domain.card.service.CardService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 100L;

	@Mock
	private CardService cardService;

	private CardController controller;

	@BeforeEach
	void setUp() {
		controller = new CardController(cardService);
	}

	private CardRecommendationRequestDto recommendationRequest(Boolean enabled) {
		CardRecommendationRequestDto request = new CardRecommendationRequestDto();
		ReflectionTestUtils.setField(request, "recommendationEnabled", enabled);
		return request;
	}

	@Test
	void getCardPerformanceUsesTheGivenYearMonth() {
		CardPerformanceResponseDto performance = CardPerformanceResponseDto.builder()
			.userCardId(USER_CARD_ID)
			.targetYearMonth("202607")
			.build();
		when(cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202607")).thenReturn(performance);

		ResponseEntity<CardPerformanceResponseDto> response =
			controller.getCardPerformance(USER_ID, USER_CARD_ID, "202607");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(performance);
	}

	@Test
	void getCardPerformanceDefaultsToTheCurrentYearMonthWhenNotProvided() {
		when(cardService.getCardPerformance(eq(USER_ID), eq(USER_CARD_ID), anyString()))
			.thenReturn(CardPerformanceResponseDto.builder().build());

		controller.getCardPerformance(USER_ID, USER_CARD_ID, null);

		ArgumentCaptor<String> yearMonthCaptor = ArgumentCaptor.forClass(String.class);
		verify(cardService).getCardPerformance(eq(USER_ID), eq(USER_CARD_ID), yearMonthCaptor.capture());
		String expected = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
		assertThat(yearMonthCaptor.getValue()).isEqualTo(expected);
	}

	@Test
	void getCardBenefitsReturnsOkWithTheServiceResult() {
		CardBenefitResponseDto benefits = CardBenefitResponseDto.builder().userCardId(USER_CARD_ID).build();
		when(cardService.getCardBenefits(USER_ID, USER_CARD_ID)).thenReturn(benefits);

		ResponseEntity<CardBenefitResponseDto> response = controller.getCardBenefits(USER_ID, USER_CARD_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(benefits);
	}

	@Test
	void getCardDetailReturnsOkWithTheServiceResult() {
		CardDetailResponseDto detail = CardDetailResponseDto.builder().userCardId(USER_CARD_ID).build();
		when(cardService.getCardDetail(USER_ID, USER_CARD_ID)).thenReturn(detail);

		ResponseEntity<CardDetailResponseDto> response = controller.getCardDetail(USER_ID, USER_CARD_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(detail);
	}

	@Test
	void getCardListReturnsOkWithTheServiceResult() {
		List<CardListResponseDto> cards = List.of(CardListResponseDto.builder().userCardId(USER_CARD_ID).build());
		when(cardService.getCardList(USER_ID)).thenReturn(cards);

		ResponseEntity<List<CardListResponseDto>> response = controller.getCardList(USER_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(cards);
	}

	@Test
	void setRepresentativeCardReturnsOkWithTheServiceResult() {
		CardRepresentativeResponseDto representative = CardRepresentativeResponseDto.builder()
			.userCardId(USER_CARD_ID)
			.primary(true)
			.build();
		when(cardService.setRepresentativeCard(USER_ID, USER_CARD_ID)).thenReturn(representative);

		ResponseEntity<CardRepresentativeResponseDto> response =
			controller.setRepresentativeCard(USER_ID, USER_CARD_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(representative);
	}

	@Test
	void updateRecommendationPassesTheRequestedFlagToTheService() {
		CardRecommendationResponseDto updated = CardRecommendationResponseDto.builder()
			.userCardId(USER_CARD_ID)
			.recommendationEnabled(true)
			.build();
		when(cardService.updateRecommendation(USER_ID, USER_CARD_ID, true)).thenReturn(updated);

		ResponseEntity<CardRecommendationResponseDto> response =
			controller.updateRecommendation(USER_ID, USER_CARD_ID, recommendationRequest(true));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(updated);
	}
}
