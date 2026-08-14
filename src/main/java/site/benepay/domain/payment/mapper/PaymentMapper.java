package site.benepay.domain.payment.mapper;

import java.time.LocalDateTime;
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
	// startPaymentTime/endPaymentTime이 있으면 그 구간만(끝은 미포함), 둘 다 null이면 전체 조회.
	// yyyyMM 문자열 대신 날짜 범위를 받는 이유: payment_time 컬럼을 DATE_FORMAT() 등으로 감싸면
	// 인덱스를 못 타서(non-sargable) 결제가 많아질수록 이 쿼리 자체가 느려진다 - 정확히 이 필터를
	// 추가한 목적(쿼리 단 효율화)과 어긋나서, 컬럼은 그대로 두고 범위 비교로 바꿨다.
	List<PaymentHistoryVO> findPaymentHistoryByUserId(@Param("userId") Long userId,
		@Param("startPaymentTime") LocalDateTime startPaymentTime,
		@Param("endPaymentTime") LocalDateTime endPaymentTime);

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