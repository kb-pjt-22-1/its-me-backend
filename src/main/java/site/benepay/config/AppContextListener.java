package site.benepay.config;

import site.benepay.service.OutboxDeliveryService;
import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.*;

@WebListener
public class AppContextListener implements ServletContextListener {
    private ScheduledExecutorService scheduler;
    @Override public void contextInitialized(ServletContextEvent sce){
        DatabaseInitializer.initialize();
        sce.getServletContext().setAttribute("appName","BenePay Payment Demo");
        scheduler=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"benepay-outbox-worker");t.setDaemon(true);return t;});
        final OutboxDeliveryService service=new OutboxDeliveryService();
        scheduler.scheduleWithFixedDelay(()->service.retryPending(20),5,10,TimeUnit.SECONDS);
    }
    @Override public void contextDestroyed(ServletContextEvent sce){if(scheduler!=null)scheduler.shutdownNow();}
}
