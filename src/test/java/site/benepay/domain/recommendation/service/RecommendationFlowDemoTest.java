package site.benepay.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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

import site.benepay.common.event.MerchantRecommendationRequestedEvent;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantCategoryService;
import site.benepay.domain.recommendation.dto.NearbyMerchantRecommendationResponseDto;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;
import site.benepay.domain.recommendation.listener.MerchantRecommendationEventListener;
import site.benepay.domain.recommendation.mapper.RecommendationMapper;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

/**
 * 프론트 -> 백엔드 -> 프론트로 이어지는 추천 흐름을 실제 RecommendationServiceImpl +
 * MerchantRecommendationEventListener로 재현해서 눈으로 확인하기 위한 시나리오 테스트다.
 *
 * <p>"매장 도메인이 (유저 아이디 + 위치정보) 진입점에서 카드 도메인의 유저 보유 카드와 자신이
 * 조회한 위치 기반 매장 리스트를 모아 이벤트로 발행한다"는 것은 이 저장소 밖(다른 도메인)의
 * 일이라고 가정한다 - 여기서는 그 이벤트가 이미 만들어졌다고 보고, 추천 도메인의 리스너가 그
 * 이벤트를 받아서 "① BenefitEngine으로 매장마다 최적 카드 계산 -> ② 그 카드가 지금 당장
 * 혜택을 주는 매장만 필터링 -> ③ 결과를 이벤트에 채워 넣어(발행자가 이어서 프론트로 반환)"
 * 처리하는 부분만 격리해서 검증한다. 추천 파라미터는 실제 recommendation-params.json 원본을
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

	private MerchantRecommendationEventListener listener;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		RecommendationServiceImpl recommendationService = new RecommendationServiceImpl(
			recommendationMapper, merchantCategoryService, objectMapper, realParamsLoader(objectMapper)
		);
		listener = new MerchantRecommendationEventListener(recommendationService);

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

	/** 회원 42가 실제로 들고 있다고(카드 도메인 이벤트로 전달받았다고) 가정한 보유 카드 3장. */
	private List<RecommendationCardCandidateVO> heldCards() {
		return List.of(
			card(1L, "청춘대로 톡톡카드", 50),
			card(2L, "굿데이카드", 10),
			card(3L, "ALL카드", 1)
		);
	}

	@Test
	void merchantRecommendationRequestedFlow_eventDataToFrontendReadyResult() {
		System.out.println();
		System.out.println("[1] (가정) 프론트 -> 매장 도메인: 유저 아이디 + 위치정보. 매장 도메인이 카드 도메인에게 "
			+ "회원 " + USER_ID + "의 보유 카드를 받고, 자신은 위치 기반 매장 리스트를 조회한 뒤 "
			+ "MerchantRecommendationRequestedEvent를 발행한다.");
		NearbyMerchantResponseDto cafeMerchant = NearbyMerchantResponseDto.builder()
			.merchantId(501L)
			.categoryCode(CAFE_CODE)
			.merchantName("카페 트리 홍대점")
			.latitude(BigDecimal.valueOf(37.556))
			.longitude(BigDecimal.valueOf(126.925))
			.build();
		NearbyMerchantResponseDto otherCategoryMerchant = NearbyMerchantResponseDto.builder()
			.merchantId(502L)
			.categoryCode("9999")
			.merchantName("올리브영 홍대점")
			.latitude(BigDecimal.valueOf(37.557))
			.longitude(BigDecimal.valueOf(126.926))
			.build();
		when(recommendationMapper.findYearlyBenefitUsage(anyLong(), anyInt())).thenReturn(List.of());

		MerchantRecommendationRequestedEvent event = new MerchantRecommendationRequestedEvent(
			USER_ID, heldCards(), List.of(cafeMerchant, otherCategoryMerchant)
		);

		System.out.println("[2] 추천 도메인 리스너: 이벤트에서 유저 보유 카드 " + heldCards().size()
			+ "장 + 매장 " + event.getMerchants().size() + "개를 받는다 (직접 DB/다른 서비스 조회 없음)");

		listener.handleMerchantRecommendationRequested(event);

		System.out.println("[3] 리스너 -> RecommendationService.recommendMerchants: 매장마다 보유 카드 전체를 "
			+ "BenefitEngine.evaluateNow()로 평가 -> 최적 카드가 즉시할인인 매장만 채택");
		for (NearbyMerchantRecommendationResponseDto m : event.getResult()) {
			System.out.printf("    - %-16s 추천카드=%-14s %s%n",
				m.getMerchantName(), m.getRecommendedCardName(), m.getBenefitSummary());
		}
		System.out.println("[4] 리스너: 계산 결과를 이벤트에 채워 넣는다 (event.completeWith) -> "
			+ "발행한 매장 도메인이 이어서 프론트로 반환");

		assertThat(event.getResult()).hasSize(1);
		assertThat(event.getResult().get(0).getMerchantName()).isEqualTo("카페 트리 홍대점");
		assertThat(event.getResult().get(0).getRecommendedCardName()).isEqualTo("청춘대로 톡톡카드");
		assertThat(event.getResult().get(0).getCategoryName()).isEqualTo("카페");
	}
}
