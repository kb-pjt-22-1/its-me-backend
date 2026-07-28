package site.benepay.controller;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import site.benepay.service.RecommendationService;
import site.benepay.util.DemoUser;

@WebServlet({"/api/v1/recommend", "/api/v1/payment-recommendations"})
public class RecommendationServlet extends BaseApiServlet {
	private final RecommendationService recommendationService = new RecommendationService();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		process(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			RecommendationRequest body = readJson(request, RecommendationRequest.class);
			writeSuccess(response, recommendationService.recommend(
				DemoUser.USER_ID, body.merchantId, body.amount));
		} catch (Exception e) {
			handleError(response, e);
		}
	}

	private void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			long merchantId = parseLong(request.getParameter("merchantId"), "merchantId");
			BigDecimal amount = new BigDecimal(request.getParameter("amount"));
			writeSuccess(response, recommendationService.recommend(DemoUser.USER_ID, merchantId, amount));
		} catch (Exception e) {
			handleError(response, e);
		}
	}

	private static class RecommendationRequest {
		long merchantId;
		BigDecimal amount;
	}
}
