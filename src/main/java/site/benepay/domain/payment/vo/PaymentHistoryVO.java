package site.benepay.domain.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * payments를 merchants/user_cards/cards와 조인한 read model. 가맹점명·카드명처럼 화면에
 * 바로 보여줄 수 있는 값까지 함께 담는다. 결제 내역 목록 조회와 단건 상세 조회 둘 다 이걸 쓴다.
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
	private Long userCardId;
	@NonNull
	private String merchantName;
	// 결제 이벤트(PaymentApprovedEvent/PaymentCanceledEvent)에 실어 보내는 용도.
	// benefit 도메인이 카테고리별 혜택 사용액(user_card_benefit_monthly_status)을
	// 갱신할 때 필요하다.
	@NonNull
	private String categoryCode;
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
	// 이 결제에 적용된 혜택의 serviceName. 적용된 혜택이 없으면(할인 0원) null - benefit
	// 도메인이 card_benefit_monthly_usage를 갱신할 때(BenefitUsageEventHandler) 키로 쓴다.
	private String benefitServiceName;
	@NonNull
	private String paymentStatus;
	@NonNull
	private String paymentMethod;
}
