package site.benepay.service;

import site.benepay.config.AppConfig;
import site.benepay.util.JsonUtil;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class CardPerformanceClient {
    private final CardPerformanceService localService = new CardPerformanceService();

    public void deliver(long paymentId, String eventType) {
        String baseUrl = AppConfig.get("card.service.baseUrl", "local");
        if (baseUrl.isEmpty() || "local".equalsIgnoreCase(baseUrl)) {
            localService.apply(paymentId, eventType);
            return;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl.replaceAll("/$", "") + "/internal/api/v1/card-performance-events");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("X-Internal-Key", AppConfig.get("internal.api.key", "benepay-demo-key"));
            Map<String,Object> payload = new LinkedHashMap<String,Object>();
            payload.put("paymentId", paymentId); payload.put("eventType", eventType);
            byte[] bytes = JsonUtil.gson().toJson(payload).getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Card service HTTP status=" + status);
        } catch (Exception e) {
            throw new ServiceException(503, "CARD_SERVICE_UNAVAILABLE", "카드 서비스 전달에 실패했습니다.", e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
