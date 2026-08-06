package site.benepay.domain.card.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

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
			return objectMapper.readTree(benefitsInfo);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("카드 혜택 JSON 형식이 올바르지 않습니다.", e);
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
}
