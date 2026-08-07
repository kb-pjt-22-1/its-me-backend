package site.benepay.domain.payment.service;

import site.benepay.domain.payment.dto.PaymentHistoryResponseDto;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;

public interface PaymentTokenService {

	// merchantId는 nullable. 매장 페이지를 거쳐온 흐름이면 채워서 넘기고,
	// 결제 페이지 직접 진입이면 null로 넘긴다 (완료 시점에 서버가 무작위로 채운다).
	PaymentTokenResponseDto issueToken(Long userId, Long userCardId, Long merchantId);

	PaymentTokenResponseDto getTokenStatus(String paymentTokenId);

	// "결제완료하기" 버튼 → 결제 이벤트 생성 (payments 테이블에 실제 행을 만든다).
	// 발급 시점에 가맹점을 이미 알던 흐름이면 그 값을 그대로 쓰고, 몰랐던 흐름이면 데모용으로 무작위 생성한다.
	PaymentHistoryResponseDto completeToken(String paymentTokenId);

	// "취소하기" 버튼 → 아직 실제 결제가 일어난 적이 없어서 payments 테이블은 건드리지 않고
	// 토큰만 무효화한다.
	PaymentTokenResponseDto cancelToken(String paymentTokenId);
}