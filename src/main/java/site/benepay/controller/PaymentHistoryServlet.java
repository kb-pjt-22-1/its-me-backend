package site.benepay.controller;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import site.benepay.service.PaymentQueryService;
import site.benepay.service.ServiceException;
import site.benepay.util.DemoUser;

@WebServlet("/api/v1/payments/*")
public class PaymentHistoryServlet extends BaseApiServlet {
    private final PaymentQueryService paymentQueryService = new PaymentQueryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String pathInfo = request.getPathInfo();

            if (pathInfo == null || "/".equals(pathInfo)) {
                String yearMonth = request.getParameter("yearMonth");
                writeSuccess(response, paymentQueryService.findMonthly(DemoUser.USER_ID, yearMonth));
                return;
            }

            String paymentIdText = pathInfo.substring(1);
            if (paymentIdText.trim().isEmpty() || paymentIdText.contains("/"))
                throw new ServiceException(400, "INVALID_PAYMENT_ID", "결제 ID가 올바르지 않습니다.");

            long paymentId = parseLong(paymentIdText, "paymentId");
            writeSuccess(response, paymentQueryService.findDetail(DemoUser.USER_ID, paymentId));
        } catch (Exception e) {
            handleError(response, e);
        }
    }
}
