package site.benepay.controller;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.config.Database;
import site.benepay.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() throws Exception {
        try (Connection ignored = Database.getConnection()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "UP");
            data.put("service", "benepay-payment-spring-jsp");
            data.put("framework", "Spring MVC + JSP");
            data.put("database", Database.getUrl().startsWith("jdbc:h2:") ? "H2" : "MySQL");
            return ApiResponse.success(data);
        }
    }
}
