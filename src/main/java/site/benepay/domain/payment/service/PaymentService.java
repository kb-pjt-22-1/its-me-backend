package site.benepay.domain.payment.service;

import java.util.List;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;

public interface PaymentService {

	PaymentResponseDto getPayment(Long paymentId);

	List<PaymentHistoryResponseDto> getPaymentHistory(Long userId);
}
