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

	// 위와 같은 조인으로 이 사용자 소유 카드의 결제 내역을 최신순으로 조회
	List<PaymentHistoryVO> findPaymentHistoryByUserId(@Param("userId") Long userId);

	// user_id + user_card_id + status='ACTIVE' + 미삭제 조건이라, 소유권/활성 상태 검증을 겸한다.
	// 소유하지 않았거나 비활성/삭제된 카드면 빈 값이 온다.
	Optional<UserCardPaymentTokenVO> findActiveCardPaymentToken(@Param("userId") Long userId,
		@Param("userCardId") Long userCardId);
}