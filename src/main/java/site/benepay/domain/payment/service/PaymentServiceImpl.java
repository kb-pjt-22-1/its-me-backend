package site.benepay.domain.payment.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.InvalidYearMonthException;
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

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

	private final PaymentMapper paymentMapper;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public PaymentHistoryResponseDto getPayment(Long paymentId) {
		return paymentMapper.findByPaymentId(paymentId)
			.map(PaymentHistoryResponseDto::from)
			.orElseThrow(() -> new PaymentNotFoundException("결제 내역을 찾을 수 없습니다."));
	}

	@Override
	public List<PaymentHistoryResponseDto> getPaymentHistory(Long userId, String yearMonth) {
		// yearMonth가 없으면(null/빈 문자열) 전체 내역을 반환하고, 있으면 [해당 월 1일 00:00, 다음 달
		// 1일 00:00) 범위로 변환해서 Mapper에 넘긴다. yyyyMM 문자열을 그대로 넘겨 SQL에서
		// DATE_FORMAT(payment_time, ...)으로 비교하면 인덱스를 못 타서(non-sargable) 결제가
		// 많아질수록 이 쿼리 자체가 느려진다 - 그래서 여기서 날짜 범위로 미리 변환해 넘긴다.
		LocalDateTime[] range = toPaymentTimeRange(yearMonth);

		return paymentMapper.findPaymentHistoryByUserId(userId, range[0], range[1]).stream()
			.map(PaymentHistoryResponseDto::from)
			.toList();
	}

	/**
	 * yearMonth(yyyyMM)를 [해당 월 1일 00:00, 다음 달 1일 00:00) 범위로 변환한다. 비어있으면
	 * {null, null}을 돌려줘서 "전체 조회"임을 나타낸다. YearMonth.parse가 형식 검증뿐 아니라
	 * "202613"처럼 존재하지 않는 달도 걸러준다.
	 */
	private LocalDateTime[] toPaymentTimeRange(String yearMonth) {
		if (yearMonth == null || yearMonth.isBlank()) {
			return new LocalDateTime[] { null, null };
		}

		YearMonth parsed;
		try {
			parsed = YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new InvalidYearMonthException("yearMonth는 yyyyMM 형식이어야 합니다.");
		}

		LocalDateTime start = parsed.atDay(1).atStartOfDay();
		LocalDateTime end = parsed.plusMonths(1).atDay(1).atStartOfDay();

		return new LocalDateTime[] { start, end };
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