package site.benepay.service;

import site.benepay.config.Database;
import site.benepay.domain.OutboxEvent;
import site.benepay.repository.OutboxRepository;
import java.sql.Connection;
import java.util.List;

public class OutboxDeliveryService {
    private final OutboxRepository repository = new OutboxRepository();
    private final CardPerformanceClient client = new CardPerformanceClient();

    public void deliver(long paymentId, String eventType) {
        try (Connection c = Database.getConnection()) {
            OutboxEvent event = repository.findByPaymentEvent(c, paymentId, eventType);
            if (event == null || "SENT".equals(event.getDeliveryStatus())) return;
            dispatch(event);
        } catch (Exception e) {
            System.err.println("[BenePay] immediate outbox delivery failed: " + e.getMessage());
        }
    }

    public void retryPending(int limit) {
        try (Connection c = Database.getConnection()) {
            List<OutboxEvent> events = repository.findRetryable(c, limit);
            for (OutboxEvent event : events) dispatch(event);
        } catch (Exception e) {
            System.err.println("[BenePay] outbox retry scan failed: " + e.getMessage());
        }
    }

    private void dispatch(OutboxEvent event) {
        try {
            client.deliver(event.getPaymentId(), event.getEventType());
            try (Connection c = Database.getConnection()) { repository.markSent(c, event.getOutboxId()); }
        } catch (Exception e) {
            try (Connection c = Database.getConnection()) {
                repository.markFailed(c, event.getOutboxId(), event.getRetryCount() + 1, e.getMessage());
            } catch (Exception ignored) { }
        }
    }
}
