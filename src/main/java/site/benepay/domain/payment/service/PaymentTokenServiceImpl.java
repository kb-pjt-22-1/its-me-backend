package site.benepay.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.InvalidPaymentAmountException;
import site.benepay.common.exception.PaymentTokenNotFoundException;
import site.benepay.common.exception.PaymentTokenNotUsableException;
import site.benepay.common.exception.UserCardNotAvailableException;
import site.benepay.domain.merchant.service.MerchantService;
import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.mapper.PaymentMapper;
import site.benepay.domain.payment.vo.PaymentTokenVO;
import site.benepay.domain.payment.vo.PaymentVO;
import site.benepay.domain.payment.vo.UserCardPaymentTokenVO;

@Service
@RequiredArgsConstructor
public class PaymentTokenServiceImpl implements PaymentTokenService {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String PAYMENT_METHOD_BARCODE = "BARCODE";

	private final PaymentTokenStore paymentTokenStore;
	private final PaymentMapper paymentMapper;
	private final MerchantService merchantService;

	@Override
	public PaymentTokenResponseDto issueToken(Long userId, Long userCardId) {
		// user_id + user_card_id + status='ACTIVE' 조건 조회라, 소유권/활성 상태 검증을 겸한다.
		UserCardPaymentTokenVO userCardToken = paymentMapper.findActiveCardPaymentToken(userId, userCardId)
			.orElseThrow(() -> new UserCardNotAvailableException("결제에 사용할 수 없는 카드입니다."));

		PaymentTokenVO token = paymentTokenStore.issue(userId, userCardId, userCardToken.getPaymentToken());

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
	public PaymentHistoryResponseDto completeToken(String paymentTokenId, Long merchantId,
		BigDecimal originalAmount, BigDecimal discountAmount) {
		BigDecimal safeDiscountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;

		if (safeDiscountAmount.compareTo(originalAmount) > 0) {
			throw new InvalidPaymentAmountException("할인 금액이 결제 금액보다 클 수 없습니다.");
		}

		PaymentTokenVO token = paymentTokenStore.find(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotFoundException("결제 토큰을 찾을 수 없거나 만료되었습니다."));

		if (!PaymentTokenStore.STATUS_ISSUED.equals(token.getStatus())) {
			throw new PaymentTokenNotUsableException("이미 처리되었거나 완료할 수 없는 결제 토큰입니다.");
		}

		// 존재하지 않는 가맹점이면 MerchantService가 MerchantNotFoundException을 던진다.
		merchantService.getMerchant(merchantId);

		// USED 전환에 실패했다면(동시에 두 번 완료 요청이 들어온 경쟁 상황) 위 상태 체크를 통과한 직후라도
		// 여기서 다시 막힌다 - 이 경우도 "이미 처리됨"으로 응답한다.
		paymentTokenStore.markUsedIfIssued(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotUsableException("이미 처리되었거나 완료할 수 없는 결제 토큰입니다."));

		PaymentVO payment = PaymentVO.builder()
			.merchantId(merchantId)
			.userCardId(token.getUserCardId())
			.paymentTime(LocalDateTime.now(ZONE))
			.originalAmount(originalAmount)
			.discountAmount(safeDiscountAmount)
			.finalAmount(originalAmount.subtract(safeDiscountAmount))
			.paymentStatus(STATUS_APPROVED)
			.paymentMethod(PAYMENT_METHOD_BARCODE)
			.build();

		paymentMapper.insertPayment(payment);

		return paymentMapper.findByPaymentId(payment.getPaymentId())
			.map(PaymentHistoryResponseDto::from)
			.orElseThrow(() -> new IllegalStateException("결제 생성 직후 조회에 실패했습니다."));
	}
}