package site.benepay.domain.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QR/바코드 결제용 단명 토큰. DB 테이블이 아니라 Redis에 JSON으로 저장된다
 * (site.benepay.domain.user.service.SignupVerificationStore와 동일한 패턴).
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
	private Long userId;
	private Long userCardId;
	private String status;
	private String issuedAt;
}
