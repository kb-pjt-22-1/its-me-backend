package site.benepay.domain.payment.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import site.benepay.common.exception.PaymentTokenNotFoundException;
import site.benepay.domain.payment.dto.PaymentTokenResponseDto;
import site.benepay.domain.payment.vo.PaymentTokenVO;

@Service
@RequiredArgsConstructor
public class PaymentTokenServiceImpl implements PaymentTokenService {

	private final PaymentTokenStore paymentTokenStore;

	@Override
	public PaymentTokenResponseDto issueToken(Long userId, Long userCardId) {
		// TODO: userCardId가 userId 소유의 활성 카드인지 검증하는 로직이 아직 없음
		// (card 도메인 CardMapper.existsActiveUserCard와 동일한 검증 필요, 연동 방식은 논의 필요)
		PaymentTokenVO token = paymentTokenStore.issue(userId, userCardId);

		return PaymentTokenResponseDto.from(token);
	}

	@Override
	public PaymentTokenResponseDto getTokenStatus(String paymentTokenId) {
		PaymentTokenVO token = paymentTokenStore.find(paymentTokenId)
			.orElseThrow(() -> new PaymentTokenNotFoundException("결제 토큰을 찾을 수 없거나 만료되었습니다."));

		return PaymentTokenResponseDto.from(token);
	}
}
