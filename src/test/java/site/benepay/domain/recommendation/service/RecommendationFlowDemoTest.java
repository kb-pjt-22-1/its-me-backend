package site.benepay.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.recommendation.dto.CardBenefitScoreDto;
import site.benepay.domain.recommendation.dto.CategoryCardRecommendationResponseDto;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

/**
 * 프론트 -> 백엔드 -> 프론트로 이어지는 추천 흐름을 실제 RecommendationServiceImpl로 재현해서
 * 눈으로 확인하기 위한 시나리오 테스트다.
 *
 * <p>회원가입·카드 자동연동 이벤트, 실제 HTTP/게이트웨이 등 아직 완성되지 않은 다른 기능에는
 * 기대지 않는다 - "① 카테고리 검색 또는 지도 위치정보(bounds) -> ② 백엔드가 그 회원의 보유
 * 카드·혜택 조회 -> ③ BenefitEngine 계산 -> ④ 해당 매장/카드를 프론트로 반환"이라는 핵심
 * 파이프라인만 격리해서 검증한다. 추천 파라미터는 실제 recommendation-params.json 원본을
 * 그대로 읽어 쓴다(합성 데이터 아님) - 운영과 같은 통상결제액 기준으로 계산된다.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationFlowDemoTest {

	private static final Long USER_ID = 42L;
	// its-me-infra/mysql/init/02_seed.sql의 실제 merchant_categories 코드
	private static final String CAFE_CODE = "5813";

	@Mock
	private RecommendationMapper recommendationMapper;

	@Mock
	private MerchantService merchantService;

	@Mock
	private MerchantCategoryService merchantCategoryService;

	private RecommendationServiceImpl recommendationService;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		recommendationService = new RecommendationServiceImpl(
			recommendationMapper, merchantService, merchantCategoryService, objectMapper, realParamsLoader(objectMapper)
		);

		when(merchantCategoryService.getCategoryList()).thenReturn(List.of(
			MerchantCategoryResponseDto.builder().categoryCode(CAFE_CODE).categoryName("카페").build()
		));
	}

	/**
	 * RecommendationParamsLoader는 원래 컨테이너가 @PostConstruct로 load()를 불러 준다.
	 * 이 테스트는 컨테이너 밖이라 리플렉션으로 직접 호출한다 - 그래야 이 테스트도 운영과 똑같이
	 * classpath의 recommendation-params.json 원본을 그대로 읽는다.
	 */
	private RecommendationParamsLoader realParamsLoader(ObjectMapper objectMapper) {
		RecommendationParamsLoader loader = new RecommendationParamsLoader(objectMapper);
		try {
			Method load = RecommendationParamsLoader.class.getDeclaredMethod("load");
			load.setAccessible(true);
			load.invoke(loader);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("failed to load recommendation-params.json for the test", e);
		}
		return loader;
	}

	private RecommendationCardCandidateVO card(Long userCardId, String cardName, double discountRate) {
		RecommendationCardCandidateVO vo = new RecommendationCardCandidateVO();
		vo.setUserCardId(userCardId);
		vo.setCardId(userCardId);
		vo.setCardName(cardName);
		vo.setCardImageUrl("https://cdn.benepay.com/cards/" + userCardId + ".png");
		vo.setTotalSpendingAmount(420_000L);
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":0}]}]}",
			cardName, CAFE_CODE, discountRate
		));
		return vo;
	}

	/** 회원 42가 실제로 들고 있다고 가정한 보유 카드 3장. */
	private List<RecommendationCardCandidateVO> heldCards() {
		return List.of(
			card(1L, "청춘대로 톡톡카드", 50),
			card(2L, "굿데이카드", 10),
			card(3L, "ALL카드", 1)
		);
	}

	@Test
	void categorySearchFlow_frontendCategoryQueryToRankedCards() {
		System.out.println();
		System.out.println("[1] 프론트 -> 백엔드: 카테고리 검색 \"카페\" (GET /api/v1/recommendations/categories/카페/cards)");
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString())).thenReturn(heldCards());
		when(recommendationMapper.findYearlyBenefitUsage(anyLong(), anyInt())).thenReturn(List.of());
		System.out.println("[2] 백엔드: 회원 " + USER_ID + "의 보유 카드 " + heldCards().size() + "장 + 혜택 정보(benefits_info) 조회");

		CategoryCardRecommendationResponseDto response =
			recommendationService.getCardRecommendationsByCategory(USER_ID, "카페");

		System.out.println("[3] 백엔드: BenefitEngine.evaluateNow()(모드1)+evaluateBuild()(모드2)로 카드별 계산 -> "
			+ "모드1 상태 -> 할인율 순 정렬");
		System.out.println("    통상결제액(recommendation-params.json 실값) = " + response.getTypicalPaymentAmount() + "원");
		for (CardBenefitScoreDto c : response.getCards()) {
			System.out.printf("    - %-14s [모드1] %-6s 할인율 %5.1f%%  [모드2] %-8s 다음구간까지 %,8d원 기대값 %,6d원%n",
				c.getCardName(), c.getStatus(), c.getDiscountRate() * 100,
				c.getBuildStatus(), c.getGapAmount(), c.getExpectedValue());
		}
		System.out.println("[4] 백엔드 -> 프론트: 위 순위 그대로 응답 (프론트는 1등 카드를 강조 표시)");

		assertThat(response.getCards()).extracting(CardBenefitScoreDto::getCardName)
			.containsExactly("청춘대로 톡톡카드", "굿데이카드", "ALL카드");
		assertThat(response.getCards().get(0).getStatus()).isEqualTo("즉시할인");
		assertThat(response.getTypicalPaymentAmount()).isGreaterThan(0);
	}

	@Test
	void mapBoundsFlow_frontendLocationToFilteredMerchants() {
		System.out.println();
		System.out.println("[1] 프론트 -> 백엔드: 지도 위치정보 bounds=(37.550,126.920)~(37.570,126.940) "
			+ "(GET /api/v1/recommendations/merchants)");
		NearbyMerchantResponseDto merchant = NearbyMerchantResponseDto.builder()
			.merchantId(501L)
			.categoryCode(CAFE_CODE)
			.merchantName("카페 트리 홍대점")
			.latitude(BigDecimal.valueOf(37.556))
			.longitude(BigDecimal.valueOf(126.925))
			.build();
		when(merchantService.getMerchantsWithinBounds(37.55, 126.92, 37.57, 126.94)).thenReturn(List.of(merchant));
		when(recommendationMapper.findRecommendationCardCandidates(any(), anyString())).thenReturn(heldCards());
		when(recommendationMapper.findYearlyBenefitUsage(anyLong(), anyInt())).thenReturn(List.of());
		System.out.println("[2] 백엔드: bounds 안 매장 조회(merchants.within-bounds) + "
			+ "회원 " + USER_ID + "의 보유 카드·혜택 조회");

		List<NearbyMerchantRecommendationResponseDto> merchants =
			recommendationService.getNearbyMerchants(USER_ID, 37.55, 126.92, 37.57, 126.94);

		System.out.println("[3] 백엔드: 매장마다 보유 카드 전체를 BenefitEngine으로 평가 -> "
			+ "1등이 즉시할인인 매장만 필터링");
		for (NearbyMerchantRecommendationResponseDto m : merchants) {
			System.out.printf("    - %-16s 추천카드=%-14s %s%n",
				m.getMerchantName(), m.getRecommendedCardName(), m.getBenefitSummary());
		}
		System.out.println("[4] 백엔드 -> 프론트: 위 매장만 지도 화면 마커로 표시 (recommendedCardName 포함)");

		assertThat(merchants).hasSize(1);
		assertThat(merchants.get(0).getMerchantName()).isEqualTo("카페 트리 홍대점");
		assertThat(merchants.get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
	}
}
