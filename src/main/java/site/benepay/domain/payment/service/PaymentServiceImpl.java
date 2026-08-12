package site.benepay.domain.payment.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.PaymentNotCancelableException;
import site.benepay.common.exception.PaymentNotFoundException;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.event.PaymentCanceledEvent;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentHistoryVO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

	private final PaymentMapper paymentMapper;
	private final ApplicationEventPublisher eventPublisher;

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

	@Override
	@Transactional
	public PaymentHistoryResponseDto cancelPayment(Long userId, Long paymentId) {
		// 소유권 + 현재 APPROVED 상태인지를 UPDATE 조건 자체로 확인한다 (0건이면 아래에서 막힘).
		int updatedCount = paymentMapper.cancelApprovedPayment(userId, paymentId);

		if (updatedCount != 1) {
			throw new PaymentNotCancelableException("취소할 수 있는 결제 내역을 찾을 수 없습니다.");
		}

		PaymentHistoryVO canceled = paymentMapper.findByPaymentId(paymentId)
			.orElseThrow(() -> new PaymentNotFoundException("결제 내역을 찾을 수 없습니다."));

		// approvedAt은 취소 시각이 아니라 원래 결제가 승인됐던 시각이다 - 실적을 더했던 그 달에서
		// 그대로 빼야 하기 때문에, canceled.getPaymentTime()(원래 결제 시각)을 그대로 쓴다.
		eventPublisher.publishEvent(new PaymentCanceledEvent(
			canceled.getPaymentId(),
			canceled.getUserCardId(),
			canceled.getCategoryCode(),
			canceled.getPaymentTime(),
			canceled.getFinalAmount(),
			canceled.getDiscountAmount()
		));

		return PaymentHistoryResponseDto.from(canceled);
	}
}