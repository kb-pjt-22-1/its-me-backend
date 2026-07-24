package site.benepay.controller;

import site.benepay.config.AppConfig;
import site.benepay.service.CardPerformanceService;
import site.benepay.service.ServiceException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/internal/api/v1/card-performance-events")
public class CardPerformanceInternalServlet extends BaseApiServlet {
    private final CardPerformanceService service = new CardPerformanceService();
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String supplied=request.getHeader("X-Internal-Key");
            String expected=AppConfig.get("internal.api.key","benepay-demo-key");
            if(!expected.equals(supplied)) throw new ServiceException(401,"INVALID_INTERNAL_KEY","내부 API 인증에 실패했습니다.");
            EventRequest body=readJson(request,EventRequest.class);
            boolean applied=service.apply(body.paymentId,body.eventType);
            Map<String,Object> result=new LinkedHashMap<String,Object>();result.put("applied",applied);result.put("paymentId",body.paymentId);result.put("eventType",body.eventType);
            writeSuccess(response,result);
        } catch(Exception e){handleError(response,e);}
    }
    private static class EventRequest { long paymentId; String eventType; }
}
