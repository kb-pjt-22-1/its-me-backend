package site.benepay.controller;

import site.benepay.service.BookmarkService;
import site.benepay.service.ServiceException;
import site.benepay.util.DemoUser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/v1/bookmarks/*")
public class BookmarkServlet extends BaseApiServlet {
    private final BookmarkService bookmarkService = new BookmarkService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeSuccess(response, bookmarkService.findAll(DemoUser.USER_ID));
        } catch (Exception e) {
            handleError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            BookmarkRequest body = readJson(request, BookmarkRequest.class);
            if (body.merchantId <= 0) throw new ServiceException(400, "INVALID_MERCHANT", "매장 ID가 필요합니다.");
            writeSuccess(response, bookmarkService.save(DemoUser.USER_ID, body.merchantId));
        } catch (Exception e) {
            handleError(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = request.getPathInfo();
            if (path == null || path.length() <= 1) {
                throw new ServiceException(400, "MERCHANT_ID_REQUIRED", "매장 ID가 필요합니다.");
            }
            long merchantId = parseLong(path.substring(1), "merchantId");
            writeSuccess(response, bookmarkService.delete(DemoUser.USER_ID, merchantId));
        } catch (Exception e) {
            handleError(response, e);
        }
    }

    private static class BookmarkRequest {
        long merchantId;
    }
}
