package site.benepay.domain.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * QR/바코드 결제용 단명 토큰. payments 테이블처럼 DB에 저장되는 게 아니라
 * Redis에 JSON으로 저장된다 (site.benepay.domain.user.service.SignupVerificationStore와 동일한 패턴).
 *
 * <p>merchantId는 nullable이다: 매장 페이지 → 카드 페이지로 넘어온 흐름이면 발급 시점에 이미 채워지고,
 * 결제 페이지로 바로 들어온 흐름이면 null로 남아있다가 결제완료 시점에 서버가 무작위로 채운다.
 *
 * <p>issuedAt을 LocalDateTime이 아니라 String(ISO-8601)으로 두는 이유: 이 프로젝트의
 * ObjectMapper 빈(JacksonConfig)에 JavaTimeModule이 등록되어 있지 않아, LocalDateTime을
 * 그대로 직렬화하면 Redis 저장 시점에 실패한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTokenVO {

	private String paymentTokenId;
	@NonNull
	private Long userId;
	@NonNull
	private Long userCardId;
	// 매장 페이지를 거쳐온 흐름이면 채워져 있고, 결제 페이지 직접 진입이면 null.
	private Long merchantId;
	// user_cards.payment_token에서 가져온 실제 카드 결제 토큰. 바코드에 실제로 실리는 값.
	@NonNull
	private String cardPaymentToken;
	// payment_method: BARCODE, QR (common_codes 그룹 PAYMENT_METHOD)
	@NonNull
	private String paymentMethod;
	@NonNull
	private String status;
	@NonNull
	private String issuedAt;
}
