package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardVariantVO {

    /**
     * CHAR(2)
     * 예: 01, 02, 03
     */
    private String cardVariantId;

    /**
     * DOMESTIC, VISA, MASTER 등
     */
    private String cardNetwork;
}