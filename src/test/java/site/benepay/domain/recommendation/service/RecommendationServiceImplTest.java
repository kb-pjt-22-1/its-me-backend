package site.benepay.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.CategoryNotFoundException;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.CardBenefitScoreDto;
import site.benepay.domain.recommendation.dto.CategoryCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.BenefitStatus;
import site.benepay.domain.recommendation.engine.RecommendationParams;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.BenefitUsageVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 100L;
	private static final String CAFE_CODE = "5311";

	@Mock
	private RecommendationMapper recommendationMapper;

	@Mock
	private MerchantService merchantService;

	@Mock
	private MerchantCategoryService merchantCategoryService;

	@Mock
	private RecommendationParamsLoader recommendationParamsLoader;

	private RecommendationServiceImpl recommendationService;

	@BeforeEach
	void setUp() {
		recommendationService = new RecommendationServiceImpl(
			recommendationMapper, merchantService, merchantCategoryService, new ObjectMapper(), recommendationParamsLoader
		);
	}

	private static RecommendationParams paramsWithTypicalAmount(long typicalAmount) {
		return new RecommendationParams(
			Map.of("카페", typicalAmount),
			new RecommendationParams.TicketHistogram(new double[0], Map.of()),
			new RecommendationParams.Constants(1700.0)
		);
	}

	private static RecommendationCardCandidateVO candidate(Long userCardId, String cardName, double rate) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		vo.setTotalSpendingAmount(500_000L);
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":0}]}]}",
			cardName, CAFE_CODE, rate
		));
		return vo;
	}

	private static RecommendationCardCandidateVO candidateWithMinPayment(
		Long userCardId, String cardName, double rate, long minimumPaymentAmount
	) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		vo.setTotalSpendingAmount(500_000L);
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":%d}]}]}",
			cardName, CAFE_CODE, rate, minimumPaymentAmount
		));
		return vo;
	}

	private static NearbyMerchantResponseDto merchant(Long merchantId, String categoryCode) {
		return NearbyMerchantResponseDto.builder()
			.merchantId(merchantId)
			.categoryCode(categoryCode)
			.merchantName("스타벅스 강남점")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();
	}

	private void stubNoUsage() {
		when(recommendationMapper.findYearlyBenefitUsage(anyLong(), anyInt())).thenReturn(List.<BenefitUsageVO>of());
	}

	private void stubCafeCategory() {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(CAFE_CODE).categoryName("카페").build()
		));
	}

	@Test
	void ranksCardsByStatusThenByDiscountRateDescending() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmount(10_000L));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString())).thenReturn(List.of(
			candidate(1L, "굿데이카드", 10),
			candidate(2L, "청춘대로 톡톡카드", 50),
			candidate(3L, "ALL카드", 1)
		));
		stubNoUsage();

		CategoryCardRecommendationResponseDto response =
			recommendationService.getCardRecommendationsByCategory(USER_ID, "카페");

		assertThat(response.getCategoryName()).isEqualTo("카페");
		assertThat(response.getTypicalPaymentAmount()).isEqualTo(10_000L);
		assertThat(response.getCards()).extracting(CardBenefitScoreDto::getCardName)
			.containsExactly("청춘대로 톡톡카드", "굿데이카드", "ALL카드");
		assertThat(response.getCards()).extracting(CardBenefitScoreDto::getStatus)
			.containsOnly(BenefitStatus.IMMEDIATE_DISCOUNT.label());
		assertThat(response.getCards().get(0).getDiscountRate()).isCloseTo(0.50, Offset.offset(1e-9));
	}

	@Test
	void throwsWhenCategoryNameDoesNotExist() {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of());

		assertThatThrownBy(() -> recommendationService.getCardRecommendationsByCategory(USER_ID, "존재하지않음"))
			.isInstanceOf(CategoryNotFoundException.class);
	}

	@Test
	void throwsWhenCategoryHasNoTypicalPaymentAmount() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(
			new RecommendationParams(Map.of(), new RecommendationParams.TicketHistogram(new double[0], Map.of()),
				new RecommendationParams.Constants(1700.0))
		);

		assertThatThrownBy(() -> recommendationService.getCardRecommendationsByCategory(USER_ID, "카페"))
			.isInstanceOf(CategoryNotFoundException.class)
			.hasMessageContaining("추천 분석 대상 대분류가 아닙니다");
	}

	@Test
	void throwsWhenUserIdIsNull() {
		assertThatThrownBy(() -> recommendationService.getCardRecommendationsByCategory(null, "카페"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("로그인 사용자 정보가 필요합니다.");
	}

	@Test
	void returnsEmptyCardListWhenUserHasNoCandidateCards() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmount(10_000L));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString())).thenReturn(List.of());

		CategoryCardRecommendationResponseDto response =
			recommendationService.getCardRecommendationsByCategory(USER_ID, "카페");

		assertThat(response.getCards()).isEmpty();
	}

	// ---- getNearbyMerchants ----

	@Test
	void includesMerchantWhenTheBestCardGivesAnImmediateDiscountNow() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmount(10_000L));
		when(merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1))
			.thenReturn(List.of(merchant(MERCHANT_ID, CAFE_CODE)));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString()))
			.thenReturn(List.of(candidate(1L, "청춘대로 톡톡카드", 50)));
		stubNoUsage();

		List<NearbyMerchantRecommendationResponseDto> result =
			recommendationService.getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.get(0).getCategoryName()).isEqualTo("카페");
		assertThat(result.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
		assertThat(result.get(0).getBenefitSummary()).isNotBlank();
		assertThat(result.get(0).getDistanceMeters()).isNull();
	}

	@Test
	void excludesMerchantWhenTheBestCardOnlyOffersAConditionalDiscount() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmount(10_000L));
		when(merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1))
			.thenReturn(List.of(merchant(MERCHANT_ID, CAFE_CODE)));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString()))
			.thenReturn(List.of(candidateWithMinPayment(1L, "청춘대로 톡톡카드", 50, 50_000L)));
		stubNoUsage();

		List<NearbyMerchantRecommendationResponseDto> result =
			recommendationService.getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1);

		assertThat(result).isEmpty();
	}

	@Test
	void excludesMerchantWhoseCategoryIsNotAnAnalyzedMajorCategory() {
		stubCafeCategory();
		when(merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1))
			.thenReturn(List.of(merchant(MERCHANT_ID, "9999")));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString()))
			.thenReturn(List.of(candidate(1L, "청춘대로 톡톡카드", 50)));

		List<NearbyMerchantRecommendationResponseDto> result =
			recommendationService.getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyListWhenNoMerchantsAreWithinBounds() {
		when(merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1)).thenReturn(List.of());

		List<NearbyMerchantRecommendationResponseDto> result =
			recommendationService.getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyListWhenUserHasNoCandidateCardsForNearbyMerchants() {
		when(merchantService.getMerchantsWithinBounds(37.4, 126.9, 37.6, 127.1))
			.thenReturn(List.of(merchant(MERCHANT_ID, CAFE_CODE)));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString())).thenReturn(List.of());

		List<NearbyMerchantRecommendationResponseDto> result =
			recommendationService.getNearbyMerchants(USER_ID, 37.4, 126.9, 37.6, 127.1);

		assertThat(result).isEmpty();
	}

	@Test
	void throwsWhenUserIdIsNullForNearbyMerchants() {
		assertThatThrownBy(() -> recommendationService.getNearbyMerchants(null, 37.4, 126.9, 37.6, 127.1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("로그인 사용자 정보가 필요합니다.");
	}
}
