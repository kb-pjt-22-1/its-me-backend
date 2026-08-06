package site.benepay.domain.payment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;

	// 결제 내역 목록 (최신순, 가맹점명·카드명 포함)
	@GetMapping
	public ResponseEntity<List<PaymentHistoryResponseDto>> getPaymentHistory(@AuthenticationPrincipal Long userId) {
		List<PaymentHistoryResponseDto> response = paymentService.getPaymentHistory(userId);

		return ResponseEntity.ok(response);
	}

	// 결제 단건 상세 (가맹점명·카드명 포함)
	@GetMapping("/{paymentId}")
	public ResponseEntity<PaymentHistoryResponseDto> getPayment(@AuthenticationPrincipal Long userId,
		@PathVariable Long paymentId) {
		PaymentHistoryResponseDto response = paymentService.getPayment(paymentId);

		return ResponseEntity.ok(response);
	}
}
