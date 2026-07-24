package site.benepay.controller;

import site.benepay.config.Database;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/v1/health")
public class HealthServlet extends BaseApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (Connection ignored = Database.getConnection()) {
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("status", "UP");
            data.put("service", "benepay-payment");
            data.put("database", Database.getUrl().startsWith("jdbc:h2:") ? "H2" : "MySQL");
            writeSuccess(response, data);
        } catch (Exception e) {
            handleError(response, e);
        }
    }
}
