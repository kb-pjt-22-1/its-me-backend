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
import site.benepay.domain.payment.dto.PaymentResponseDto;
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
		PaymentTokenResponseDto response = paymentTokenService.issueToken(
			userId,
			request.getUserCardId(),
			request.getMerchantId(),
			request.getOriginalAmount(),
			request.getDiscountAmount()
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{paymentTokenId}")
	public ResponseEntity<PaymentTokenResponseDto> getTokenStatus(@AuthenticationPrincipal Long userId,
		@PathVariable String paymentTokenId) {
		PaymentTokenResponseDto response = paymentTokenService.getTokenStatus(paymentTokenId);

		return ResponseEntity.ok(response);
	}

	// 명세서엔 없는 엔드포인트: 실제 매장 스캔 대신 프론트의 "결제완료하기" 버튼이 호출한다.
	@PostMapping("/{paymentTokenId}/complete")
	public ResponseEntity<PaymentResponseDto> completeToken(@AuthenticationPrincipal Long userId,
		@PathVariable String paymentTokenId) {
		PaymentResponseDto response = paymentTokenService.completeToken(paymentTokenId);

		return ResponseEntity.ok(response);
	}
}
