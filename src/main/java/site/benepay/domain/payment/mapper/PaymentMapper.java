package site.benepay.domain.payment.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.payment.vo.PaymentHistoryVO;

public interface PaymentMapper {

	// merchants/user_cards/cards와 조인해 화면 표시용 값까지 채워서 단건 조회
	Optional<PaymentHistoryVO> findByPaymentId(@Param("paymentId") Long paymentId);

	// 위와 같은 조인으로 이 사용자 소유 카드의 결제 내역을 최신순으로 조회
	List<PaymentHistoryVO> findPaymentHistoryByUserId(@Param("userId") Long userId);
}
