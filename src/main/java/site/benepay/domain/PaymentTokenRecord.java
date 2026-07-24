package site.benepay.domain;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class PaymentTokenRecord {
 private long paymentTokenId,userId,userCardId; private Long merchantId; private BigDecimal expectedAmount; private String tokenStatus; private LocalDateTime expiresAt,usedAt;
 public long getPaymentTokenId(){return paymentTokenId;} public void setPaymentTokenId(long v){paymentTokenId=v;}
 public long getUserId(){return userId;} public void setUserId(long v){userId=v;}
 public long getUserCardId(){return userCardId;} public void setUserCardId(long v){userCardId=v;}
 public Long getMerchantId(){return merchantId;} public void setMerchantId(Long v){merchantId=v;}
 public BigDecimal getExpectedAmount(){return expectedAmount;} public void setExpectedAmount(BigDecimal v){expectedAmount=v;}
 public String getTokenStatus(){return tokenStatus;} public void setTokenStatus(String v){tokenStatus=v;}
 public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime v){expiresAt=v;}
 public LocalDateTime getUsedAt(){return usedAt;} public void setUsedAt(LocalDateTime v){usedAt=v;}
}
