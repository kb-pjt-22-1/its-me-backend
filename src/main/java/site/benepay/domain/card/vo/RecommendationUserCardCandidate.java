package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecommendationUserCardCandidate {

    private Long userCardId;
    private Long cardId;
    private String cardName;
    private String cardImageUrl;
    private String cardLast4;
    private boolean primary;
    private String benefitType;
    private String benefitName;
    private String benefitDescription;
    private String benefitsInfo;
    private BigDecimal totalSpendingAmount;
}
