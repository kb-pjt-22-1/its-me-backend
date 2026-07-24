package site.benepay.service;

import site.benepay.domain.BenefitResult;
import site.benepay.domain.CardBenefit;
import site.benepay.domain.Merchant;
import site.benepay.domain.MonthlyStatus;
import site.benepay.domain.UserCard;
import site.benepay.repository.BenefitRepository;
import site.benepay.repository.MonthlyStatusRepository;
import site.benepay.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BenefitCalculationService {
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final BenefitRepository benefitRepository = new BenefitRepository();
    private final MonthlyStatusRepository monthlyStatusRepository = new MonthlyStatusRepository();

    public BenefitResult calculate(Connection connection, UserCard card, Merchant merchant, BigDecimal amount) throws Exception {
        String targetYearMonth = YearMonth.now().format(YEAR_MONTH);
        MonthlyStatus status = monthlyStatusRepository.find(connection, card.getUserCardId(), targetYearMonth);
        if (card.getMinBenefitAmount().compareTo(BigDecimal.ZERO) > 0 && !status.isBenefitEligible()) {
            BenefitResult result = noBenefit(amount);
            result.setBenefitName("전월 실적 미충족");
            result.setDescription("현재 카드의 혜택 적용 기준 금액을 충족하지 못했습니다.");
            return result;
        }

        List<CardBenefit> benefits = benefitRepository.findApplicable(
                connection, card.getUserCardId(), card.getCardId(), merchant);
        BenefitResult best = noBenefit(amount);
        for (CardBenefit benefit : benefits) {
            BenefitResult candidate = calculateBenefit(benefit, status, amount);
            if (candidate.getBenefitAmount().compareTo(best.getBenefitAmount()) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    public BenefitResult calculateBenefit(CardBenefit benefit, MonthlyStatus status, BigDecimal amount) {
        BigDecimal normalizedAmount = MoneyUtil.won(amount);
        BigDecimal rate = zeroIfNull(benefit.getRatePercent());
        BigDecimal fixed = zeroIfNull(benefit.getFixedAmount());
        BigDecimal calculated = normalizedAmount.multiply(rate)
                .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                .add(fixed);

        BigDecimal monthlyLimit = zeroIfNull(benefit.getMonthlyLimit());
        boolean rewardType = "POINT".equalsIgnoreCase(benefit.getBenefitType()) ||
                "CASHBACK".equalsIgnoreCase(benefit.getBenefitType());
        if (monthlyLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal alreadyUsed = rewardType
                    ? zeroIfNull(status.getRewardUsed())
                    : zeroIfNull(status.getDiscountUsed());
            BigDecimal remaining = MoneyUtil.max(monthlyLimit.subtract(alreadyUsed), BigDecimal.ZERO);
            calculated = MoneyUtil.min(calculated, remaining);
        }

        calculated = MoneyUtil.max(MoneyUtil.won(calculated), BigDecimal.ZERO);
        BenefitResult result = new BenefitResult();
        result.setBenefitId(benefit.getBenefitId());
        result.setBenefitType(benefit.getBenefitType());
        result.setBenefitName(benefit.getBenefitName());
        result.setDescription(benefit.getDescription());
        result.setBenefitAmount(calculated);

        if (rewardType) {
            result.setRewardAmount(calculated);
            result.setDiscountAmount(BigDecimal.ZERO);
            result.setFinalAmount(normalizedAmount);
        } else {
            BigDecimal discount = MoneyUtil.min(calculated, normalizedAmount);
            result.setDiscountAmount(discount);
            result.setRewardAmount(BigDecimal.ZERO);
            result.setFinalAmount(normalizedAmount.subtract(discount));
        }
        return result;
    }

    public BenefitResult noBenefit(BigDecimal amount) {
        BenefitResult result = new BenefitResult();
        result.setBenefitName("적용 혜택 없음");
        result.setDescription("해당 결제에 적용 가능한 할인·적립 혜택이 없습니다.");
        result.setBenefitAmount(BigDecimal.ZERO);
        result.setDiscountAmount(BigDecimal.ZERO);
        result.setRewardAmount(BigDecimal.ZERO);
        result.setFinalAmount(MoneyUtil.won(amount));
        return result;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
