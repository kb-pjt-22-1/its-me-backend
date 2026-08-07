package site.benepay.domain.payment.vo;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * user_cards에서 조회한 카드사 발급 결제 토큰. payments/payment_tokens 흐름과는 별개로,
 * "이 카드가 실제로 결제에 쓰는 토큰이 뭔지"만 담는 read model이다.
 * 이 조회 자체가 user_id + user_card_id + status='ACTIVE' 조건이라, 카드 소유권/활성 상태
 * 검증도 같이 이뤄진다 (별도 검증 로직 불필요).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCardPaymentTokenVO {

	private Long userCardId;
	// 카드사가 발급한, 실제 카드번호 대신 결제에 쓰는 토큰 (user_cards.payment_token)
	@NonNull
	private String paymentToken;
	@NonNull
	private LocalDate tokenExpiryDate;
}
