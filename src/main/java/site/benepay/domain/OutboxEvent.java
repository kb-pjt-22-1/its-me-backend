package site.benepay.domain;

import java.time.LocalDateTime;

public class OutboxEvent {
	private long outboxId;
	private long paymentId;
	private String eventType;
	private String payloadJson;
	private String deliveryStatus;
	private int retryCount;
	private LocalDateTime nextRetryAt;

	public long getOutboxId() {
		return outboxId;
	}

	public void setOutboxId(long outboxId) {
		this.outboxId = outboxId;
	}

	public long getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(long paymentId) {
		this.paymentId = paymentId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	public String getDeliveryStatus() {
		return deliveryStatus;
	}

	public void setDeliveryStatus(String deliveryStatus) {
		this.deliveryStatus = deliveryStatus;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public LocalDateTime getNextRetryAt() {
		return nextRetryAt;
	}

	public void setNextRetryAt(LocalDateTime nextRetryAt) {
		this.nextRetryAt = nextRetryAt;
	}
}
