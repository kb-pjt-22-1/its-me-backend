package site.benepay.service;

import site.benepay.config.Database;
import site.benepay.domain.Merchant;
import site.benepay.repository.BookmarkRepository;
import site.benepay.repository.MerchantRepository;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookmarkService {
    private final BookmarkRepository bookmarkRepository = new BookmarkRepository();
    private final MerchantRepository merchantRepository = new MerchantRepository();

    public List<Merchant> findAll(long userId) {
        try (Connection connection = Database.getConnection()) {
            return bookmarkRepository.findByUser(connection, userId);
        } catch (Exception e) {
            throw new ServiceException(500, "BOOKMARK_LIST_FAILED", "북마크 매장 조회에 실패했습니다.", e);
        }
    }

    public Map<String, Object> save(long userId, long merchantId) {
        try (Connection connection = Database.getConnection()) {
            Merchant merchant = merchantRepository.findById(connection, merchantId);
            if (merchant == null) throw new ServiceException(404, "MERCHANT_NOT_FOUND", "가맹점을 찾을 수 없습니다.");
            bookmarkRepository.save(connection, userId, merchantId);
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("bookmarked", true);
            response.put("merchant", merchant);
            return response;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "BOOKMARK_SAVE_FAILED", "매장 북마크 저장에 실패했습니다.", e);
        }
    }

    public Map<String, Object> delete(long userId, long merchantId) {
        try (Connection connection = Database.getConnection()) {
            bookmarkRepository.delete(connection, userId, merchantId);
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("bookmarked", false);
            response.put("merchantId", merchantId);
            return response;
        } catch (Exception e) {
            throw new ServiceException(500, "BOOKMARK_DELETE_FAILED", "매장 북마크 해제에 실패했습니다.", e);
        }
    }
}
