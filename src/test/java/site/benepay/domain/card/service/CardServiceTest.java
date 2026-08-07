package site.benepay.domain.card.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.card.dto.CardBenefitResponseDto;
import site.benepay.domain.card.dto.CardDetailResponseDto;
import site.benepay.domain.card.dto.CardListResponseDto;
import site.benepay.domain.card.dto.CardPerformanceResponseDto;
import site.benepay.domain.card.dto.CardRecommendationResponseDto;
import site.benepay.domain.card.dto.CardRepresentativeResponseDto;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.card.vo.UserCardBenefitVO;
import site.benepay.domain.card.vo.UserCardDetailVO;
import site.benepay.domain.card.vo.UserCardListVO;
import site.benepay.domain.card.vo.UserCardPerformanceVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long USER_CARD_ID = 100L;

	@Mock
	private CardMapper cardMapper;

	private CardService cardService;

	@BeforeEach
	void setUp() {
		cardService = new CardService(cardMapper, new ObjectMapper());
	}

	private UserCardPerformanceVO performanceVO(Long current, Long required) {
		UserCardPerformanceVO vo = new UserCardPerformanceVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setCardId(1L);
		vo.setCardName("노리 체크카드");
		vo.setTargetYearMonth("202608");
		vo.setCurrentSpendingAmount(current);
		vo.setRequiredSpendingAmount(required);
		return vo;
	}

	// ---- getCardPerformance ----

	@Test
	void getCardPerformanceComputesRemainingAmountAndAchievementRate() {
		when(cardMapper.findPerformanceByUserCardId(USER_ID, USER_CARD_ID, "202608"))
			.thenReturn(Optional.of(performanceVO(5000L, 10000L)));

		CardPerformanceResponseDto response = cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202608");

		assertThat(response.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(response.getCurrentSpendingAmount()).isEqualTo(5000L);
		assertThat(response.getRequiredSpendingAmount()).isEqualTo(10000L);
		assertThat(response.getRemainingAmount()).isEqualTo(5000L);
		assertThat(response.getAchievementRate()).isEqualTo(50.0);
		assertThat(response.getPerformanceMet()).isFalse();
	}

	@Test
	void getCardPerformanceCapsAchievementRateAt100AndRemainingAtZeroWhenExceeded() {
		when(cardMapper.findPerformanceByUserCardId(USER_ID, USER_CARD_ID, "202608"))
			.thenReturn(Optional.of(performanceVO(12000L, 10000L)));

		CardPerformanceResponseDto response = cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202608");

		assertThat(response.getRemainingAmount()).isEqualTo(0L);
		assertThat(response.getAchievementRate()).isEqualTo(100.0);
		assertThat(response.getPerformanceMet()).isTrue();
	}

	@Test
	void getCardPerformanceTreatsZeroRequiredAmountAsAlreadyMet() {
		when(cardMapper.findPerformanceByUserCardId(USER_ID, USER_CARD_ID, "202608"))
			.thenReturn(Optional.of(performanceVO(0L, 0L)));

		CardPerformanceResponseDto response = cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202608");

		assertThat(response.getAchievementRate()).isEqualTo(100.0);
		assertThat(response.getPerformanceMet()).isTrue();
		assertThat(response.getRemainingAmount()).isEqualTo(0L);
	}

	@Test
	void getCardPerformanceTreatsNullSpendingAmountsAsZero() {
		when(cardMapper.findPerformanceByUserCardId(USER_ID, USER_CARD_ID, "202608"))
			.thenReturn(Optional.of(performanceVO(null, null)));

		CardPerformanceResponseDto response = cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202608");

		assertThat(response.getCurrentSpendingAmount()).isEqualTo(0L);
		assertThat(response.getRequiredSpendingAmount()).isEqualTo(0L);
	}

	@Test
	void getCardPerformanceRoundsAchievementRateToOneDecimalPlace() {
		when(cardMapper.findPerformanceByUserCardId(USER_ID, USER_CARD_ID, "202608"))
			.thenReturn(Optional.of(performanceVO(1L, 3L)));

		CardPerformanceResponseDto response = cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202608");

		assertThat(response.getAchievementRate()).isEqualTo(33.3);
	}

	@Test
	void getCardPerformanceThrowsWhenTheCardCannotBeFound() {
		when(cardMapper.findPerformanceByUserCardId(USER_ID, USER_CARD_ID, "202608"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardService.getCardPerformance(USER_ID, USER_CARD_ID, "202608"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("보유 카드를 찾을 수 없습니다.");
	}

	@Test
	void getCardPerformanceThrowsWhenYearMonthIsNotInYyyyMmFormat() {
		assertThatThrownBy(() -> cardService.getCardPerformance(USER_ID, USER_CARD_ID, "2026-08"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("yearMonth는 YYYYMM 형식이어야 합니다.");
	}

	// ---- getCardBenefits ----

	@Test
	void getCardBenefitsParsesTheStoredJsonIntoAJsonNode() {
		UserCardBenefitVO vo = new UserCardBenefitVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setCardId(1L);
		vo.setCardName("노리 체크카드");
		vo.setMinBenefitAmount(300000L);
		vo.setBenefitsInfo("{\"tier\":\"1\"}");
		when(cardMapper.findBenefitsByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.of(vo));

		CardBenefitResponseDto response = cardService.getCardBenefits(USER_ID, USER_CARD_ID);

		assertThat(response.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(response.getMinBenefitAmount()).isEqualTo(300000L);
		assertThat(response.getBenefits().get("tier").asText()).isEqualTo("1");
	}

	@Test
	void getCardBenefitsReturnsAnEmptyObjectWhenBenefitsInfoIsNull() {
		UserCardBenefitVO vo = new UserCardBenefitVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setBenefitsInfo(null);
		when(cardMapper.findBenefitsByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.of(vo));

		CardBenefitResponseDto response = cardService.getCardBenefits(USER_ID, USER_CARD_ID);

		assertThat(response.getBenefits().isEmpty()).isTrue();
	}

	@Test
	void getCardBenefitsReturnsAnEmptyObjectWhenBenefitsInfoIsBlank() {
		UserCardBenefitVO vo = new UserCardBenefitVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setBenefitsInfo("   ");
		when(cardMapper.findBenefitsByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.of(vo));

		CardBenefitResponseDto response = cardService.getCardBenefits(USER_ID, USER_CARD_ID);

		assertThat(response.getBenefits().isEmpty()).isTrue();
	}

	@Test
	void getCardBenefitsThrowsWhenTheStoredJsonIsMalformed() {
		UserCardBenefitVO vo = new UserCardBenefitVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setBenefitsInfo("not-json");
		when(cardMapper.findBenefitsByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.of(vo));

		assertThatThrownBy(() -> cardService.getCardBenefits(USER_ID, USER_CARD_ID))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("카드 혜택 JSON 형식이 올바르지 않습니다.");
	}

	@Test
	void getCardBenefitsThrowsWhenTheCardCannotBeFound() {
		when(cardMapper.findBenefitsByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardService.getCardBenefits(USER_ID, USER_CARD_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("보유 카드를 찾을 수 없습니다.");
	}

	// ---- getCardDetail ----

	@Test
	void getCardDetailMapsEveryFieldAndMasksThePanLast4() {
		UserCardDetailVO vo = new UserCardDetailVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setCardId(1L);
		vo.setCardName("노리 체크카드");
		vo.setCardType("CHECK");
		vo.setCardImageUrl("https://example.com/card.png");
		vo.setDescription("설명");
		vo.setCardNetwork("VISA");
		vo.setAnnualFee(0L);
		vo.setPanLast4("1234");
		vo.setStatus("ACTIVE");
		vo.setPrimaryCard(true);
		vo.setRecommendationEnabled(true);
		vo.setSupported(true);
		vo.setMinBenefitAmount(300000L);
		when(cardMapper.findDetailByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.of(vo));

		CardDetailResponseDto response = cardService.getCardDetail(USER_ID, USER_CARD_ID);

		assertThat(response.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(response.getCardName()).isEqualTo("노리 체크카드");
		assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 1234");
		assertThat(response.getPrimary()).isTrue();
		assertThat(response.getSupported()).isTrue();
		assertThat(response.getMinBenefitAmount()).isEqualTo(300000L);
	}

	@Test
	void getCardDetailReturnsNullMaskedCardNumberWhenPanLast4IsMissing() {
		UserCardDetailVO vo = new UserCardDetailVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setPanLast4(null);
		when(cardMapper.findDetailByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.of(vo));

		CardDetailResponseDto response = cardService.getCardDetail(USER_ID, USER_CARD_ID);

		assertThat(response.getMaskedCardNumber()).isNull();
	}

	@Test
	void getCardDetailThrowsWhenTheCardCannotBeFound() {
		when(cardMapper.findDetailByUserCardId(USER_ID, USER_CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardService.getCardDetail(USER_ID, USER_CARD_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("카드를 찾을 수 없습니다.");
	}

	// ---- getCardList ----

	@Test
	void getCardListMapsEveryCardForTheUser() {
		UserCardListVO vo = new UserCardListVO();
		vo.setUserCardId(USER_CARD_ID);
		vo.setCardId(1L);
		vo.setCardName("노리 체크카드");
		vo.setPanLast4("1234");
		vo.setPrimaryCard(true);
		vo.setRecommendationEnabled(false);
		when(cardMapper.findAllByUserId(USER_ID)).thenReturn(List.of(vo));

		List<CardListResponseDto> cards = cardService.getCardList(USER_ID);

		assertThat(cards).hasSize(1);
		assertThat(cards.get(0).getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(cards.get(0).getPanLast4()).isEqualTo("1234");
		assertThat(cards.get(0).getPrimary()).isTrue();
		assertThat(cards.get(0).getRecommendationEnabled()).isFalse();
	}

	@Test
	void getCardListReturnsAnEmptyListWhenTheUserHasNoCards() {
		when(cardMapper.findAllByUserId(USER_ID)).thenReturn(List.of());

		assertThat(cardService.getCardList(USER_ID)).isEmpty();
	}

	// ---- setRepresentativeCard ----

	@Test
	void setRepresentativeCardClearsThenSetsTheNewPrimaryCard() {
		when(cardMapper.existsActiveUserCard(USER_ID, USER_CARD_ID)).thenReturn(true);
		when(cardMapper.setPrimaryCard(USER_ID, USER_CARD_ID)).thenReturn(1);

		CardRepresentativeResponseDto response = cardService.setRepresentativeCard(USER_ID, USER_CARD_ID);

		assertThat(response.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(response.getPrimary()).isTrue();
		verify(cardMapper).clearPrimaryCard(USER_ID);
		verify(cardMapper).setPrimaryCard(USER_ID, USER_CARD_ID);
	}

	@Test
	void setRepresentativeCardThrowsWhenTheCardIsNotActivelyOwned() {
		when(cardMapper.existsActiveUserCard(USER_ID, USER_CARD_ID)).thenReturn(false);

		assertThatThrownBy(() -> cardService.setRepresentativeCard(USER_ID, USER_CARD_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("정상 사용 가능한 보유 카드를 찾을 수 없습니다.");
	}

	@Test
	void setRepresentativeCardThrowsWhenTheUpdateAffectsNoRows() {
		when(cardMapper.existsActiveUserCard(USER_ID, USER_CARD_ID)).thenReturn(true);
		when(cardMapper.setPrimaryCard(USER_ID, USER_CARD_ID)).thenReturn(0);

		assertThatThrownBy(() -> cardService.setRepresentativeCard(USER_ID, USER_CARD_ID))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("대표카드 설정에 실패했습니다.");
	}

	// ---- updateRecommendation ----

	@Test
	void updateRecommendationUpdatesTheFlagForTheOwnedCard() {
		when(cardMapper.existsActiveUserCard(USER_ID, USER_CARD_ID)).thenReturn(true);
		when(cardMapper.updateRecommendationEnabled(USER_ID, USER_CARD_ID, true)).thenReturn(1);

		CardRecommendationResponseDto response = cardService.updateRecommendation(USER_ID, USER_CARD_ID, true);

		assertThat(response.getUserCardId()).isEqualTo(USER_CARD_ID);
		assertThat(response.getRecommendationEnabled()).isTrue();
	}

	@Test
	void updateRecommendationThrowsWhenTheCardIsNotActivelyOwned() {
		when(cardMapper.existsActiveUserCard(USER_ID, USER_CARD_ID)).thenReturn(false);

		assertThatThrownBy(() -> cardService.updateRecommendation(USER_ID, USER_CARD_ID, true))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("정상 사용 가능한 보유 카드를 찾을 수 없습니다.");
	}

	@Test
	void updateRecommendationThrowsWhenTheUpdateAffectsNoRows() {
		when(cardMapper.existsActiveUserCard(USER_ID, USER_CARD_ID)).thenReturn(true);
		when(cardMapper.updateRecommendationEnabled(USER_ID, USER_CARD_ID, false)).thenReturn(0);

		assertThatThrownBy(() -> cardService.updateRecommendation(USER_ID, USER_CARD_ID, false))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("카드 추천 설정 변경에 실패했습니다.");
	}
}
