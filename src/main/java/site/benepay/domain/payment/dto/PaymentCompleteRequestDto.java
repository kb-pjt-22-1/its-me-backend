package site.benepay.domain.payment.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 실제로는 매장 POS가 바코드를 스캔한 뒤 입력하는 값들. 스캔 기능이 없어서 프론트의
// "결제완료하기" 버튼이 이 값들을 직접 실어 보내는 걸로 시뮬레이션한다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompleteRequestDto {

	@NotNull(message = "가맹점 아이디는 필수입니다.")
	private Long merchantId;

	@NotNull(message = "결제 금액은 필수입니다.")
	@DecimalMin(value = "0", inclusive = false, message = "결제 금액은 0보다 커야 합니다.")
	private BigDecimal originalAmount;

	// 미전달 시 0으로 처리
	@DecimalMin(value = "0", message = "할인 금액은 0 이상이어야 합니다.")
	private BigDecimal discountAmount;
}
