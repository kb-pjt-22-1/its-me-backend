package site.benepay.domain.payment.dto;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentTokenCreateRequestDto {

	@NotNull(message = "결제에 사용할 카드는 필수입니다.")
	private Long userCardId;
}
