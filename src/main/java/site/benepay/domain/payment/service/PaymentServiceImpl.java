package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.InvalidPaymentAmountException;
import site.benepay.common.exception.PaymentNotFoundException;
import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

	private static final String STATUS_PENDING = "PENDING";

	private final PaymentMapper paymentMapper;

	@Override
	@Transactional
	public PaymentResponseDto createPayment(PaymentCreateRequestDto request) {
		BigDecimal originalAmount = request.getOriginalAmount();
		BigDecimal discountAmount = request.getDiscountAmount() == null
			? BigDecimal.ZERO
			: request.getDiscountAmount();
		BigDecimal finalAmount = originalAmount.subtract(discountAmount);

		if (finalAmount.signum() < 0) {
			throw new InvalidPaymentAmountException("할인 금액이 결제 금액보다 클 수 없습니다.");
		}

		PaymentVO payment = new PaymentVO();
		payment.setMerchantId(request.getMerchantId());
		payment.setUserCardId(request.getUserCardId());
		payment.setPaymentTime(LocalDateTime.now());
		payment.setOriginalAmount(originalAmount);
		payment.setDiscountAmount(discountAmount);
		payment.setFinalAmount(finalAmount);
		payment.setPaymentStatus(STATUS_PENDING);
		payment.setPaymentMethod(request.getPaymentMethod());

		paymentMapper.insertPayment(payment);

		return PaymentResponseDto.from(payment);
	}

	@Override
	public PaymentResponseDto getPayment(Long paymentId) {
		return PaymentResponseDto.from(findPaymentOrThrow(paymentId));
	}

	@Override
	@Transactional
	public PaymentResponseDto updatePaymentStatus(Long paymentId, String paymentStatus) {
		int updatedCount = paymentMapper.updatePaymentStatus(paymentId, paymentStatus);

		if (updatedCount != 1) {
			throw new PaymentNotFoundException("결제 내역을 찾을 수 없습니다.");
		}

		return PaymentResponseDto.from(findPaymentOrThrow(paymentId));
	}

	private PaymentVO findPaymentOrThrow(Long paymentId) {
		return paymentMapper.findByPaymentId(paymentId)
			.orElseThrow(() -> new PaymentNotFoundException("결제 내역을 찾을 수 없습니다."));
	}
}