package site.benepay.domain.payment.service;

import java.util.List;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;

public interface PaymentService {

	PaymentHistoryResponseDto getPayment(Long paymentId);

	List<PaymentHistoryResponseDto> getPaymentHistory(Long userId);
}
