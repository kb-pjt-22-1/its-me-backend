package site.benepay.domain.payment.service;

import java.util.List;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;

public interface PaymentService {

	PaymentHistoryResponseDto getPayment(Long paymentId);

	// yearMonth(yyyyMM)가 있으면 그 달만, null/빈 문자열이면 전체 내역을 반환한다.
	List<PaymentHistoryResponseDto> getPaymentHistory(Long userId, String yearMonth);

	// 승인(APPROVED)된 결제만 취소 가능. 취소되면 내역 조회에 CANCELED 상태로 그대로 나온다.
	PaymentHistoryResponseDto cancelPayment(Long userId, Long paymentId);
}