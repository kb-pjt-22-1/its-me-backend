package site.benepay.domain.payment.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// merchantId는 선택값이다: 매장 페이지 → 카드 페이지로 넘어온 흐름이면 이미 아는 가맹점을 실어 보내고,
// 결제 페이지로 바로 들어온 흐름이면 비워둔다 (완료 시점에 서버가 데모용으로 무작위 생성).
//
// paymentMethod도 선택값이다: 생략하면 BARCODE로 처리한다(기존 프론트가 이 필드를 안 보내던
// 상태라 하위호환을 위해 기본값을 둔다). 값을 보내는 경우엔 BARCODE/QR만 허용한다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTokenCreateRequestDto {

	@NotNull(message = "결제에 사용할 카드는 필수입니다.")
	private Long userCardId;

	private Long merchantId;

	@Pattern(regexp = "^(BARCODE|QR)$", message = "paymentMethod는 BARCODE 또는 QR이어야 합니다.")
	private String paymentMethod;
}