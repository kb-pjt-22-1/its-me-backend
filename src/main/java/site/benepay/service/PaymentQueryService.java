package site.benepay.service;

import java.sql.Connection;
import java.util.List;

import site.benepay.config.Database;
import site.benepay.domain.PaymentView;
import site.benepay.repository.PaymentRepository;

public class PaymentQueryService {
	private final PaymentRepository paymentRepository = new PaymentRepository();

	public List<PaymentView> findAll(long userId) {
		try (Connection connection = Database.getConnection()) {
			return paymentRepository.findByUser(connection, userId);
		} catch (Exception e) {
			throw new ServiceException(500, "PAYMENT_HISTORY_FAILED", "결제 내역 조회에 실패했습니다.", e);
		}
	}
}
