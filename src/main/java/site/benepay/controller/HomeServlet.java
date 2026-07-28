package site.benepay.controller;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import site.benepay.service.HomeService;
import site.benepay.util.DemoUser;

@WebServlet("/api/v1/home")
public class HomeServlet extends BaseApiServlet {
	private final HomeService homeService = new HomeService();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			writeSuccess(response, homeService.load(DemoUser.USER_ID));
		} catch (Exception e) {
			handleError(response, e);
		}
	}
}
