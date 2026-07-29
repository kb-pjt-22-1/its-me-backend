package site.benepay.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CardDetailResponseDto {

    private Long userCardId;
    private Long cardId;

    private String cardName;
    private String cardType;
    private String cardImageUrl;
    private String description;

    private String cardNetwork;
    private Long annualFee;

    private String maskedCardNumber;
    private LocalDate tokenExpiryDate;
    private String status;

    private Boolean primary;
    private Boolean recommendationEnabled;
    private Boolean supported;

    private Long minBenefitAmount;
}