package site.benepay.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import site.benepay.service.OutboxDeliveryService;

/**
 * ServletContextListener가 하던 DB 초기화와 Outbox 재처리를 Spring 생명주기로 이전한다.
 */
public class PaymentApplicationLifecycle implements InitializingBean, DisposableBean {
    private final OutboxDeliveryService outboxDeliveryService;
    private ScheduledExecutorService scheduler;

    public PaymentApplicationLifecycle(OutboxDeliveryService outboxDeliveryService) {
        this.outboxDeliveryService = outboxDeliveryService;
    }

    @Override
    public void afterPropertiesSet() {
        DatabaseInitializer.initialize();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "benepay-outbox-worker");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(
            () -> outboxDeliveryService.retryPending(20),
            5,
            10,
            TimeUnit.SECONDS
        );
    }

    @Override
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
