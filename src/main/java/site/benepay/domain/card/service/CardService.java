package site.benepay.domain.card.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.dto.CardBenefitResponseDto;
import site.benepay.domain.card.dto.CardDetailResponseDto;
import site.benepay.domain.card.dto.CardListResponseDto;
import site.benepay.domain.card.dto.CardPerformanceResponseDto;
import site.benepay.domain.card.dto.CardRecommendationResponseDto;
import site.benepay.domain.card.dto.CardRepresentativeResponseDto;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.card.vo.CardMonthlyStatusVO;
import site.benepay.domain.card.vo.UserCardBenefitVO;
import site.benepay.domain.card.vo.UserCardDetailVO;
import site.benepay.domain.card.vo.UserCardListVO;
import site.benepay.domain.card.vo.UserCardPerformanceVO;
import site.benepay.domain.card.vo.UserCardRecommendationVO;
import site.benepay.domain.recommendation.vo.RecommendationCardCandidateVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

	// DB 커넥션의 serverTimezone(application.properties의 db.url)과 맞춘다 - UserServiceImpl과 동일 규약.
	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

	private final CardMapper cardMapper;
	private final ObjectMapper objectMapper;

	public CardPerformanceResponseDto getCardPerformance(Long userId, Long userCardId, String yearMonth) {
		validateYearMonth(yearMonth);

		UserCardPerformanceVO performance =
			cardMapper.findPerformanceByUserCardId(
				userId,
				userCardId,
				yearMonth
			).orElseThrow(() ->
				new IllegalArgumentException(
					"보유 카드를 찾을 수 없습니다."
				)
			);

		long currentAmount =
			performance.getCurrentSpendingAmount() == null ? 0L : performance.getCurrentSpendingAmount();

		long requiredAmount =
			performance.getRequiredSpendingAmount() == null ? 0L : performance.getRequiredSpendingAmount();

		long remainingAmount = Math.max(requiredAmount - currentAmount, 0L);

		boolean performanceMet = requiredAmount == 0L || currentAmount >= requiredAmount;

		double achievementRate = calculateAchievementRate(
			currentAmount,
			requiredAmount
		);

		return CardPerformanceResponseDto.builder()
			.userCardId(performance.getUserCardId())
			.cardId(performance.getCardId())
			.cardName(performance.getCardName())
			.targetYearMonth(performance.getTargetYearMonth())
			.currentSpendingAmount(currentAmount)
			.requiredSpendingAmount(requiredAmount)
			.remainingAmount(remainingAmount)
			.achievementRate(achievementRate)
			.performanceMet(performanceMet)
			.build();
	}

	//달성률 상한선 100%로 설정
	private double calculateAchievementRate(long currentAmount, long requiredAmount) {
		if (requiredAmount == 0L) {
			return 100.0;
		}

		double rate = (double)currentAmount / requiredAmount * 100;
		double roundedRate = Math.round(rate * 10.0) / 10.0;

		return Math.min(roundedRate, 100.0);
	}

	private void validateYearMonth(String yearMonth) {
		try {
			YearMonth.parse(
				yearMonth,
				DateTimeFormatter.ofPattern("yyyyMM")
			);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException(
				"yearMonth는 YYYYMM 형식이어야 합니다."
			);
		}
	}

	public CardBenefitResponseDto getCardBenefits(Long userId, Long userCardId) {
		UserCardBenefitVO benefit = cardMapper.findBenefitsByUserCardId(userId, userCardId)
			.orElseThrow(() -> new IllegalArgumentException("보유 카드를 찾을 수 없습니다."));
		JsonNode benefitsJson = parseBenefitsInfo(benefit.getBenefitsInfo());

		return CardBenefitResponseDto.builder()
			.userCardId(benefit.getUserCardId())
			.cardId(benefit.getCardId())
			.cardName(benefit.getCardName())
			.minBenefitAmount(benefit.getMinBenefitAmount())
			.benefits(benefitsJson)
			.build();
	}

	private JsonNode parseBenefitsInfo(String benefitsInfo) {
		if (benefitsInfo == null || benefitsInfo.isBlank()) {
			return objectMapper.createObjectNode();
		}
		try {
			JsonNode root = objectMapper.readTree(benefitsInfo);
			fillDiscountRateForPointAccumulation(root);
			return root;
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("카드 혜택 JSON 형식이 올바르지 않습니다.", e);
		}
	}

	/**
	 * 프론트는 혜택 배지를 discountRate/discountAmount 기준으로 렌더링하는데(둘 다 없으면
	 * description 원문이 그대로 노출됨), 적립형(POINT_ACCUMULATION) 혜택은 원본 데이터에
	 * discountRate 대신 rewardRate로 저장돼 있어 배지가 깨진다. 적립도 결제금액 대비
	 * 비율이라는 계산 의미는 할인과 같으므로(BenefitJsonParser와 동일 취급), discountRate가
	 * 없을 때만 rewardRate 값을 그대로 채워 넣는다 - 원본 rewardRate 필드는 남겨 둔다.
	 */
	private void fillDiscountRateForPointAccumulation(JsonNode root) {
		for (JsonNode tier : root.path("performanceTiers")) {
			for (JsonNode benefit : tier.path("benefits")) {
				if (!(benefit instanceof ObjectNode benefitObject)) {
					continue;
				}
				if (!"POINT_ACCUMULATION".equals(benefitObject.path("discountMethod").asText())) {
					continue;
				}
				if (benefitObject.has("discountRate") || !benefitObject.has("rewardRate")) {
					continue;
				}
				benefitObject.set("discountRate", benefitObject.get("rewardRate"));
			}
		}
	}

	public CardDetailResponseDto getCardDetail(Long userId, Long userCardId) {
		UserCardDetailVO card = cardMapper.findDetailByUserCardId(userId, userCardId)
			.orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다."));

		return CardDetailResponseDto.builder()
			.userCardId(card.getUserCardId())
			.cardId(card.getCardId())
			.cardName(card.getCardName())
			.cardType(card.getCardType())
			.cardImageUrl(card.getCardImageUrl())
			.description(card.getDescription())
			.cardNetwork(card.getCardNetwork())
			.annualFee(card.getAnnualFee())
			.maskedCardNumber(maskCardNumber(card.getPanLast4()))
			.tokenExpiryDate(card.getTokenExpiryDate())
			.status(card.getStatus())
			.primary(card.getPrimaryCard())
			.recommendationEnabled(card.getRecommendationEnabled())
			.supported(card.getSupported())
			.minBenefitAmount(card.getMinBenefitAmount())
			.build();
	}

	public List<CardListResponseDto> getCardList(Long userId) {
		List<UserCardListVO> cardList = cardMapper.findAllByUserId(userId);

		return cardList.stream()
			.map(this::toCardListResponseDto)
			.collect(Collectors.toList());
	}

	private CardListResponseDto toCardListResponseDto(UserCardListVO userCard) {
		return CardListResponseDto.builder()
			.userCardId(userCard.getUserCardId())
			.cardId(userCard.getCardId())
			.cardName(userCard.getCardName())
			.cardType(userCard.getCardType())
			.cardImageUrl(userCard.getCardImageUrl())
			.cardNetwork(userCard.getCardNetwork())
			.annualFee(userCard.getAnnualFee())
			.panLast4(userCard.getPanLast4())
			.status(userCard.getStatus())
			.primary(userCard.getPrimaryCard())
			.recommendationEnabled(
				userCard.getRecommendationEnabled()
			)
			.build();
	}

	private String maskCardNumber(String panLast4) {
		if (panLast4 == null || panLast4.isBlank()) {
			return null;
		}
		return "**** **** **** " + panLast4;
	}

	@Transactional
	public CardRepresentativeResponseDto setRepresentativeCard(Long userId, Long userCardId) {

		validateActiveOwnedCard(userId, userCardId);

		// 기존 대표카드 해제
		cardMapper.clearPrimaryCard(userId);

		// 선택한 카드 대표카드 설정
		int updatedCount =
			cardMapper.setPrimaryCard(userId, userCardId);

		if (updatedCount != 1) {
			throw new IllegalStateException(
				"대표카드 설정에 실패했습니다."
			);
		}

		return CardRepresentativeResponseDto.builder()
			.userCardId(userCardId)
			.primary(true)
			.build();
	}

	@Transactional
	public CardRecommendationResponseDto updateRecommendation(Long userId, Long userCardId,
		Boolean recommendationEnabled) {

		validateActiveOwnedCard(userId, userCardId);

		int updatedCount = cardMapper.updateRecommendationEnabled(userId, userCardId, recommendationEnabled);

		if (updatedCount != 1) {
			throw new IllegalStateException(
				"카드 추천 설정 변경에 실패했습니다."
			);
		}

		return CardRecommendationResponseDto.builder()
			.userCardId(userCardId)
			.recommendationEnabled(recommendationEnabled)
			.build();
	}

	private void validateActiveOwnedCard(
		Long userId,
		Long userCardId
	) {
		boolean exists =
			cardMapper.existsActiveUserCard(userId, userCardId);

		if (!exists) {
			throw new IllegalArgumentException(
				"정상 사용 가능한 보유 카드를 찾을 수 없습니다."
			);
		}
	}

	/**
	 * 추천 도메인에서 사용할 사용자의 보유 카드 정보를 조회한다.
	 *
	 * 추천이 활성화된 ACTIVE 카드만 대상으로 하며,
	 * 각 카드에 혜택 JSON과 월별 실적 정보를 함께 구성한다.
	 *
	 * 과거 완료 월 실적은 spendHistory에,
	 * 현재 월 누적 실적은 currentMonthSpend에 분리하여 전달한다.
	 */
	public List<RecommendationCardCandidateVO> getRecommendationCandidates(Long userId) {

		List<UserCardRecommendationVO> cards =
			cardMapper.findRecommendationCardsByUserId(userId);

		if (cards.isEmpty()) {
			return List.of();
		}

		List<CardMonthlyStatusVO> monthlyStatuses =
			cardMapper.findMonthlyStatusByUserId(userId);

		String currentYearMonth = YearMonth.now(APP_ZONE)
			.format(DateTimeFormatter.ofPattern("yyyyMM"));

		/*
		 * userCardId별로 월별 실적을 묶는다.
		 *
		 * 예)
		 * 1 -> [202606, 202607, 202608]
		 * 2 -> [202606, 202607, 202608]
		 */
		Map<Long, List<CardMonthlyStatusVO>> monthlyStatusByCard =
			monthlyStatuses.stream()
				.collect(Collectors.groupingBy(
					CardMonthlyStatusVO::getUserCardId
				));

		return cards.stream()
			.map(card -> toRecommendationCandidate(
				card,
				monthlyStatusByCard.getOrDefault(
					card.getUserCardId(),
					List.of()
				),
				currentYearMonth
			))
			.collect(Collectors.toList());
	}

	/**
	 * 카드 기본정보와 월별 실적 이력을
	 * 추천 도메인에서 사용하는 RecommendationCardCandidateVO로 변환한다.
	 */
	private RecommendationCardCandidateVO toRecommendationCandidate(
		UserCardRecommendationVO card,
		List<CardMonthlyStatusVO> monthlyStatuses,
		String currentYearMonth
	) {

		Map<String, Long> spendHistory = new HashMap<>();

		long currentMonthSpend = 0L;

		for (CardMonthlyStatusVO status : monthlyStatuses) {

			String targetYearMonth = status.getTargetYearMonth();

			long spendingAmount = status.getTotalSpendingAmount() == null ? 0L : status.getTotalSpendingAmount();

			/*
			 * 현재 월은 진행 중인 실적이므로
			 * currentMonthSpend에 별도로 저장한다.
			 */
			if (currentYearMonth.equals(targetYearMonth)) {
				currentMonthSpend = spendingAmount;
				continue;
			}

			/*
			 * 현재 월보다 이전인 완료된 월만
			 * spendHistory에 포함한다.
			 *
			 * yyyyMM 형식이므로 문자열 비교로도
			 * 연월의 선후 관계를 비교할 수 있다.
			 */
			if (targetYearMonth.compareTo(currentYearMonth) < 0) {
				spendHistory.put(
					targetYearMonth,
					spendingAmount
				);
			}
		}

		RecommendationCardCandidateVO candidate = new RecommendationCardCandidateVO();

		candidate.setUserCardId(card.getUserCardId());
		candidate.setCardId(card.getCardId());
		candidate.setCardName(card.getCardName());
		candidate.setCardImageUrl(card.getCardImageUrl());
		candidate.setBenefitsInfo(card.getBenefitsInfo());

		candidate.setSpendHistory(spendHistory);
		candidate.setCurrentMonthSpend(currentMonthSpend);

		return candidate;
	}
}
