package site.benepay.domain.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// payments 테이블에 행을 쓸 때 쓰는 VO. 조회는 PaymentHistoryVO(조인된 read model)를 쓰고,
// 이건 결제완료 시점에 INSERT하기 위한 용도로만 쓴다.
//
// MyBatis는 기본 생성자로 인스턴스를 만든 뒤 setter 없이도 필드에 직접 리플렉션으로 값을 채운다.
// Builder는 Service에서 직접 값을 채워 만들 때 쓴다.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVO {

	private Long paymentId;
	@NonNull
	private Long merchantId;
	@NonNull
	private Long userCardId;
	@NonNull
	private LocalDateTime paymentTime;
	@NonNull
	private BigDecimal originalAmount;
	@NonNull
	private BigDecimal discountAmount;
	@NonNull
	private BigDecimal finalAmount;
	// payment_status: PENDING, APPROVED, CANCELED, PAYMENT_FAILED (common_codes 그룹 PAYMENT_STATUS)
	@NonNull
	private String paymentStatus;
	// payment_method: BARCODE, QR (common_codes 그룹 PAYMENT_METHOD)
	@NonNull
	private String paymentMethod;
}
