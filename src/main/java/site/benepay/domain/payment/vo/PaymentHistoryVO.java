package site.benepay.domain.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 결제 내역 목록 조회 전용 read model. payments를 merchants/user_cards/cards와 조인해서
 * 가맹점명·카드명처럼 화면에 바로 보여줄 수 있는 값까지 함께 담는다.
 * payments 테이블 컬럼만 그대로 담는 {@link PaymentVO}와는 별도로 둔다.
 *
 * MyBatis는 기본 생성자로 인스턴스를 만든 뒤 setter 없이도 필드에 직접 리플렉션으로 값을 채운다.
 * Builder는 테스트 등 코드에서 직접 값을 채워 만들 때 쓴다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryVO {

	private Long paymentId;
	@NonNull
	private String merchantName;
	@NonNull
	private String cardName;
	// 카드 마스킹 표시용 (카드번호 자체가 아니라 user_cards.pan_last4)
	@NonNull
	private String panLast4;
	@NonNull
	private LocalDateTime paymentTime;
	@NonNull
	private BigDecimal originalAmount;
	@NonNull
	private BigDecimal discountAmount;
	@NonNull
	private BigDecimal finalAmount;
	@NonNull
	private String paymentStatus;
	@NonNull
	private String paymentMethod;
}
