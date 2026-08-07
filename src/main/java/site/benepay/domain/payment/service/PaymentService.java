package site.benepay.domain.payment.service;

import java.util.List;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;

public interface PaymentService {

	PaymentHistoryResponseDto getPayment(Long paymentId);

	List<PaymentHistoryResponseDto> getPaymentHistory(Long userId);

	// 승인(APPROVED)된 결제만 취소 가능. 취소되면 내역 조회에 CANCELED 상태로 그대로 나온다.
	PaymentHistoryResponseDto cancelPayment(Long userId, Long paymentId);
}