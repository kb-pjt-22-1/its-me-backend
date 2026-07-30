package site.benepay.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.benepay.dto.ApiResponse;
import site.benepay.service.BookmarkService;
import site.benepay.service.ServiceException;
import site.benepay.util.DemoUser;

@RestController
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ApiResponse<Object> findAll() {
        return ApiResponse.success(bookmarkService.findAll(DemoUser.USER_ID));
    }

    @PostMapping
    public ApiResponse<Object> save(@RequestBody BookmarkRequest request) {
        if (request.getMerchantId() <= 0) {
            throw new ServiceException(400, "INVALID_MERCHANT", "매장 ID가 필요합니다.");
        }
        return ApiResponse.success(bookmarkService.save(DemoUser.USER_ID, request.getMerchantId()));
    }

    @DeleteMapping("/{merchantId}")
    public ApiResponse<Object> delete(@PathVariable long merchantId) {
        return ApiResponse.success(bookmarkService.delete(DemoUser.USER_ID, merchantId));
    }

    public static class BookmarkRequest {
        private long merchantId;

        public long getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(long merchantId) {
            this.merchantId = merchantId;
        }
    }
}
