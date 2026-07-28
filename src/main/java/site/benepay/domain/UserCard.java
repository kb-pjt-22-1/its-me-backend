package site.benepay.domain;

import java.math.BigDecimal;

public class UserCard {
	private long userCardId;
	private long userId;
	private long cardId;
	private String cardLast4;
	private boolean primary;
	private boolean recommendationEnabled;
	private String cardCompanyName;
	private String cardName;
	private String cardType;
	private String cardStatus;
	private BigDecimal minBenefitAmount = BigDecimal.ZERO;

	public long getUserCardId() {
		return userCardId;
	}

	public void setUserCardId(long userCardId) {
		this.userCardId = userCardId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getCardId() {
		return cardId;
	}

	public void setCardId(long cardId) {
		this.cardId = cardId;
	}

	public String getCardLast4() {
		return cardLast4;
	}

	public void setCardLast4(String cardLast4) {
		this.cardLast4 = cardLast4;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public boolean isRecommendationEnabled() {
		return recommendationEnabled;
	}

	public void setRecommendationEnabled(boolean recommendationEnabled) {
		this.recommendationEnabled = recommendationEnabled;
	}

	public String getCardCompanyName() {
		return cardCompanyName;
	}

	public void setCardCompanyName(String cardCompanyName) {
		this.cardCompanyName = cardCompanyName;
	}

	public String getCardName() {
		return cardName;
	}

	public void setCardName(String cardName) {
		this.cardName = cardName;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

	public String getCardStatus() {
		return cardStatus;
	}

	public void setCardStatus(String cardStatus) {
		this.cardStatus = cardStatus;
	}

	public BigDecimal getMinBenefitAmount() {
		return minBenefitAmount;
	}

	public void setMinBenefitAmount(BigDecimal minBenefitAmount) {
		this.minBenefitAmount = minBenefitAmount == null ? BigDecimal.ZERO : minBenefitAmount;
	}
}
