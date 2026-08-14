package site.benepay.domain.benefit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.benepay.domain.benefit.dto.BenefitCoachDataDto.CardData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.MonthlyUsageData;
import site.benepay.domain.benefit.dto.BenefitCoachDataDto.PaymentData;
import site.benepay.domain.benefit.dto.BenefitCoachResponseDto;
import site.benepay.domain.benefit.dto.BenefitCoachResponseDto.BenefitCoachItemDto;
import site.benepay.domain.benefit.mapper.BenefitMapper;
import site.benepay.domain.recommendation.engine.RecommendationParamsLoader;

@ExtendWith(MockitoExtension.class)
class BenefitCoachServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long RECOMMENDED_USER_CARD_ID = 100L;
	private static final Long NEXT_USER_CARD_ID = 200L;

	private static final ZoneId ZONE =
		ZoneId.of("Asia/Seoul");

	@Mock
	private BenefitMapper benefitMapper;

	@Mock
	private RecommendationParamsLoader recommendationParamsLoader;

	@Mock
	private OpenAiClient openAiClient;

	private BenefitServiceImpl benefitService;

	@BeforeEach
	void setUp() {
		benefitService =
			new BenefitServiceImpl(
				benefitMapper,
				new ObjectMapper(),
				recommendationParamsLoader,
				openAiClient
			);

		lenient()
			.when(openAiClient.generateCoachingText(anyList()))
			.thenReturn(
				new OpenAiClient.OpenAiCoachingText(
					"이번 주 카드 사용 전략을 준비했어요.",
					List.of(
						new OpenAiClient.OpenAiCoachingItemText(
							0,
							"토요일 주유소에는",
							"추천 카드를 사용하면 추가 혜택을 기대할 수 있어요."
						)
					)
				)
			);
	}

	@Test
	void returnsEmptyResponseWhenPaymentsDoNotExist() {
		when(
			benefitMapper.findBenefitCoachingPayments(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of());

		BenefitCoachResponseDto response =
			benefitService.getBenefitCoaching(USER_ID);

		assertThat(response.getSummary())
			.isEqualTo(
				"최근 3개월 결제 내역이 없어 분석할 소비 패턴이 없습니다."
			);

		assertThat(response.getItems()).isEmpty();

		verify(openAiClient, never())
			.generateCoachingText(anyList());
	}

	@Test
	void usesBenefitGuideWhenAdditionalSavingIsLessThanThreshold() {
		stubCoachingData(
			payment(999L),
			List.of(
				card(
					RECOMMENDED_USER_CARD_ID,
					"NEED Global 카드",
					215L,
					null
				)
			),
			List.of()
		);

		BenefitCoachItemDto item = getSingleCoachingItem();

		assertThat(item.getStrategyType())
			.isEqualTo("BENEFIT_GUIDE");

		assertThat(item.getRecommendedCardName())
			.isEqualTo("NEED Global 카드");

		assertThat(item.getExpectedSavingAmount())
			.isEqualTo(215L);

		assertThat(item.getExpectedAdditionalSavingAmount())
			.isEqualTo(215L);
	}

	@Test
	void switchesNowWhenAdditionalSavingMeetsThreshold() {
		stubCoachingData(
			payment(999L),
			List.of(
				card(
					RECOMMENDED_USER_CARD_ID,
					"굿데이카드",
					500L,
					null
				)
			),
			List.of()
		);

		BenefitCoachItemDto item = getSingleCoachingItem();

		assertThat(item.getStrategyType())
			.isEqualTo("SWITCH_NOW");

		assertThat(item.getExpectedAdditionalSavingAmount())
			.isEqualTo(500L);
	}

	@Test
	void keepsUsingCardWhenUsualCardIsTheBestCard() {
		stubCoachingData(
			payment(RECOMMENDED_USER_CARD_ID),
			List.of(
				card(
					RECOMMENDED_USER_CARD_ID,
					"굿데이카드",
					700L,
					null
				)
			),
			List.of()
		);

		BenefitCoachItemDto item = getSingleCoachingItem();

		assertThat(item.getStrategyType())
			.isEqualTo("KEEP_USING");

		assertThat(item.getExpectedAdditionalSavingAmount())
			.isZero();
	}

	@Test
	void usesBenefitThenSwitchesWhenUsageCountRemains() {
		MonthlyUsageData usage = new MonthlyUsageData();

		usage.setUserCardId(RECOMMENDED_USER_CARD_ID);
		usage.setCategoryCode("5541");
		usage.setUsedBenefitAmount(BigDecimal.ZERO);
		usage.setUsageCount(2);

		stubCoachingData(
			payment(RECOMMENDED_USER_CARD_ID),
			List.of(
				card(
					RECOMMENDED_USER_CARD_ID,
					"주유 혜택 카드",
					1_000L,
					3
				),
				card(
					NEXT_USER_CARD_ID,
					"굿데이카드",
					500L,
					null
				)
			),
			List.of(usage)
		);

		BenefitCoachItemDto item = getSingleCoachingItem();

		assertThat(item.getStrategyType())
			.isEqualTo("USE_THEN_SWITCH");

		assertThat(item.getRemainingUsageCount())
			.isEqualTo(1);

		assertThat(item.getNextRecommendedCardName())
			.isEqualTo("굿데이카드");
	}

	private BenefitCoachItemDto getSingleCoachingItem() {
		BenefitCoachResponseDto response =
			benefitService.getBenefitCoaching(USER_ID);

		assertThat(response.getItems()).hasSize(1);

		verify(openAiClient)
			.generateCoachingText(anyList());

		return response.getItems().get(0);
	}

	private void stubCoachingData(
		PaymentData payment,
		List<CardData> cards,
		List<MonthlyUsageData> monthlyUsages
	) {
		when(
			benefitMapper.findBenefitCoachingPayments(
				eq(USER_ID),
				any(LocalDateTime.class),
				any(LocalDateTime.class)
			)
		).thenReturn(List.of(payment));

		when(
			benefitMapper.findBenefitCoachingCards(
				eq(USER_ID),
				anyString()
			)
		).thenReturn(cards);

		when(
			benefitMapper.findBenefitCoachingMonthlyUsages(
				eq(USER_ID),
				anyString()
			)
		).thenReturn(monthlyUsages);
	}

	private PaymentData payment(Long userCardId) {
		PaymentData payment = new PaymentData();

		payment.setPaymentId(1L);
		payment.setUserCardId(userCardId);
		payment.setCategoryCode("5541");
		payment.setCategoryName("주유소");
		payment.setMerchantName("KB 주유소");
		payment.setOriginalAmount(
			BigDecimal.valueOf(43_000L)
		);
		payment.setPaymentTime(
			LocalDateTime.now(ZONE).minusDays(1)
		);

		return payment;
	}

	private CardData card(
		Long userCardId,
		String cardName,
		Long discountAmount,
		Integer monthlyCountLimit
	) {
		CardData card = new CardData();

		card.setUserCardId(userCardId);
		card.setCardId(userCardId + 1L);
		card.setCardName(cardName);
		card.setPreviousMonthSpendingAmount(100_000L);
		card.setBenefitsInfo(
			benefitsInfo(
				discountAmount,
				monthlyCountLimit
			)
		);

		return card;
	}

	private String benefitsInfo(
		Long discountAmount,
		Integer monthlyCountLimit
	) {
		String monthlyCountLimitJson =
			monthlyCountLimit == null
				? "null"
				: monthlyCountLimit.toString();

		return String.format(
			"""
				{
				  "performanceTiers": [
				    {
				      "benefitNodeId": "TEST_TIER",
				      "tierName": "테스트 구간",
				      "minimumSpending": 0,
				      "maximumSpending": null,
				      "benefits": [
				        {
				          "serviceName": "주유 할인",
				          "benefitType": "DISCOUNT",
				          "categoryCodes": ["5541"],
				          "discountMethod": "FIXED",
				          "discountAmount": %d,
				          "minimumPaymentAmount": 0,
				          "monthlyCountLimit": %s
				        }
				      ]
				    }
				  ]
				}
				""",
			discountAmount,
			monthlyCountLimitJson
		);
	}
}
