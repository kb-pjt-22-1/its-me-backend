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
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
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
			userId, request.getUserCardId(), request.getMerchantId());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{paymentTokenId}")
	public ResponseEntity<PaymentTokenResponseDto> getTokenStatus(@AuthenticationPrincipal Long userId,
		@PathVariable String paymentTokenId) {
		PaymentTokenResponseDto response = paymentTokenService.getTokenStatus(paymentTokenId);

		return ResponseEntity.ok(response);
	}

	// 프론트의 "결제완료하기" 버튼이 호출한다. 요청 바디 없음 - 실제 매장 스캔이 불가능한 구조라
	// 가맹점/금액은 서버가 데모용으로 무작위 생성한다.
	@PostMapping("/{paymentTokenId}/complete")
	public ResponseEntity<PaymentHistoryResponseDto> completeToken(@AuthenticationPrincipal Long userId,
		@PathVariable String paymentTokenId) {
		PaymentHistoryResponseDto response = paymentTokenService.completeToken(paymentTokenId);

		return ResponseEntity.ok(response);
	}

	// 프론트의 "취소하기" 버튼이 호출한다. 아직 결제가 일어난 적이 없어서 payments 테이블은 안 건드린다.
	@PostMapping("/{paymentTokenId}/cancel")
	public ResponseEntity<PaymentTokenResponseDto> cancelToken(@AuthenticationPrincipal Long userId,
		@PathVariable String paymentTokenId) {
		PaymentTokenResponseDto response = paymentTokenService.cancelToken(paymentTokenId);

		return ResponseEntity.ok(response);
	}
}