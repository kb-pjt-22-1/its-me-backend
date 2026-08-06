package site.benepay.domain.card.controller;

import java.time.YearMonth;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

	private final CardService cardService;

	@GetMapping("/{userCardId}/performance")
	public ResponseEntity<CardPerformanceResponseDto> getCardPerformance(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId, @RequestParam(required = false) String yearMonth) {
		String targetYearMonth =
			yearMonth != null ? yearMonth : YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

		CardPerformanceResponseDto response = cardService.getCardPerformance(userId, userCardId, targetYearMonth);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{userCardId}/benefits")
	public ResponseEntity<CardBenefitResponseDto> getCardBenefits(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId) {
		CardBenefitResponseDto response = cardService.getCardBenefits(userId, userCardId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{userCardId}")
	public ResponseEntity<CardDetailResponseDto> getCardDetail(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId) {
		CardDetailResponseDto response = cardService.getCardDetail(userId, userCardId);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<List<CardListResponseDto>> getCardList(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(cardService.getCardList(userId));
	}

	@PatchMapping("/{userCardId}/representative")
	public ResponseEntity<CardRepresentativeResponseDto> setRepresentativeCard(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId) {
		CardRepresentativeResponseDto response = cardService.setRepresentativeCard(userId, userCardId);

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{userCardId}/recommendation")
	public ResponseEntity<CardRecommendationResponseDto> updateRecommendation(@AuthenticationPrincipal Long userId,
		@PathVariable Long userCardId, @Valid @RequestBody CardRecommendationRequestDto request) {
		CardRecommendationResponseDto response = cardService.updateRecommendation(userId, userCardId,
			request.getRecommendationEnabled());

		return ResponseEntity.ok(response);
	}
}
