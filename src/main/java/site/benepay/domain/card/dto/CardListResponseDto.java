package site.benepay.domain.card.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class CardListResponseDto {

    private Long userCardId;
    private Long cardId;

    private String cardName;
    private String cardType;
    private String cardImageUrl;

    private String cardNetwork;
    private Long annualFee;

    private String panLast4;
    private String status;

    private Boolean primary;
    private Boolean recommendationEnabled;

}