package site.benepay.domain.payment.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.payment.vo.PaymentVO;

public interface PaymentMapper {

	int insertPayment(PaymentVO payment);

	Optional<PaymentVO> findByPaymentId(@Param("paymentId") Long paymentId);

	int updatePaymentStatus(@Param("paymentId") Long paymentId, @Param("paymentStatus") String paymentStatus);
}
