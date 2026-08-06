package site.benepay.domain.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.PaymentNotFoundException;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

	private final PaymentMapper paymentMapper;

	@Override
	public PaymentHistoryResponseDto getPayment(Long paymentId) {
		return paymentMapper.findByPaymentId(paymentId)
			.map(PaymentHistoryResponseDto::from)
			.orElseThrow(() -> new PaymentNotFoundException("결제 내역을 찾을 수 없습니다."));
	}

	@Override
	public List<PaymentHistoryResponseDto> getPaymentHistory(Long userId) {
		return paymentMapper.findPaymentHistoryByUserId(userId).stream()
			.map(PaymentHistoryResponseDto::from)
			.toList();
	}
}