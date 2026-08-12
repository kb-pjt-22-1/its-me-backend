package site.benepay.domain.recommendation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.RecommendationParams;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 100L;
	private static final String CAFE_CODE = "5311";
	private static final String CONVENIENCE_CODE = "5411";
	private static final Map<String, Long> SOME_PAST_HISTORY = Map.of("202501", 100_000L);

	@Mock
	private RecommendationMapper recommendationMapper;

	@Mock
	private MerchantCategoryService merchantCategoryService;

	@Mock
	private RecommendationParamsLoader recommendationParamsLoader;

	private RecommendationServiceImpl recommendationService;

	@BeforeEach
	void setUp() {
		recommendationService = new RecommendationServiceImpl(
			recommendationMapper, merchantCategoryService, new ObjectMapper(), recommendationParamsLoader
		);
	}

	private static RecommendationParams paramsWithTypicalAmounts(Map<String, Long> typicalAmounts) {
		return new RecommendationParams(
			typicalAmounts,
			new RecommendationParams.TicketHistogram(new double[0], Map.of()),
			Map.of(),
			testConstants()
		);
	}

	// CsvProcessing/recommendation_params.json의 실제 값과 동일.
	private static RecommendationParams.Constants testConstants() {
		return new RecommendationParams.Constants(1700.0, 0.35, 2.0, 0.25, 0.15, 0.05, 0.95, 0.2);
	}

	/** 단일 구간(다음 구간 없음) · 정률 혜택 하나짜리 카드. 다음 구간이 없어 future=0, total=now다. */
	private static RecommendationCardCandidateVO candidate(Long userCardId, String cardName, String categoryCode,
		double rate) {
		return candidate(userCardId, cardName, categoryCode, rate, SOME_PAST_HISTORY, 0L);
	}

	private static RecommendationCardCandidateVO candidate(
		Long userCardId, String cardName, String categoryCode, double rate,
		Map<String, Long> spendHistory, long currentMonthSpend
	) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":0}]}]}",
			cardName, categoryCode, rate
		));
		vo.setSpendHistory(spendHistory);
		vo.setCurrentMonthSpend(currentMonthSpend);
		return vo;
	}

	private static MerchantResponseDto merchant(Long merchantId, String categoryCode) {
		return MerchantResponseDto.builder()
			.merchantId(merchantId)
			.categoryCode(categoryCode)
			.merchantName("스타벅스 강남점")
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();
	}

	private void stubCafeCategory() {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(CAFE_CODE).categoryName("카페").build()
		));
	}

	private void stubCafeAndConvenienceCategories() {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(CAFE_CODE).categoryName("카페").build(),
			MerchantCategoryResponseDto.builder().categoryCode(CONVENIENCE_CODE).categoryName("편의점").build()
		));
	}

	@Test
	void includesMerchantWhenTheBestCardHasPositivePriorityValue() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidate(1L, "청춘대로 톡톡카드", CAFE_CODE, 50)),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(result.get(0).getCategoryCode()).isEqualTo(CAFE_CODE);
		assertThat(result.get(0).isBenefitAvailable()).isTrue();
		assertThat(result.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
		assertThat(result.get(0).getBenefitSummary()).isNotBlank();
		assertThat(result.get(0).getDistanceMeters()).isNull();
	}

	@Test
	void picksTheHighestTotalValueCardAmongMultipleHeldCards() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(
				candidate(1L, "굿데이카드", CAFE_CODE, 10),
				candidate(2L, "청춘대로 톡톡카드", CAFE_CODE, 50),
				candidate(3L, "ALL카드", CAFE_CODE, 1)
			),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
	}

	@Test
	void marksMerchantUnavailableWhenNoCardHasAnyBenefitForTheCategory() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidate(1L, "전혀다른카드", "9999", 50)),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		assertThat(result.get(0).getRecommendedCardName()).isNull();
		assertThat(result.get(0).getBenefitSummary()).isNull();
	}

	@Test
	void marksMerchantUnavailableWhenCategoryIsNotAnAnalyzedMajorCategory() {
		stubCafeCategory();

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidate(1L, "청춘대로 톡톡카드", CAFE_CODE, 50)),
			List.of(merchant(MERCHANT_ID, "8888"))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		assertThat(result.get(0).getCategoryCode()).isEqualTo("8888");
		assertThat(result.get(0).getRecommendedCardName()).isNull();
	}

	@Test
	void evaluatesEachMerchantAgainstItsOwnCategorySoOtherCategoriesAreNotHidden() {
		// 검색한 카테고리 밖 매장은 보이지 않던 지난 설계의 문제가 해소됐는지 검증한다 - 카페
		// 카드 하나, 편의점 카드 하나를 들고 있을 때 두 카테고리 매장이 섞여 있으면 둘 다
		// 각자의 카테고리 기준으로 평가되어 결과에 포함돼야 한다.
		stubCafeAndConvenienceCategories();
		when(recommendationParamsLoader.params()).thenReturn(
			paramsWithTypicalAmounts(Map.of("카페", 10_000L, "편의점", 15_000L))
		);

		Long cafeMerchantId = 501L;
		Long convenienceMerchantId = 502L;

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(
				candidate(1L, "카페카드", CAFE_CODE, 50),
				candidate(2L, "편의점카드", CONVENIENCE_CODE, 30)
			),
			List.of(
				merchant(cafeMerchantId, CAFE_CODE),
				merchant(convenienceMerchantId, CONVENIENCE_CODE)
			)
		);

		assertThat(result).extracting(NearbyMerchantRecommendationResponseDto::getMerchantId)
			.containsExactlyInAnyOrder(cafeMerchantId, convenienceMerchantId);
		assertThat(result).filteredOn(m -> m.getMerchantId().equals(cafeMerchantId))
			.allSatisfy(m -> assertThat(m.isBenefitAvailable()).isTrue())
			.extracting(NearbyMerchantRecommendationResponseDto::getRecommendedCardName)
			.containsExactly("카페카드");
		assertThat(result).filteredOn(m -> m.getMerchantId().equals(convenienceMerchantId))
			.allSatisfy(m -> assertThat(m.isBenefitAvailable()).isTrue())
			.extracting(NearbyMerchantRecommendationResponseDto::getRecommendedCardName)
			.containsExactly("편의점카드");
	}

	@Test
	void returnsEmptyListWhenNoMerchantsAreGiven() {
		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidate(1L, "청춘대로 톡톡카드", CAFE_CODE, 50)),
			List.of()
		);

		assertThat(result).isEmpty();
	}

	@Test
	void marksMerchantUnavailableWhenUserHasNoHeldCards() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		assertThat(result.get(0).getRecommendedCardName()).isNull();
	}

	@Test
	void throwsWhenUserIdIsNull() {
		assertThatThrownBy(() -> recommendationService.recommendMerchants(null, List.of(), List.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("로그인 사용자 정보가 필요합니다.");
	}

	@Test
	void includesMerchantsWithNullCoordinatesWithoutFailing() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));
		MerchantResponseDto merchantWithoutCoordinates = MerchantResponseDto.builder()
			.merchantId(MERCHANT_ID)
			.categoryCode(CAFE_CODE)
			.merchantName("좌표 없는 매장")
			.build();

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidate(1L, "청춘대로 톡톡카드", CAFE_CODE, 50)),
			List.of(merchantWithoutCoordinates)
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLatitude()).isNull();
		assertThat(result.get(0).getLongitude()).isNull();
	}

	@Test
	void treatsHeldCardWithNullSpendDataAsNoHistory() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));
		RecommendationCardCandidateVO candidateWithoutSpendData =
			candidate(1L, "청춘대로 톡톡카드", CAFE_CODE, 50, null, 0L);
		candidateWithoutSpendData.setCurrentMonthSpend(null);

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidateWithoutSpendData),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		// spendHistory/currentMonthSpend가 null이어도 0으로 취급해 예외 없이 단일 구간 카드의
		// 즉시 혜택(전월 실적 0원이라도 0구간은 충족)을 정상적으로 계산해야 한다.
		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isTrue();
		assertThat(result.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
	}

	@Test
	void aggregatesWalletSpendHistorySkippingCardsWithNullHistoryAndNullMonthValues() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		RecommendationCardCandidateVO cardWithoutHistory = candidate(1L, "이력없는카드", CAFE_CODE, 5, null, 0L);
		Map<String, Long> historyWithNullValue = new HashMap<>();
		historyWithNullValue.put("202501", null);
		historyWithNullValue.put("202502", 100_000L);
		RecommendationCardCandidateVO cardWithNullMonthValue =
			candidate(2L, "청춘대로 톡톡카드", CAFE_CODE, 50, historyWithNullValue, 0L);

		// 지갑 합산 로직(aggregateWalletSpendHistory)이 null 이력·null 월값을 만나도 예외 없이
		// 0원으로 처리하고 나머지 카드를 정상 평가하는지 검증한다.
		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(cardWithoutHistory, cardWithNullMonthValue),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
	}

	@Test
	void categoryNameLookupKeepsTheFirstEntryWhenCategoryCodesAreDuplicated() {
		// typicalPaymentAmount 맵에는 "카페"만 있고 "카페(중복)"은 없으므로, 룩업이 첫 항목("카페")을
		// 쓰지 않고 두 번째 항목을 썼다면 typicalAmount를 못 찾아 benefitAvailable=false가 된다.
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(CAFE_CODE).categoryName("카페").build(),
			MerchantCategoryResponseDto.builder().categoryCode(CAFE_CODE).categoryName("카페(중복)").build()
		));
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidate(1L, "청춘대로 톡톡카드", CAFE_CODE, 50)),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isTrue();
		assertThat(result.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
	}

	@Test
	void getCardRecommendationsThrowsWhenMerchantDoesNotExist() {
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(null);

		assertThatThrownBy(() -> recommendationService.getCardRecommendations(USER_ID, MERCHANT_ID))
			.isInstanceOf(MerchantNotFoundException.class)
			.hasMessage("존재하지 않는 매장입니다.");
	}

	@Test
	void getCardRecommendationsReturnsMerchantInfoWithEmptyCardsWhenMerchantExists() {
		RecommendationMerchantVO merchant = new RecommendationMerchantVO();
		merchant.setMerchantId(MERCHANT_ID);
		merchant.setMerchantName("스타벅스 강남점");
		merchant.setCategoryCode(CAFE_CODE);
		merchant.setBrandId(9L);
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(merchant);

		MerchantCardRecommendationResponseDto response =
			recommendationService.getCardRecommendations(USER_ID, MERCHANT_ID);

		assertThat(response.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(response.getMerchantName()).isEqualTo("스타벅스 강남점");
		assertThat(response.getCategoryCode()).isEqualTo(CAFE_CODE);
		assertThat(response.getBrandId()).isEqualTo(9L);
		// 다른 팀원의 추천 알고리즘이 아직 연결되지 않아 카드 비교 결과는 항상 빈 목록이다.
		assertThat(response.getCards()).isEmpty();
	}

	@Test
	void getCardRecommendationsThrowsWhenUserIdIsNull() {
		assertThatThrownBy(() -> recommendationService.getCardRecommendations(null, MERCHANT_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("로그인 사용자 정보가 필요합니다.");
	}
}
