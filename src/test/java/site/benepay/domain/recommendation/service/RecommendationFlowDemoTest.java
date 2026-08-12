package site.benepay.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

/**
 * 프론트 -> 백엔드 -> 프론트로 이어지는 추천 흐름을 실제 RecommendationServiceImpl로 재현해서
 * 눈으로 확인하기 위한 시나리오 테스트다.
 *
 * <p>"매장 도메인 -> 카드 도메인 -> 추천 도메인 순으로 위임하는 Facade({기능명}ProcessFacade)가
 * 유저 정보 + 위치 기반 매장 리스트 + 카드 도메인의 유저 보유 카드(월별 실적 이력 포함)를 모아
 * RecommendationService.recommendMerchants(...)를 직접 호출한다"는 파이프라인 자체(컨트롤러,
 * Facade)는 다른 팀원 담당이라 이 저장소에서 만들지 않는다 - 여기서는 Facade가 그 데이터를
 * 다 모아서 넘겨준 순간부터, 추천 도메인이 "① BenefitEngine.evaluatePriority()(모드 3: 결제
 * 이력·지갑 전체 여력을 함께 고려)로 매장마다 최적 카드 계산 -> ② 총 기대 가치가 0보다 큰
 * 매장만 benefitAvailable=true로 표시 -> ③ 매장을 걸러내지 않고 전달받은 매장 전부를 그대로
 * 반환"하는 부분만 격리해서 검증한다. 추천 파라미터는 실제 recommendation-params.json 원본을
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
	private MerchantCategoryService merchantCategoryService;

	private RecommendationServiceImpl recommendationService;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		recommendationService = new RecommendationServiceImpl(
			recommendationMapper, merchantCategoryService, objectMapper, realParamsLoader(objectMapper)
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
		vo.setSpendHistory(Map.of("202507", 420_000L));
		vo.setCurrentMonthSpend(0L);
		vo.setBenefitsInfo(String.format(
			"{\"performanceTiers\":[{\"minimumSpending\":0,\"benefits\":["
				+ "{\"serviceName\":\"%s\",\"benefitType\":\"MERCHANT_CATEGORY\","
				+ "\"categoryCodes\":[\"%s\"],\"discountMethod\":\"STATEMENT_DISCOUNT\","
				+ "\"discountRate\":%s,\"minimumPaymentAmount\":0}]}]}",
			cardName, CAFE_CODE, discountRate
		));
		return vo;
	}

	/** 회원 42가 실제로 들고 있다고(카드 도메인이 Facade를 통해 전달했다고) 가정한 보유 카드 3장. */
	private List<RecommendationCardCandidateVO> heldCards() {
		return List.of(
			card(1L, "청춘대로 톡톡카드", 50),
			card(2L, "굿데이카드", 10),
			card(3L, "ALL카드", 1)
		);
	}

	@Test
	void facadeDelegatedFlow_userAndMerchantAndCardDataToFrontendReadyResult() {
		System.out.println();
		System.out.println("[1] (가정) 프론트 -> 컨트롤러 -> {추천}ProcessFacade.processPipeline(request): "
			+ "매장 도메인이 위치 기반 매장 리스트를 조회하고, 카드 도메인이 회원 " + USER_ID
			+ "의 보유 카드 + 실적 정보를 붙여 추천 도메인에 한 번에 넘긴다.");
		MerchantResponseDto cafeMerchant = MerchantResponseDto.builder()
			.merchantId(501L)
			.categoryCode(CAFE_CODE)
			.merchantName("카페 트리 홍대점")
			.latitude(BigDecimal.valueOf(37.556))
			.longitude(BigDecimal.valueOf(126.925))
			.build();
		MerchantResponseDto otherCategoryMerchant = MerchantResponseDto.builder()
			.merchantId(502L)
			.categoryCode("9999")
			.merchantName("올리브영 홍대점")
			.latitude(BigDecimal.valueOf(37.557))
			.longitude(BigDecimal.valueOf(126.926))
			.build();
		System.out.println("[2] 추천 도메인: Facade가 넘긴 유저 보유 카드 " + heldCards().size()
			+ "장(월별 실적 이력 포함) + 매장 2개를 그대로 받는다 (직접 DB/다른 서비스 조회 없음)");

		List<NearbyMerchantRecommendationResponseDto> result = recommendationService.recommendMerchants(
			USER_ID, heldCards(), List.of(cafeMerchant, otherCategoryMerchant)
		);

		System.out.println("[3] 추천 도메인: 매장마다 보유 카드 전체를 BenefitEngine.evaluatePriority()(모드 3)로 평가 -> "
			+ "최적 카드의 총 기대 가치(이번 달 확정 + 다음 달 기대)가 0보다 크면 benefitAvailable=true로 표시 "
			+ "(매장은 걸러내지 않고 전부 반환)");
		for (NearbyMerchantRecommendationResponseDto m : result) {
			System.out.printf("    - %-16s benefitAvailable=%-5s 추천카드=%-14s %s%n",
				m.getMerchantName(), m.isBenefitAvailable(), m.getRecommendedCardName(), m.getBenefitSummary());
		}
		System.out.println("[4] 추천 도메인 -> Facade -> 프론트: 매장 리스트(추가 필드 benefitAvailable 포함)를 그대로 반환");

		assertThat(result).hasSize(2);
		assertThat(result).filteredOn(m -> m.getMerchantId().equals(501L))
			.allSatisfy(m -> {
				assertThat(m.getMerchantName()).isEqualTo("카페 트리 홍대점");
				assertThat(m.isBenefitAvailable()).isTrue();
				assertThat(m.getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
				assertThat(m.getCategoryCode()).isEqualTo(CAFE_CODE);
			});
		assertThat(result).filteredOn(m -> m.getMerchantId().equals(502L))
			.allSatisfy(m -> {
				assertThat(m.isBenefitAvailable()).isFalse();
				assertThat(m.getRecommendedCardName()).isNull();
			});
	}
}
