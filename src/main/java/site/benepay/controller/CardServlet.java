package site.benepay.controller;

import site.benepay.service.CatalogService;
import site.benepay.util.DemoUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/v1/cards")
public class CardServlet extends BaseApiServlet {
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeSuccess(response, catalogService.cards(DemoUser.USER_ID));
        } catch (Exception e) {
            handleError(response, e);
        }
    }
}
