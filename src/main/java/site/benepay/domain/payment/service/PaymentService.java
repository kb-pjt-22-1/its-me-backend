package site.benepay.domain.payment.service;

import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;

public interface PaymentService {

	PaymentResponseDto createPayment(PaymentCreateRequestDto request);

	PaymentResponseDto getPayment(Long paymentId);

	PaymentResponseDto updatePaymentStatus(Long paymentId, String paymentStatus);
}
