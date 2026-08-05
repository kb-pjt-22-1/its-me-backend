package site.benepay.domain.payment.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.PaymentTokenNotFoundException;
import site.benepay.common.exception.PaymentTokenNotUsableException;
import site.benepay.domain.payment.dto.PaymentCreateRequestDto;
import site.benepay.domain.payment.dto.PaymentResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.vo.PaymentTokenVO;

@Service
@RequiredArgsConstructor
public class PaymentTokenServiceImpl implements PaymentTokenService {

	// 토큰엔 카드/가맹점/금액만 있고 결제수단 구분이 없어서 우선 고정값으로 둔다.
	// 나중에 결제수단을 여러 개 지원하게 되면 발급 요청에 파라미터로 받아야 함.
	private static final String DEFAULT_PAYMENT_METHOD = "CARD";
	private static final String COMPLETED_STATUS = "COMPLETED";

	private final PaymentTokenStore paymentTokenStore;
	private final PaymentService paymentService;

	@Override
	public PaymentTokenResponseDto issueToken(Long userId, Long userCardId, Long merchantId,
		BigDecimal originalAmount, BigDecimal discountAmount) {
		// TODO: userCardId가 userId 소유의 활성 카드인지 검증하는 로직이 아직 없음
		// (card 도메인 CardMapper.existsActiveUserCard와 동일한 검증 필요, 연동 방식은 논의 필요)
		PaymentTokenVO token = paymentTokenStore.issue(userId, userCardId, merchantId, originalAmount, discountAmount);

		return PaymentTokenResponseDto.from(token);
	}

	@Override
	public PaymentTokenResponseDto getTokenStatus(String paymentTokenId) {
		PaymentTokenVO token = findTokenOrThrow(paymentTokenId);

		return PaymentTokenResponseDto.from(token);
	}

	@Override
	public PaymentResponseDto completeToken(String paymentTokenId) {
		PaymentTokenVO token = findTokenOrThrow(paymentTokenId);

		if (!PaymentTokenStore.STATUS_ISSUED.equals(token.getStatus())) {
			throw new PaymentTokenNotUsableException("이미 처리되었거나 완료할 수 없는 결제 토큰입니다.");
		}

		// USED 전환에 실패했다면(동시에 두 번 완료 요청이 들어온 경쟁 상황) 위 상태 체크를 통과한 직후라도
		// 여기서 다시 막힌다 - 이 경우도 "이미 처리됨"으로 응답한다.
		paymentTokenStore.markUsedIfIssued(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotUsableException("이미 처리되었거나 완료할 수 없는 결제 토큰입니다."));

		PaymentCreateRequestDto createRequest = new PaymentCreateRequestDto(
			token.getMerchantId(),
			token.getUserCardId(),
			token.getOriginalAmount(),
			token.getDiscountAmount(),
			DEFAULT_PAYMENT_METHOD
		);
		PaymentResponseDto created = paymentService.createPayment(createRequest);

		return paymentService.updatePaymentStatus(created.getPaymentId(), COMPLETED_STATUS);
	}

	private PaymentTokenVO findTokenOrThrow(String paymentTokenId) {
		return paymentTokenStore.find(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotFoundException("결제 토큰을 찾을 수 없거나 만료되었습니다."));
	}
}
