package site.benepay.domain.payment.dto;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 발급 시점엔 결제할 카드만 정해져 있고, 가맹점/금액은 아직 모른다
// (실제 오프라인 결제처럼 매장에서 바코드를 스캔한 뒤에야 금액이 정해지는 흐름을 흉내낸다).
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTokenCreateRequestDto {

	@NotNull(message = "결제에 사용할 카드는 필수입니다.")
	private Long userCardId;
}
