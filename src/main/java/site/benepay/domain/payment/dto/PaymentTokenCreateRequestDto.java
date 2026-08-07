package site.benepay.domain.payment.dto;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// merchantId는 선택값이다: 매장 페이지 → 카드 페이지로 넘어온 흐름이면 이미 아는 가맹점을 실어 보내고,
// 결제 페이지로 바로 들어온 흐름이면 비워둔다 (완료 시점에 서버가 데모용으로 무작위 생성).
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTokenCreateRequestDto {

	@NotNull(message = "결제에 사용할 카드는 필수입니다.")
	private Long userCardId;

	private Long merchantId;
}
