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
import site.benepay.common.exception.CardBenefitParseException;
import site.benepay.common.exception.CardSettingUpdateException;
import site.benepay.common.exception.InvalidYearMonthException;
import site.benepay.common.exception.UserCardNotAvailableException;
import site.benepay.common.exception.UserCardNotFoundException;
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

	// 카드 실적 기준 월 계산을 위한 한국 시간대
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private static final String USER_CARD_NOT_FOUND_MESSAGE = "보유 카드를 찾을 수 없습니다.";

	private final CardMapper cardMapper;
	private final ObjectMapper objectMapper;

	/**
	 * 사용자가 보유한 전체 카드 목록을 조회한다.
	 */
	public List<CardListResponseDto> getCardList(Long userId) {
		List<UserCardListVO> cardList = cardMapper.findAllByUserId(userId);
		return cardList.stream()
			.map(this::toCardListResponseDto)
			.toList();
	}

	/**
	 * 사용자가 보유한 특정 카드의 상세 정보를 조회한다.
	 */
	public CardDetailResponseDto getCardDetail(Long userId, Long userCardId) {
		UserCardDetailVO card = cardMapper.findDetailByUserCardId(userId, userCardId)
			.orElseThrow(() -> new UserCardNotFoundException(USER_CARD_NOT_FOUND_MESSAGE));

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

	/**
	 * 사용자가 보유한 특정 카드의 혜택 정보를 조회한다.
	 */
	public CardBenefitResponseDto getCardBenefits(Long userId, Long userCardId) {
		UserCardBenefitVO benefit = cardMapper.findBenefitsByUserCardId(userId, userCardId)
			.orElseThrow(() -> new UserCardNotFoundException(USER_CARD_NOT_FOUND_MESSAGE));
		JsonNode benefitsJson = parseBenefitsInfo(benefit.getBenefitsInfo());

		return CardBenefitResponseDto.builder()
			.userCardId(benefit.getUserCardId())
			.cardId(benefit.getCardId())
			.cardName(benefit.getCardName())
			.minBenefitAmount(benefit.getMinBenefitAmount())
			.benefits(benefitsJson)
			.build();
	}

	/**
	 * 특정 카드의 월별 이용 실적과 목표 달성 정보를 조회한다.
	 */
	public CardPerformanceResponseDto getCardPerformance(Long userId, Long userCardId, String yearMonth) {
		validateYearMonth(yearMonth);

		UserCardPerformanceVO performance =
			cardMapper.findPerformanceByUserCardId(
				userId,
				userCardId,
				yearMonth
			).orElseThrow(() ->
				new UserCardNotFoundException(
					USER_CARD_NOT_FOUND_MESSAGE
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

	/**
	 * 특정 카드를 사용자의 대표 카드로 설정한다.
	 */
	@Transactional
	public CardRepresentativeResponseDto setRepresentativeCard(Long userId, Long userCardId) {
		validateActiveOwnedCard(userId, userCardId);

		// 기존 대표카드 해제
		cardMapper.clearPrimaryCard(userId);

		// 선택한 카드 대표카드 설정
		int updatedCount =
			cardMapper.setPrimaryCard(userId, userCardId);

		if (updatedCount != 1) {
			throw new CardSettingUpdateException(
				"대표카드 설정에 실패했습니다."
			);
		}

		return CardRepresentativeResponseDto.builder()
			.userCardId(userCardId)
			.primary(true)
			.build();
	}

	/**
	 * 특정 카드의 추천 포함 여부를 변경한다.
	 */
	@Transactional
	public CardRecommendationResponseDto updateRecommendation(Long userId, Long userCardId,
		Boolean recommendationEnabled) {

		validateActiveOwnedCard(userId, userCardId);

		int updatedCount = cardMapper.updateRecommendationEnabled(userId, userCardId, recommendationEnabled);

		if (updatedCount != 1) {
			throw new CardSettingUpdateException(
				"카드 추천 설정 변경에 실패했습니다."
			);
		}

		return CardRecommendationResponseDto.builder()
			.userCardId(userCardId)
			.recommendationEnabled(recommendationEnabled)
			.build();
	}

	/**
	 * 추천 도메인에서 사용할 사용자의 카드 후보 정보를 조회한다.
	 * 추천이 활성화된 ACTIVE 카드에 혜택과 월별 실적 정보를 함께 구성한다.
	 */
	public List<RecommendationCardCandidateVO> getRecommendationCandidates(Long userId) {

		List<UserCardRecommendationVO> cards =
			cardMapper.findRecommendationCardsByUserId(userId);

		if (cards.isEmpty()) {
			return List.of();
		}

		List<CardMonthlyStatusVO> monthlyStatuses =
			cardMapper.findMonthlyStatusByUserId(userId);

		String currentYearMonth = YearMonth.now(ZONE)
			.format(DateTimeFormatter.ofPattern("yyyyMM"));

		// 카드별로 월별 실적을 그룹화한다.
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
			.toList();
	}

	/**
	 * 카드 목록 조회 결과 VO를 응답 DTO로 변환한다.
	 */
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

	/**
	 * 카드 끝 4자리를 표시용 마스킹 번호로 변환한다.
	 */
	private String maskCardNumber(String panLast4) {
		if (panLast4 == null || panLast4.isBlank()) {
			return null;
		}
		return "**** **** **** " + panLast4;
	}

	/**
	 * 문자열 형태의 카드 혜택 정보를 JSON 객체로 변환한다.
	 */
	private JsonNode parseBenefitsInfo(String benefitsInfo) {
		if (benefitsInfo == null || benefitsInfo.isBlank()) {
			return objectMapper.createObjectNode();
		}
		try {
			JsonNode root = objectMapper.readTree(benefitsInfo);
			fillDiscountRateForPointAccumulation(root);
			return root;
		} catch (JsonProcessingException e) {
			throw new CardBenefitParseException("카드 혜택 JSON 형식이 올바르지 않습니다.", e);
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

	/**
	 * 조회 연월이 yyyyMM 형식인지 검증한다.
	 */
	private void validateYearMonth(String yearMonth) {
		try {
			YearMonth.parse(
				yearMonth,
				DateTimeFormatter.ofPattern("yyyyMM")
			);
		} catch (DateTimeParseException e) {
			throw new InvalidYearMonthException(
				"yearMonth는 YYYYMM 형식이어야 합니다."
			);
		}
	}

	/**
	 * 현재 실적 대비 목표 실적의 달성률을 계산한다.
	 * 달성률은 최대 100%로 제한한다.
	 */
	private double calculateAchievementRate(long currentAmount, long requiredAmount) {
		if (requiredAmount == 0L) {
			return 100.0;
		}

		double rate = (double)currentAmount / requiredAmount * 100;
		double roundedRate = Math.round(rate * 10.0) / 10.0;

		return Math.min(roundedRate, 100.0);
	}

	/**
	 * 요청한 카드가 사용자의 정상 사용 가능한 보유 카드인지 검증한다.
	 */
	private void validateActiveOwnedCard(Long userId, Long userCardId) {
		boolean exists = cardMapper.existsActiveUserCard(userId, userCardId);

		if (!exists) {
			throw new UserCardNotAvailableException(
				"정상 사용 가능한 보유 카드를 찾을 수 없습니다."
			);
		}
	}

	/**
	 * 카드 기본 정보와 월별 실적을 추천용 카드 후보 객체로 변환한다.
	 */
	private RecommendationCardCandidateVO toRecommendationCandidate(UserCardRecommendationVO card,
		List<CardMonthlyStatusVO> monthlyStatuses, String currentYearMonth) {

		Map<String, Long> spendHistory = new HashMap<>();
		long currentMonthSpend = 0L;

		for (CardMonthlyStatusVO status : monthlyStatuses) {
			String targetYearMonth = status.getTargetYearMonth();
			long spendingAmount = status.getTotalSpendingAmount() == null ? 0L : status.getTotalSpendingAmount();

			// 현재 월 실적은 별도로 관리한다.
			if (currentYearMonth.equals(targetYearMonth)) {
				currentMonthSpend = spendingAmount;
				continue;
			}

			// 완료된 과거 월 실적만 이력에 포함한다.
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
