package site.benepay.domain.payment.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.payment.dto.PaymentTokenCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.service.PaymentTokenService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-tokens")
public class PaymentTokenController {

	private final PaymentTokenService paymentTokenService;

	@PostMapping
	public ResponseEntity<PaymentTokenResponseDto> issueToken(@AuthenticationPrincipal Long userId,
		@Valid @RequestBody PaymentTokenCreateRequestDto request) {
		PaymentTokenResponseDto response = paymentTokenService.issueToken(userId, request.getUserCardId());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{paymentTokenId}")
	public ResponseEntity<PaymentTokenResponseDto> getTokenStatus(@AuthenticationPrincipal Long userId,
		@PathVariable String paymentTokenId) {
		PaymentTokenResponseDto response = paymentTokenService.getTokenStatus(paymentTokenId);

		return ResponseEntity.ok(response);
	}
}
