package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.MerchantNotFoundException;
import site.benepay.common.exception.PaymentTokenNotFoundException;
import site.benepay.common.exception.PaymentTokenNotUsableException;
import site.benepay.common.exception.UserCardNotAvailableException;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.event.PaymentApprovedEvent;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentHistoryVO;
import site.benepay.domain.payment.vo.PaymentTokenVO;
import site.benepay.domain.payment.vo.PaymentVO;
import site.benepay.domain.payment.vo.UserCardPaymentTokenVO;

@Service
@RequiredArgsConstructor
public class PaymentTokenServiceImpl implements PaymentTokenService {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String PAYMENT_METHOD_BARCODE = "BARCODE";

	// 결제완료 버튼엔 금액 입력이 없어서(실제 스캔이 불가능한 구조), 데모용으로 이 범위 안에서
	// 무작위 금액을 만든다. 100원 단위로 반올림해서 그럴듯한 값처럼 보이게 한다.
	private static final int MIN_AMOUNT = 1_000;
	private static final int MAX_AMOUNT = 50_000;
	private static final int AMOUNT_UNIT = 100;

	private final PaymentTokenStore paymentTokenStore;
	private final PaymentMapper paymentMapper;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	public PaymentTokenResponseDto issueToken(Long userId, Long userCardId, Long merchantId) {
		// user_id + user_card_id + status='ACTIVE' 조건 조회라, 소유권/활성 상태 검증을 겸한다.
		UserCardPaymentTokenVO userCardToken = paymentMapper.findActiveCardPaymentToken(userId, userCardId)
			.orElseThrow(() -> new UserCardNotAvailableException("결제에 사용할 수 없는 카드입니다."));

		if (merchantId != null && !paymentMapper.existsMerchant(merchantId)) {
			throw new MerchantNotFoundException("가맹점을 찾을 수 없습니다.");
		}

		PaymentTokenVO token = paymentTokenStore.issue(userId, userCardId, merchantId, userCardToken.getPaymentToken());

		return PaymentTokenResponseDto.from(token);
	}

	@Override
	public PaymentTokenResponseDto getTokenStatus(String paymentTokenId) {
		PaymentTokenVO token = paymentTokenStore.find(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotFoundException("결제 토큰을 찾을 수 없거나 만료되었습니다."));

		return PaymentTokenResponseDto.from(token);
	}

	@Override
	@Transactional
	public PaymentHistoryResponseDto completeToken(String paymentTokenId) {
		PaymentTokenVO token = paymentTokenStore.find(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotFoundException("결제 토큰을 찾을 수 없거나 만료되었습니다."));

		if (!PaymentTokenStore.STATUS_ISSUED.equals(token.getStatus())) {
			throw new PaymentTokenNotUsableException("이미 처리되었거나 완료할 수 없는 결제 토큰입니다.");
		}

		// 발급 시점에 가맹점을 이미 알던 흐름(매장 페이지 경유)이면 그 값을 쓰고,
		// 몰랐던 흐름(결제 페이지 직접 진입)이면 데모용으로 무작위 가맹점을 고른다.
		Long merchantId = token.getMerchantId() != null ? token.getMerchantId() : randomMerchantId();
		BigDecimal originalAmount = randomAmount();

		// USED 전환에 실패했다면(동시에 두 번 완료 요청이 들어온 경쟁 상황) 위 상태 체크를 통과한 직후라도
		// 여기서 다시 막힌다 - 이 경우도 "이미 처리됨"으로 응답한다.
		paymentTokenStore.markUsedIfIssued(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotUsableException("이미 처리되었거나 완료할 수 없는 결제 토큰입니다."));

		PaymentVO payment = PaymentVO.builder()
			.merchantId(merchantId)
			.userCardId(token.getUserCardId())
			.paymentTime(LocalDateTime.now(ZONE))
			.originalAmount(originalAmount)
			.discountAmount(BigDecimal.ZERO)
			.finalAmount(originalAmount)
			.paymentStatus(STATUS_APPROVED)
			.paymentMethod(PAYMENT_METHOD_BARCODE)
			.build();

		paymentMapper.insertPayment(payment);

		PaymentHistoryVO insertedPayment = paymentMapper.findByPaymentId(payment.getPaymentId())
			.orElseThrow(() -> new IllegalStateException("결제 생성 직후 조회에 실패했습니다."));

		eventPublisher.publishEvent(new PaymentApprovedEvent(
			insertedPayment.getPaymentId(),
			insertedPayment.getUserCardId(),
			insertedPayment.getCategoryCode(),
			insertedPayment.getPaymentTime(),
			insertedPayment.getFinalAmount(),
			insertedPayment.getDiscountAmount()
		));

		return PaymentHistoryResponseDto.from(insertedPayment);
	}

	private Long randomMerchantId() {
		return paymentMapper.findRandomMerchantId()
			.orElseThrow(() -> new IllegalStateException("등록된 가맹점이 없어 결제를 완료할 수 없습니다."));
	}

	private BigDecimal randomAmount() {
		int steps = (MAX_AMOUNT - MIN_AMOUNT) / AMOUNT_UNIT;
		int amount = MIN_AMOUNT + ThreadLocalRandom.current().nextInt(steps + 1) * AMOUNT_UNIT;
		return BigDecimal.valueOf(amount);
	}

	@Override
	public PaymentTokenResponseDto cancelToken(String paymentTokenId) {
		// find()로 먼저 확인해서, 존재 자체를 안 하는 경우(404)와 이미 처리되어 취소 불가능한
		// 경우(409)를 구분해 응답한다.
		paymentTokenStore.find(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotFoundException("결제 토큰을 찾을 수 없거나 만료되었습니다."));

		PaymentTokenVO canceled = paymentTokenStore.cancelIfIssued(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotUsableException("이미 처리되었거나 취소할 수 없는 결제 토큰입니다."));

		return PaymentTokenResponseDto.from(canceled);
	}
}