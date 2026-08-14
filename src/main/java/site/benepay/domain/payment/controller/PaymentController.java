package site.benepay.domain.payment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;

	// 결제 내역 목록 (최신순, 가맹점명·카드명 포함, 취소된 결제도 CANCELED 상태로 그대로 포함됨)
	// yearMonth(yyyyMM)를 주면 그 달만 DB 단에서 필터링해서 반환한다. 생략하면 전체 내역.
	@GetMapping
	public ResponseEntity<List<PaymentHistoryResponseDto>> getPaymentHistory(@AuthenticationPrincipal Long userId,
		@RequestParam(required = false) String yearMonth) {
		List<PaymentHistoryResponseDto> response = paymentService.getPaymentHistory(userId, yearMonth);

		return ResponseEntity.ok(response);
	}

	// 결제 단건 상세 (가맹점명·카드명 포함)
	@GetMapping("/{paymentId}")
	public ResponseEntity<PaymentHistoryResponseDto> getPayment(@AuthenticationPrincipal Long userId,
		@PathVariable Long paymentId) {
		PaymentHistoryResponseDto response = paymentService.getPayment(paymentId);

		return ResponseEntity.ok(response);
	}

	// 승인된 결제 취소 (환불). 요청 바디 없음.
	@PostMapping("/{paymentId}/cancel")
	public ResponseEntity<PaymentHistoryResponseDto> cancelPayment(@AuthenticationPrincipal Long userId,
		@PathVariable Long paymentId) {
		PaymentHistoryResponseDto response = paymentService.cancelPayment(userId, paymentId);

		return ResponseEntity.ok(response);
	}
}