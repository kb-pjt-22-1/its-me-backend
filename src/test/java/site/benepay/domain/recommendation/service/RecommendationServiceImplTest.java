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
import site.benepay.domain.recommendation.dto.CardBenefitComparisonResponseDto;
import site.benepay.domain.recommendation.dto.MerchantCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.RecommendedCardResponseDto;
import site.benepay.domain.recommendation.dto.TodayCardRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.RecommendationParams;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationBenefitUsageVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;
import site.benepay.domain.recommendation.vo.RecommendationMerchantVO;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 100L;
	private static final String CAFE_CODE = "5311";
	private static final String CONVENIENCE_CODE = "5411";
	private static final String RESTAURANT_CODE = "5812";
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
		return candidate(userCardId, cardName, categoryCode, rate, spendHistory, currentMonthSpend, 0L);
	}

	private static RecommendationCardCandidateVO candidate(
		Long userCardId, String cardName, String categoryCode, double rate,
		Map<String, Long> spendHistory, long currentMonthSpend, long minimumPaymentAmount
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
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":%d}]}]}",
			cardName, categoryCode, rate, minimumPaymentAmount
		));
		vo.setSpendHistory(spendHistory);
		vo.setCurrentMonthSpend(currentMonthSpend);
		return vo;
	}

	/**
	 * 단일 구간 · 건당 정액 할인 카드. count=usable/ticket=qualifying(테스트 환경에서는 항상
	 * 1.0)이라 now = discountAmount로 ticket 크기와 무관하게 고정된다 - 그래서 정률 카드와
	 * 짝지으면 "기준 결제액이 얼마로 정해지느냐"에 따라 순위가 뒤집히는지 확인할 수 있다.
	 */
	private static RecommendationCardCandidateVO flatDiscountCandidate(
		Long userCardId, String cardName, String categoryCode, long discountAmount, long minimumPaymentAmount
	) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"CASHBACK\","
				+ "\"discountAmount\":%d,\"minimumPaymentAmount\":%d}]}]}",
			cardName, categoryCode, discountAmount, minimumPaymentAmount
		));
		vo.setSpendHistory(SOME_PAST_HISTORY);
		vo.setCurrentMonthSpend(0L);
		return vo;
	}

	private static MerchantResponseDto merchant(Long merchantId, String categoryCode) {
		return merchant(merchantId, categoryCode, "스타벅스 강남점");
	}

	private static MerchantResponseDto merchant(Long merchantId, String categoryCode, String merchantName) {
		return MerchantResponseDto.builder()
			.merchantId(merchantId)
			.categoryCode(categoryCode)
			.merchantName(merchantName)
			.latitude(BigDecimal.valueOf(37.5))
			.longitude(BigDecimal.valueOf(127.0))
			.build();
	}

	/** MERCHANT_BRAND 혜택 하나짜리 카드 - 특정 매장 이름에서만 적용된다(예: "아웃백 10% 할인"). */
	private static RecommendationCardCandidateVO merchantLimitedCandidate(
		Long userCardId, String cardName, String categoryCode, String merchantName, double rate
	) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_BRAND\","
				+ "\"categoryCodes\":[\"%s\"],\"merchantNames\":[\"%s\"],"
				+ "\"discountMethod\":\"STATEMENT_DISCOUNT\",\"discountRate\":%s,\"minimumPaymentAmount\":0}]}]}",
			cardName, categoryCode, merchantName, rate
		));
		vo.setSpendHistory(SOME_PAST_HISTORY);
		vo.setCurrentMonthSpend(0L);
		return vo;
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
		assertThat(result.get(0).getRecommendedCards()).hasSize(1);
		assertThat(result.get(0).getRecommendedCards().get(0).getCardName()).isEqualTo("청춘대로 톡톡카드");
		assertThat(result.get(0).getRecommendedCards().get(0).getBenefitSummary()).isNotBlank();
		assertThat(result.get(0).getDistanceMeters()).isNull();
		assertThat(result.get(0).getTypicalPaymentAmount()).isEqualTo(10_000L);
	}

	@Test
	void ranksHeldCardsByTotalValueUpToTopThree() {
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
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("청춘대로 톡톡카드", "굿데이카드", "ALL카드");
	}

	@Test
	void limitsRecommendedCardsToTopThreeWhenMoreThanThreeCardsQualify() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(
				candidate(1L, "1위카드", CAFE_CODE, 90),
				candidate(2L, "2위카드", CAFE_CODE, 70),
				candidate(3L, "3위카드", CAFE_CODE, 50),
				candidate(4L, "4위카드", CAFE_CODE, 30)
			),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("1위카드", "2위카드", "3위카드");
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
		assertThat(result.get(0).getRecommendedCards()).isEmpty();
		assertThat(result.get(0).getTypicalPaymentAmount()).isNull();
	}

	// ---- MERCHANT_BRAND(매장 한정) 혜택 - 실제 버그 재현 ----
	// "직장인 보너스 체크카드"의 아웃백 10% 할인이 같은 업종(음식점)의 다른 매장에도
	// benefitAvailable=true로 잘못 뜨던 문제.

	@Test
	void marksMerchantUnavailableWhenMerchantLimitedBenefitDoesNotMatchThisMerchant() {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(RESTAURANT_CODE).categoryName("음식점").build()
		));
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("음식점", 30_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(merchantLimitedCandidate(1L, "직장인 보너스 체크카드", RESTAURANT_CODE, "아웃백", 10)),
			List.of(merchant(MERCHANT_ID, RESTAURANT_CODE, "맥도날드"))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		assertThat(result.get(0).getRecommendedCards()).isEmpty();
	}

	@Test
	void includesMerchantLimitedBenefitWhenMerchantNameMatches() {
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(RESTAURANT_CODE).categoryName("음식점").build()
		));
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("음식점", 30_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(merchantLimitedCandidate(1L, "직장인 보너스 체크카드", RESTAURANT_CODE, "아웃백", 10)),
			List.of(merchant(MERCHANT_ID, RESTAURANT_CODE, "아웃백 강남점"))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isTrue();
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("직장인 보너스 체크카드");
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
		assertThat(result.get(0).getRecommendedCards()).isEmpty();
		assertThat(result.get(0).getTypicalPaymentAmount()).isNull();
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
			.allSatisfy(m -> {
				assertThat(m.isBenefitAvailable()).isTrue();
				assertThat(m.getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
					.containsExactly("카페카드");
				assertThat(m.getTypicalPaymentAmount()).isEqualTo(10_000L);
			});
		assertThat(result).filteredOn(m -> m.getMerchantId().equals(convenienceMerchantId))
			.allSatisfy(m -> {
				assertThat(m.isBenefitAvailable()).isTrue();
				assertThat(m.getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
					.containsExactly("편의점카드");
				assertThat(m.getTypicalPaymentAmount()).isEqualTo(15_000L);
			});
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
		// 비교할 카드가 없으면 기준 결제액 계산을 위해 params()를 조회할 필요가 없다.

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		assertThat(result.get(0).getRecommendedCards()).isEmpty();
		assertThat(result.get(0).getTypicalPaymentAmount()).isNull();
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
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("청춘대로 톡톡카드");
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
		// 이력없는카드(5%)도 total>0이라 함께 뽑히지만, 청춘대로 톡톡카드(50%)가 더 높아 먼저 온다.
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("청춘대로 톡톡카드", "이력없는카드");
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
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("청춘대로 톡톡카드");
		assertThat(result.get(0).getTypicalPaymentAmount()).isEqualTo(10_000L);
	}

	@Test
	void usesTheHighestMinimumPaymentAmountAmongCandidateCardsAsTheTicket() {
		// typicalPaymentAmount("카페")=10,000원인 채로 두면(비교 대상 카드에 최소결제금액이
		// 없을 때만 쓰는 값), 정률카드(1%)는 100원, 정액카드(300원)는 300원이라 정액카드가
		// 이긴다. 정액카드의 최소결제금액(50,000원)이 기준액으로 채택되면 정률카드가
		// 50,000*1%=500원으로 역전해야 한다 - 두 결과가 다르므로 어느 로직이 쓰였는지 구분된다.
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(
				candidate(1L, "정률카드", CAFE_CODE, 1, SOME_PAST_HISTORY, 0L, 0L),
				flatDiscountCandidate(2L, "정액카드", CAFE_CODE, 300L, 50_000L)
			),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("정률카드", "정액카드");
		// 정액카드의 최소결제금액(50,000원)이 두 카드 중 더 크므로 그 값이 기준액으로 채택돼야 한다.
		assertThat(result.get(0).getTypicalPaymentAmount()).isEqualTo(50_000L);
	}

	@Test
	void fallsBackToRoundedTypicalPaymentAmountWhenNoCardHasAMinimumPaymentAmount() {
		// 두 카드 모두 최소결제금액이 0이라 통상결제액(8,700원)을 천원 단위로 반올림한
		// 9,000원이 기준액이 돼야 한다. 정률카드(3%)는 반올림 전 8,700원 기준이면
		// 261원으로 정액카드(265원)에 지고, 반올림 후 9,000원 기준이면 270원으로 이긴다 -
		// 두 결과가 다르므로 반올림이 실제로 적용됐는지 구분된다.
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 8_700L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(
				candidate(1L, "정률카드", CAFE_CODE, 3, SOME_PAST_HISTORY, 0L, 0L),
				flatDiscountCandidate(2L, "정액카드", CAFE_CODE, 265L, 0L)
			),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("정률카드", "정액카드");
		// 최소결제금액을 가진 카드가 없으므로 통상결제액(8,700원)을 천원 단위로 반올림한 값이 채택돼야 한다.
		assertThat(result.get(0).getTypicalPaymentAmount()).isEqualTo(9_000L);
	}

	private static RecommendationCardCandidateVO candidateWithMonthlyDiscountLimit(
		Long userCardId, String cardName, String categoryCode, double rate, long monthlyDiscountLimit
	) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		// BenefitJsonParser는 monthlyDiscountLimit을 "maximumDiscountAmountPerMonth" 키로 읽는다
		// (BenefitNode 필드명과 실제 JSON 키가 다름 - PaymentTokenServiceImplTest와 동일 주의사항).
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":0,\"maximumDiscountAmountPerMonth\":%d}]}]}",
			cardName, categoryCode, rate, monthlyDiscountLimit
		));
		vo.setSpendHistory(SOME_PAST_HISTORY);
		vo.setCurrentMonthSpend(0L);
		return vo;
	}

	private RecommendationBenefitUsageVO usageRow(Long userCardId, String benefitServiceName, long usedAmount,
		int usedCount) {
		RecommendationBenefitUsageVO row = new RecommendationBenefitUsageVO();
		row.setUserCardId(userCardId);
		row.setBenefitServiceName(benefitServiceName);
		row.setUsedAmount(usedAmount);
		row.setUsedCount(usedCount);
		return row;
	}

	@Test
	void recommendsTheCardWhenItsMonthlyDiscountLimitHasNotBeenTouchedYet() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));
		// 이번 달 소진액 조회에 아무것도 없으면(unstubbed -> 빈 리스트) 한도가 그대로 남아있다고
		// 계산돼야 한다: 10,000 * 50% = 5,000원 > 0이라 추천된다.

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidateWithMonthlyDiscountLimit(1L, "청춘대로 톡톡카드", CAFE_CODE, 50, 5_000L)),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isTrue();
		assertThat(result.get(0).getRecommendedCards()).extracting(RecommendedCardResponseDto::getCardName)
			.containsExactly("청춘대로 톡톡카드");
	}

	@Test
	void excludesTheCardWhenItsMonthlyDiscountLimitIsAlreadyFullyConsumed() {
		// 앞 테스트와 완전히 같은 카드(월 한도 5,000원, 정률 50%)지만, 이번 달 이미 5,000원을
		// 전부 소진했다고 card_benefit_monthly_usage에 집계돼 있으면 now=0이 되어 total>0
		// 필터에 걸려 추천 대상에서 빠져야 한다 - 한도 임박 시 실질 할인율을 반영 못 하던
		// 문제(추천 엔진이 매번 한도가 가득 남아있다고 가정하던 문제)의 회귀 테스트.
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));
		when(recommendationMapper.findMonthlyUsageByUserId(eq(USER_ID), any()))
			.thenReturn(List.of(usageRow(1L, "청춘대로 톡톡카드", 5_000L, 1)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidateWithMonthlyDiscountLimit(1L, "청춘대로 톡톡카드", CAFE_CODE, 50, 5_000L)),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
		assertThat(result.get(0).getRecommendedCards()).isEmpty();
	}

	// 기본 구간(0원)엔 혜택이 없고, 다음 구간(30만원)에만 혜택이 있는 카드 - 실제 시드 데이터의
	// 카드 70%가 이런 구조다(BenefitEngine 회귀 조사 참고).
	private static RecommendationCardCandidateVO candidateWithBenefitOnlyOnNextTier(
		Long userCardId, String cardName, String categoryCode, double rate, long nextTierMinimumSpending
	) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://example.com/" + userCardId + ".png");
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":["
				+ "{\"minimumSpending\":0,\"benefits\":[]},"
				+ "{\"minimumSpending\":%d,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":0}]}]}",
			nextTierMinimumSpending, cardName, categoryCode, rate
		));
		vo.setSpendHistory(SOME_PAST_HISTORY);
		vo.setCurrentMonthSpend(0L); // 다음 구간 문턱(30만원)에 한참 못 미침 - now=0이어야 한다.
		return vo;
	}

	@Test
	void marksMerchantUnavailableWhenTheOnlyBenefitIsOnAFutureTierNotReachedYet() {
		// 지도 핀 "혜택 매장" 표시와 매장 상세 "혜택 없음"이 어긋나던 버그의 회귀 테스트.
		// total(이번 달 확정 now + 다음 달 기대 future)이 0보다 크다는 이유만으로
		// benefitAvailable=true가 되면 안 된다 - 지금 당장(now) 받을 수 있는 혜택이 없으면
		// 카드 자체는 추천 후보(recommendedCards)에 남더라도 배지는 꺼져 있어야 한다.
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID,
			List.of(candidateWithBenefitOnlyOnNextTier(1L, "다음구간카드", CAFE_CODE, 50, 300_000L)),
			List.of(merchant(MERCHANT_ID, CAFE_CODE))
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isBenefitAvailable()).isFalse();
	}

	@Test
	void getCardRecommendationsThrowsWhenMerchantDoesNotExist() {
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(null);

		assertThatThrownBy(() -> recommendationService.getCardRecommendations(USER_ID, MERCHANT_ID, List.of()))
			.isInstanceOf(MerchantNotFoundException.class)
			.hasMessage("존재하지 않는 매장입니다.");
	}

	@Test
	void getCardRecommendationsReturnsMerchantInfoWithEmptyCardsWhenUserHoldsNoCards() {
		RecommendationMerchantVO merchant = new RecommendationMerchantVO();
		merchant.setMerchantId(MERCHANT_ID);
		merchant.setMerchantName("스타벅스 강남점");
		merchant.setCategoryCode(CAFE_CODE);
		merchant.setBrandId(9L);
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(merchant);

		MerchantCardRecommendationResponseDto response =
			recommendationService.getCardRecommendations(USER_ID, MERCHANT_ID, List.of());

		assertThat(response.getMerchantId()).isEqualTo(MERCHANT_ID);
		assertThat(response.getMerchantName()).isEqualTo("스타벅스 강남점");
		assertThat(response.getCategoryCode()).isEqualTo(CAFE_CODE);
		assertThat(response.getBrandId()).isEqualTo(9L);
		assertThat(response.getCards()).isEmpty();
	}

	@Test
	void getCardRecommendationsThrowsWhenUserIdIsNull() {
		assertThatThrownBy(() -> recommendationService.getCardRecommendations(null, MERCHANT_ID, List.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("로그인 사용자 정보가 필요합니다.");
	}

	@Test
	void getCardRecommendationsMarksTheHighestTotalCardAsRecommendedAndKeepsEveryHeldCard() {
		RecommendationMerchantVO merchant = new RecommendationMerchantVO();
		merchant.setMerchantId(MERCHANT_ID);
		merchant.setMerchantName("스타벅스 강남점");
		merchant.setCategoryCode(CAFE_CODE);
		merchant.setBrandId(9L);
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(merchant);
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		MerchantCardRecommendationResponseDto response = recommendationService.getCardRecommendations(
			USER_ID,
			MERCHANT_ID,
			List.of(
				candidate(1L, "낮은할인카드", CAFE_CODE, 10),
				candidate(2L, "높은할인카드", CAFE_CODE, 50)
			)
		);

		assertThat(response.getCards()).extracting(CardBenefitComparisonResponseDto::getCardName)
			.containsExactlyInAnyOrder("낮은할인카드", "높은할인카드");
		assertThat(response.getCards()).allMatch(CardBenefitComparisonResponseDto::isBenefitApplicable);
		assertThat(response.getCards()).allMatch(CardBenefitComparisonResponseDto::isPerformanceMet);

		CardBenefitComparisonResponseDto best = response.getCards().stream()
			.filter(c -> c.getCardName().equals("높은할인카드")).findFirst().orElseThrow();
		CardBenefitComparisonResponseDto worst = response.getCards().stream()
			.filter(c -> c.getCardName().equals("낮은할인카드")).findFirst().orElseThrow();

		assertThat(best.isRecommended()).isTrue();
		assertThat(worst.isRecommended()).isFalse();
	}

	@Test
	void getCardRecommendationsMarksCardsWithNoBenefitForTheCategoryAsNotApplicableWithAReason() {
		RecommendationMerchantVO merchant = new RecommendationMerchantVO();
		merchant.setMerchantId(MERCHANT_ID);
		merchant.setMerchantName("스타벅스 강남점");
		merchant.setCategoryCode(CAFE_CODE);
		merchant.setBrandId(9L);
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(merchant);
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		MerchantCardRecommendationResponseDto response = recommendationService.getCardRecommendations(
			USER_ID,
			MERCHANT_ID,
			List.of(candidate(1L, "전혀다른카드", "9999", 50))
		);

		assertThat(response.getCards()).hasSize(1);
		CardBenefitComparisonResponseDto card = response.getCards().get(0);
		assertThat(card.isBenefitApplicable()).isFalse();
		assertThat(card.isPerformanceMet()).isFalse();
		assertThat(card.isRecommended()).isFalse();
		assertThat(card.getReason()).isNotBlank();
	}

	@Test
	void getCardRecommendationsMarksMerchantLimitedBenefitAsNotApplicableAtADifferentMerchant() {
		// 매장 상세 "이 매장 추천 카드" 화면에서도 같은 버그가 재현됐다 - 아웃백 한정 혜택이
		// 다른 음식점 상세에서도 benefitApplicable=true로 잘못 떴다.
		RecommendationMerchantVO merchant = new RecommendationMerchantVO();
		merchant.setMerchantId(MERCHANT_ID);
		merchant.setMerchantName("맥도날드");
		merchant.setCategoryCode(RESTAURANT_CODE);
		merchant.setBrandId(9L);
		when(recommendationMapper.findMerchantForRecommendation(MERCHANT_ID)).thenReturn(merchant);
		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(RESTAURANT_CODE).categoryName("음식점").build()
		));
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("음식점", 30_000L)));

		MerchantCardRecommendationResponseDto response = recommendationService.getCardRecommendations(
			USER_ID,
			MERCHANT_ID,
			List.of(merchantLimitedCandidate(1L, "직장인 보너스 체크카드", RESTAURANT_CODE, "아웃백", 10))
		);

		assertThat(response.getCards()).hasSize(1);
		CardBenefitComparisonResponseDto card = response.getCards().get(0);
		assertThat(card.isBenefitApplicable()).isFalse();
		assertThat(card.isRecommended()).isFalse();
	}

	// ---- getTodayCardRecommendation(userId, heldCards, nearbyMerchantCandidates) ----

	@Test
	void getTodayCardRecommendationPicksTheCardCategoryComboWithTheHighestTotalAcrossTheWallet() {
		stubCafeAndConvenienceCategories();
		when(recommendationParamsLoader.params())
			.thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L, "편의점", 10_000L)));

		RecommendationCardCandidateVO cafeCard = candidate(1L, "카페카드", CAFE_CODE, 10);
		RecommendationCardCandidateVO convenienceCard = candidate(2L, "편의점카드", CONVENIENCE_CODE, 90);

		TodayCardRecommendationResponseDto response = recommendationService.getTodayCardRecommendation(
			USER_ID,
			List.of(cafeCard, convenienceCard),
			List.of(merchant(10L, CONVENIENCE_CODE))
		);

		assertThat(response.getUserCardId()).isEqualTo(2L);
		assertThat(response.getCardName()).isEqualTo("편의점카드");
		assertThat(response.getCategoryName()).isEqualTo("편의점");
		assertThat(response.getBenefitLabel()).isNotBlank();
		assertThat(response.getNearbyMerchants())
			.extracting(TodayCardRecommendationResponseDto.NearbyMerchant::getMerchantId)
			.containsExactly(10L);
	}

	@Test
	void getTodayCardRecommendationOnlyIncludesNearbyMerchantsTheWinningCardActuallyBenefits() {
		stubCafeAndConvenienceCategories();
		when(recommendationParamsLoader.params())
			.thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L, "편의점", 10_000L)));

		RecommendationCardCandidateVO cafeOnlyCard = candidate(1L, "카페전용카드", CAFE_CODE, 50);

		TodayCardRecommendationResponseDto response = recommendationService.getTodayCardRecommendation(
			USER_ID,
			List.of(cafeOnlyCard),
			List.of(
				merchant(1L, CONVENIENCE_CODE),
				merchant(2L, CAFE_CODE)
			)
		);

		assertThat(response.getUserCardId()).isEqualTo(1L);
		assertThat(response.getNearbyMerchants())
			.extracting(TodayCardRecommendationResponseDto.NearbyMerchant::getMerchantId)
			.containsExactly(2L);
	}

	@Test
	void getTodayCardRecommendationSortsNearbyMerchantsByDistanceAndCapsAtTwo() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		RecommendationCardCandidateVO cafeCard = candidate(1L, "카페카드", CAFE_CODE, 50);
		MerchantResponseDto far = MerchantResponseDto.builder()
			.merchantId(1L).categoryCode(CAFE_CODE).merchantName("먼 카페").distanceMeters(500L).build();
		MerchantResponseDto near = MerchantResponseDto.builder()
			.merchantId(2L).categoryCode(CAFE_CODE).merchantName("가까운 카페").distanceMeters(50L).build();
		MerchantResponseDto mid = MerchantResponseDto.builder()
			.merchantId(3L).categoryCode(CAFE_CODE).merchantName("중간 카페").distanceMeters(150L).build();

		TodayCardRecommendationResponseDto response = recommendationService.getTodayCardRecommendation(
			USER_ID, List.of(cafeCard), List.of(far, near, mid)
		);

		assertThat(response.getNearbyMerchants())
			.extracting(TodayCardRecommendationResponseDto.NearbyMerchant::getMerchantId)
			.containsExactly(2L, 3L);
	}

	@Test
	void getTodayCardRecommendationStillReturnsTheCardWhenNoNearbyMerchantMatches() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		RecommendationCardCandidateVO cafeCard = candidate(1L, "카페카드", CAFE_CODE, 50);

		TodayCardRecommendationResponseDto response =
			recommendationService.getTodayCardRecommendation(USER_ID, List.of(cafeCard), List.of());

		assertThat(response.getUserCardId()).isEqualTo(1L);
		assertThat(response.getCardName()).isEqualTo("카페카드");
		assertThat(response.getNearbyMerchants()).isEmpty();
	}

	@Test
	void getTodayCardRecommendationReturnsEmptyWhenHeldCardsIsEmpty() {
		TodayCardRecommendationResponseDto response =
			recommendationService.getTodayCardRecommendation(USER_ID, List.of(), List.of());

		assertThat(response.getUserCardId()).isNull();
		assertThat(response.getCardName()).isNull();
		assertThat(response.getNearbyMerchants()).isEmpty();
	}

	@Test
	void getTodayCardRecommendationReturnsEmptyWhenNoCardBenefitsAnyAnalyzedCategory() {
		stubCafeCategory();
		when(recommendationParamsLoader.params()).thenReturn(paramsWithTypicalAmounts(Map.of("카페", 10_000L)));

		RecommendationCardCandidateVO offCategoryCard = candidate(1L, "전혀다른카드", "9999", 50);

		TodayCardRecommendationResponseDto response =
			recommendationService.getTodayCardRecommendation(USER_ID, List.of(offCategoryCard), List.of());

		assertThat(response.getUserCardId()).isNull();
		assertThat(response.getNearbyMerchants()).isEmpty();
	}
}
