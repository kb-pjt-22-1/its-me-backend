package site.benepay.domain.notification.event;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.benepay.domain.notification.dto.PushNotificationMessage;
import site.benepay.domain.notification.service.PushNotificationSender;
import site.benepay.domain.payment.event.PaymentApprovedEvent;

/**
 * 결제가 승인되면 결제 정보(가맹점, 결제 금액)를 담아 푸시 알림을 보낸다.
 *
 * <p>AFTER_COMMIT을 쓰는 이유: 같은 이벤트를 구독하는 CardPerformanceEventHandler/
 * BenefitUsageEventHandler는 집계 갱신에 실패하면 예외를 던져 결제 트랜잭션 자체를
 * 롤백시킨다. 그 경우까지 포함해 결제가 실제로 COMMIT된 뒤에만 알림을 보내야
 * "결제는 실패했는데 알림만 왔다"는 상황을 피할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentApprovedPushHandler {

	private final PushNotificationSender pushNotificationSender;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(PaymentApprovedEvent event) {
		try {
			pushNotificationSender.send(new PushNotificationMessage(
				event.userId(),
				"결제가 완료됐어요",
				buildBody(event),
				Map.of("paymentId", String.valueOf(event.paymentId()))
			));
		} catch (RuntimeException e) {
			// 이미 COMMIT된 결제이므로 알림 전송 실패가 결제 처리 결과에 영향을 주면 안 된다.
			log.warn("결제 완료 푸시 알림 전송 실패. paymentId={}, userId={}", event.paymentId(), event.userId(), e);
		}
	}

	private String buildBody(PaymentApprovedEvent event) {
		String amountText = String.format("%,d원", event.performanceAmount().longValueExact());

		if (isBenefitApplied(event.discountAmount(), event.benefitServiceName())) {
			String discountText = String.format("%,d원", event.discountAmount().longValueExact());
			return String.format("%s에서 %s 결제했어요. %s 혜택으로 %s 할인받았어요.",
				event.merchantName(), amountText, event.benefitServiceName(), discountText);
		}

		return String.format("%s에서 %s 결제했어요.", event.merchantName(), amountText);
	}

	private boolean isBenefitApplied(BigDecimal discountAmount, String benefitServiceName) {
		return benefitServiceName != null && discountAmount != null && discountAmount.signum() > 0;
	}
}
