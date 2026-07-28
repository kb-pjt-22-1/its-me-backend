package site.benepay.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import site.benepay.config.Database;
import site.benepay.domain.PaymentView;
import site.benepay.repository.PaymentRepository;

public class PaymentQueryService {
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public Map<String, Object> findMonthly(long userId, String yearMonthValue) {
        YearMonth yearMonth = parseYearMonth(yearMonthValue);

        try (Connection connection = Database.getConnection()) {
            List<PaymentView> payments = paymentRepository.findByUserAndPeriod(
                connection,
                userId,
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay()
            );

            BigDecimal totalPaymentAmount = BigDecimal.ZERO;
            BigDecimal totalBenefitAmount = BigDecimal.ZERO;

            for (PaymentView payment : payments) {
                if (!"APPROVED".equals(payment.getPaymentStatus()))
                    continue;

                totalPaymentAmount = totalPaymentAmount.add(zeroIfNull(payment.getFinalAmount()));
                totalBenefitAmount = totalBenefitAmount
                    .add(zeroIfNull(payment.getDiscountAmount()))
                    .add(zeroIfNull(payment.getRewardAmount()));
            }

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("yearMonth", yearMonth.toString());
            result.put("totalPaymentAmount", totalPaymentAmount);
            result.put("totalBenefitAmount", totalBenefitAmount);
            result.put("payments", payments);
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "PAYMENT_HISTORY_FAILED", "결제 내역 조회에 실패했습니다.", e);
        }
    }

    public PaymentView findDetail(long userId, long paymentId) {
        try (Connection connection = Database.getConnection()) {
            PaymentView payment = paymentRepository.findByIdAndUserId(connection, paymentId, userId);
            if (payment == null)
                throw new ServiceException(404, "PAYMENT_NOT_FOUND", "결제 내역을 찾을 수 없습니다.");
            return payment;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "PAYMENT_DETAIL_FAILED", "결제 상세 조회에 실패했습니다.", e);
        }
    }

    private YearMonth parseYearMonth(String value) {
        try {
            return value == null || value.trim().isEmpty() ? YearMonth.now() : YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new ServiceException(400, "INVALID_YEAR_MONTH", "yearMonth는 yyyy-MM 형식이어야 합니다.", e);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
