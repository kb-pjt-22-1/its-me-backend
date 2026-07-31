package site.benepay.domain.recommendation.dto.request;

import java.math.BigDecimal;

public class BenefitStoreDetailRequestDto {

    private BigDecimal estimatedPaymentAmount;
    private Double latitude;
    private Double longitude;

    public BigDecimal getEstimatedPaymentAmount() {
        return estimatedPaymentAmount;
    }

    public void setEstimatedPaymentAmount(BigDecimal estimatedPaymentAmount) {
        this.estimatedPaymentAmount = estimatedPaymentAmount;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
