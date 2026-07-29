package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CardVO {

    private Long cardId;
    private String cardName;
    private String cardType;
    private String cardVariantId;
    private Long annualFee;
    private String cardImageUrl;
    private String description;
    private Boolean supported;
    private BigDecimal minBenefitAmount;
    private String benefitsInfo;
}