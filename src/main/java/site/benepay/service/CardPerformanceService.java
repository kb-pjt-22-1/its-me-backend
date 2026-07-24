package site.benepay.service;

import site.benepay.config.Database;
import site.benepay.domain.PaymentView;
import site.benepay.repository.CardPerformanceEventRepository;
import site.benepay.repository.MonthlyStatusRepository;
import site.benepay.repository.PaymentRepository;

import java.sql.Connection;
import java.time.format.DateTimeFormatter;

public class CardPerformanceService {
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final MonthlyStatusRepository monthlyStatusRepository = new MonthlyStatusRepository();
    private final CardPerformanceEventRepository eventRepository = new CardPerformanceEventRepository();

    public boolean apply(long paymentId, String eventType) {
        if (!"APPROVED".equals(eventType) && !"CANCELED".equals(eventType)) {
            throw new ServiceException(400, "INVALID_EVENT_TYPE", "처리할 수 없는 카드 실적 이벤트입니다.");
        }
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!eventRepository.insertIfAbsent(connection, paymentId, eventType)) {
                    connection.commit();
                    return false;
                }
                PaymentView payment = paymentRepository.findById(connection, paymentId);
                if (payment == null) throw new ServiceException(404, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다.");
                String month = payment.getPaymentTime().format(YEAR_MONTH);
                if ("APPROVED".equals(eventType)) {
                    monthlyStatusRepository.addPayment(connection, payment.getUserCardId(), month,
                            payment.getOriginalAmount(), payment.getDiscountAmount(), payment.getRewardAmount());
                } else {
                    monthlyStatusRepository.reversePayment(connection, payment.getUserCardId(), month,
                            payment.getOriginalAmount(), payment.getDiscountAmount(), payment.getRewardAmount());
                }
                connection.commit();
                return true;
            } catch (Exception e) {
                try { connection.rollback(); } catch (Exception ignored) { }
                throw e;
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "CARD_PERFORMANCE_FAILED", "카드 실적 반영에 실패했습니다.", e);
        }
    }
}
