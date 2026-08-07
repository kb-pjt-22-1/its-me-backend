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
 * <p>발급 시점엔 어느 카드로 결제할지만 정해져 있다. 가맹점/금액은 매장에서 바코드를 스캔한 뒤에야
 * 정해지는 걸로 흉내내는 흐름이라, 이 VO엔 아직 담지 않는다 (결제 확정 단계에서 별도로 받는다).
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
