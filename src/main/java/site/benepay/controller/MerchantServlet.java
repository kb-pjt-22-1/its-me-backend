package site.benepay.controller;

import site.benepay.service.CatalogService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/v1/merchants")
public class MerchantServlet extends BaseApiServlet {
    private final CatalogService catalogService = new CatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeSuccess(response, catalogService.merchants());
        } catch (Exception e) {
            handleError(response, e);
        }
    }
}
