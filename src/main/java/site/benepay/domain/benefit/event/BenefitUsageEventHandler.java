package site.benepay.domain.benefit.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.domain.benefit.mapper.BenefitUsageMapper;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.event.PaymentCanceledEvent;

/**
 * 결제 승인/취소 이벤트를 구독해 card_benefit_monthly_usage(혜택별 월/연 한도 소진액)를
 * 갱신한다. card 도메인의 CardPerformanceEventHandler(카드 실적 갱신)와 동일한 패턴 -
 * 실제로 혜택이 적용된 결제(discountAmount > 0 && benefitServiceName != null)만 다룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BenefitUsageEventHandler {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

	private final BenefitUsageMapper benefitUsageMapper;

	@EventListener
	public void handlePaymentApproved(PaymentApprovedEvent event) {
		if (!isBenefitApplied(event.discountAmount(), event.benefitServiceName())) {
			return;
		}

		benefitUsageMapper.upsertMonthlyUsage(
			event.userCardId(),
			event.benefitServiceName(),
			event.approvedAt().getYear(),
			toYearMonth(event.approvedAt()),
			event.discountAmount().longValueExact()
		);
	}

	@EventListener
	public void handlePaymentCanceled(PaymentCanceledEvent event) {
		if (!isBenefitApplied(event.discountAmount(), event.benefitServiceName())) {
			return;
		}

		int affectedRows = benefitUsageMapper.decrementMonthlyUsage(
			event.userCardId(),
			event.benefitServiceName(),
			toYearMonth(event.approvedAt()),
			event.discountAmount().longValueExact()
		);

		if (affectedRows < 1) {
			log.warn(
				"혜택 소진액 집계 행이 없어 차감을 건너뜁니다: paymentId={}, userCardId={}, benefitServiceName={}",
				event.paymentId(),
				event.userCardId(),
				event.benefitServiceName()
			);
		}
	}

	private boolean isBenefitApplied(BigDecimal discountAmount, String benefitServiceName) {
		return benefitServiceName != null && discountAmount != null && discountAmount.signum() > 0;
	}

	private static String toYearMonth(LocalDateTime approvedAt) {
		return approvedAt.format(YEAR_MONTH_FORMATTER);
	}
}
