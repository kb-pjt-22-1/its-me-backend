package site.benepay.domain.payment.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.payment.vo.PaymentVO;

public interface PaymentMapper {

	int insertPayment(PaymentVO payment);

	Optional<PaymentVO> findByPaymentId(@Param("paymentId") Long paymentId);

	int updatePaymentStatus(@Param("paymentId") Long paymentId, @Param("paymentStatus") String paymentStatus);

	// user_cards와 조인해서 이 사용자 소유 카드로 결제된 내역을 최신순으로 가져온다.
	List<PaymentVO> findPaymentsByUserId(@Param("userId") Long userId);
}
