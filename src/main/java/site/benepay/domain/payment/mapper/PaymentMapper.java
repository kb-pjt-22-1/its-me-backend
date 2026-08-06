package site.benepay.domain.payment.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.payment.vo.PaymentHistoryVO;
import site.benepay.domain.payment.vo.PaymentVO;

public interface PaymentMapper {

	Optional<PaymentVO> findByPaymentId(@Param("paymentId") Long paymentId);

	// user_cards로 소유자를 확인하고, merchants/cards와 조인해 화면 표시용 값까지 채워서 최신순으로 반환한다.
	List<PaymentHistoryVO> findPaymentHistoryByUserId(@Param("userId") Long userId);
}
