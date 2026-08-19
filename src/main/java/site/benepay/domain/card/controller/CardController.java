package site.benepay.domain.card.controller;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.card.dto.CardBenefitResponseDto;
import site.benepay.domain.card.dto.CardDetailResponseDto;
import site.benepay.domain.card.dto.CardListResponseDto;
import site.benepay.domain.card.dto.CardPerformanceResponseDto;
import site.benepay.domain.card.dto.CardRecommendationRequestDto;
import site.benepay.domain.card.dto.CardRecommendationResponseDto;
import site.benepay.domain.card.dto.CardRepresentativeResponseDto;
import site.benepay.domain.card.service.CardService;

/**
 * 사용자 카드 조회 및 설정 변경 API를 제공하는 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

	private final CardService cardService;

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	/**
	 * 사용자가 보유한 전체 카드 목록을 조회한다.
	 */
	@GetMapping
	public ResponseEntity<List<CardListResponseDto>> getCardList(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(cardService.getCardList(userId));
	}

	/**
	 * 사용자가 보유한 특정 카드의 상세 정보를 조회한다.
	 */
	@GetMapping("/{userCardId}")
	public ResponseEntity<CardDetailResponseDto> getCardDetail(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId) {
		CardDetailResponseDto response = cardService.getCardDetail(userId, userCardId);
		return ResponseEntity.ok(response);
	}

	/**
	 * 사용자 카드에 적용되는 혜택 정보를 조회한다.
	 */
	@GetMapping("/{userCardId}/benefits")
	public ResponseEntity<CardBenefitResponseDto> getCardBenefits(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId) {
		CardBenefitResponseDto response = cardService.getCardBenefits(userId, userCardId);
		return ResponseEntity.ok(response);
	}

	/**
	 * 사용자의 카드 월별 실적 정보를 조회한다.
	 * 조회 월이 없으면 현재 월을 기준으로 조회한다.
	 */
	@GetMapping("/{userCardId}/performance")
	public ResponseEntity<CardPerformanceResponseDto> getCardPerformance(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId, @RequestParam(required = false) String yearMonth) {
		String targetYearMonth =
			yearMonth != null ? yearMonth : YearMonth.now(ZONE).format(DateTimeFormatter.ofPattern("yyyyMM"));
		CardPerformanceResponseDto response = cardService.getCardPerformance(userId, userCardId, targetYearMonth);
		return ResponseEntity.ok(response);
	}

	/**
	 * 특정 카드를 사용자의 대표 카드로 설정한다.
	 */
	@PatchMapping("/{userCardId}/representative")
	public ResponseEntity<CardRepresentativeResponseDto> setRepresentativeCard(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId) {
		CardRepresentativeResponseDto response = cardService.setRepresentativeCard(userId, userCardId);
		return ResponseEntity.ok(response);
	}

	/**
	 * 특정 카드의 추천 포함 여부를 변경한다.
	 */
	@PatchMapping("/{userCardId}/recommendation")
	public ResponseEntity<CardRecommendationResponseDto> updateRecommendation(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId, @Valid @RequestBody CardRecommendationRequestDto request) {
		CardRecommendationResponseDto response = cardService.updateRecommendation(userId, userCardId,
			request.getRecommendationEnabled());
		return ResponseEntity.ok(response);
	}
}
