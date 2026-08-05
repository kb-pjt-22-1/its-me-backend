package site.benepay.domain.payment.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.dto.PaymentStatusUpdateRequestDto;
import site.benepay.domain.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;

	@GetMapping
	public ResponseEntity<List<PaymentResponseDto>> getPaymentHistory(@AuthenticationPrincipal Long userId) {
		List<PaymentResponseDto> response = paymentService.getPaymentHistory(userId);

		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<PaymentResponseDto> createPayment(@AuthenticationPrincipal Long userId,
		@Valid @RequestBody PaymentCreateRequestDto request) {
		// TODO: userCardId가 userId 소유의 카드인지 검증하는 로직이 아직 없음 (card 도메인 연동 필요)
		PaymentResponseDto response = paymentService.createPayment(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{paymentId}")
	public ResponseEntity<PaymentResponseDto> getPayment(@AuthenticationPrincipal Long userId,
		@PathVariable Long paymentId) {
		PaymentResponseDto response = paymentService.getPayment(paymentId);

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{paymentId}/status")
	public ResponseEntity<PaymentResponseDto> updatePaymentStatus(@AuthenticationPrincipal Long userId,
		@PathVariable Long paymentId, @Valid @RequestBody PaymentStatusUpdateRequestDto request) {
		PaymentResponseDto response = paymentService.updatePaymentStatus(paymentId, request.getPaymentStatus());

		return ResponseEntity.ok(response);
	}
}
