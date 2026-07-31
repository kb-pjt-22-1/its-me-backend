package site.benepay.domain.card.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

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

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate tokenExpiryDate;
	private String status;

	private Boolean primary;
	private Boolean recommendationEnabled;
	private Boolean supported;

	private Long minBenefitAmount;
}
