package site.benepay.domain.payment.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdateRequestDto {

	// 허용 상태값은 실제 상태 전이 정책이 정해지면 다시 확인 필요
	@NotBlank(message = "결제 상태는 필수입니다.")
	@Pattern(regexp = "^(PENDING|COMPLETED|FAILED|CANCELLED)$", message = "결제 상태 값이 올바르지 않습니다.")
	private String paymentStatus;
}
