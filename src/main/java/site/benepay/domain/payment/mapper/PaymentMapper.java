package site.benepay.domain.payment.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.payment.vo.PaymentHistoryVO;
import site.benepay.domain.payment.vo.PaymentVO;
import site.benepay.domain.payment.vo.UserCardPaymentTokenVO;

public interface PaymentMapper {

	int insertPayment(PaymentVO payment);

	// merchants/user_cards/cards와 조인해 화면 표시용 값까지 채워서 단건 조회
	Optional<PaymentHistoryVO> findByPaymentId(@Param("paymentId") Long paymentId);

	// 위와 같은 조인으로 이 사용자 소유 카드의 결제 내역을 최신순으로 조회.
	// yearMonth(yyyyMM)가 있으면 그 달만, null이면 전체 조회.
	List<PaymentHistoryVO> findPaymentHistoryByUserId(@Param("userId") Long userId,
		@Param("yearMonth") String yearMonth);

	// user_id + user_card_id + status='ACTIVE' + 미삭제 조건이라, 소유권/활성 상태 검증을 겸한다.
	// 소유하지 않았거나 비활성/삭제된 카드면 빈 값이 온다.
	Optional<UserCardPaymentTokenVO> findActiveCardPaymentToken(@Param("userId") Long userId,
		@Param("userCardId") Long userCardId);

	// 결제완료 버튼엔 가맹점 정보가 없어서(실제 스캔이 불가능한 구조), 데모용으로 실제 존재하는
	// 가맹점 중 하나를 무작위로 골라 쓴다.
	Optional<Long> findRandomMerchantId();

	// user_cards로 소유권을 확인하고 status='APPROVED'인 것만 CANCELED로 바꾼다 (승인된 결제만 취소 가능).
	// 소유하지 않았거나, 없거나, 이미 APPROVED가 아니면 0을 반환한다.
	int cancelApprovedPayment(@Param("userId") Long userId, @Param("paymentId") Long paymentId);

	// 발급 시점에 merchantId를 넘겨받았을 때 존재 여부만 확인한다. MerchantService에 단건 조회가
	// 없어서(카테고리/좌표 기반 목록 조회만 있음), payment 도메인 자체 쿼리로 확인한다.
	boolean existsMerchant(@Param("merchantId") Long merchantId);
}