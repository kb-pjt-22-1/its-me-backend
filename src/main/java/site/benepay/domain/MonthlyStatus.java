package site.benepay.domain;

import java.math.BigDecimal;

public class MonthlyStatus {
    private BigDecimal spentAmount = BigDecimal.ZERO;
    private BigDecimal discountUsed = BigDecimal.ZERO;
    private BigDecimal rewardUsed = BigDecimal.ZERO;
    private boolean benefitEligible;

    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    public BigDecimal getDiscountUsed() { return discountUsed; }
    public void setDiscountUsed(BigDecimal discountUsed) { this.discountUsed = discountUsed; }
    public BigDecimal getRewardUsed() { return rewardUsed; }
    public void setRewardUsed(BigDecimal rewardUsed) { this.rewardUsed = rewardUsed; }
    public boolean isBenefitEligible() { return benefitEligible; }
    public void setBenefitEligible(boolean benefitEligible) { this.benefitEligible = benefitEligible; }
}
