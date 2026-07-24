package site.benepay.service;

import org.junit.jupiter.api.Test;
import site.benepay.domain.BenefitResult;
import site.benepay.domain.CardBenefit;
import site.benepay.domain.MonthlyStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BenefitCalculationServiceTest {
    private final BenefitCalculationService service = new BenefitCalculationService();

    @Test
    void monthlyRemainingLimitCapsDiscount() {
        CardBenefit benefit = benefit("DISCOUNT", "10", "5000", "30000");
        MonthlyStatus status = new MonthlyStatus();
        status.setDiscountUsed(new BigDecimal("29000"));

        BenefitResult result = service.calculateBenefit(benefit, status, new BigDecimal("15000"));

        assertEquals(new BigDecimal("1000"), result.getDiscountAmount());
        assertEquals(new BigDecimal("14000"), result.getFinalAmount());
    }

    @Test
    void pointBenefitDoesNotReduceFinalAmount() {
        CardBenefit benefit = benefit("POINT", "3", "3000", "20000");
        MonthlyStatus status = new MonthlyStatus();

        BenefitResult result = service.calculateBenefit(benefit, status, new BigDecimal("20000"));

        assertEquals(new BigDecimal("600"), result.getRewardAmount());
        assertEquals(new BigDecimal("20000"), result.getFinalAmount());
    }

    private CardBenefit benefit(String type, String rate, String perPayment, String monthly) {
        CardBenefit benefit = new CardBenefit();
        benefit.setBenefitId(1L);
        benefit.setBenefitType(type);
        benefit.setBenefitName("테스트 혜택");
        benefit.setDescription("테스트");
        benefit.setRatePercent(new BigDecimal(rate));
        benefit.setFixedAmount(BigDecimal.ZERO);
        benefit.setMonthlyLimit(new BigDecimal(monthly));
        return benefit;
    }
}
