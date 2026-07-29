package site.benepay.domain.card.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CardMonthlyStatusVO {

    private Long cardMonthlyStatusId;
    private Long userCardId;
    private String targetYearMonth;
    private BigDecimal totalSpendingAmount;
    private LocalDateTime updatedAt;
}