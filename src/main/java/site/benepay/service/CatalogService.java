package site.benepay.service;

import site.benepay.config.Database;
import site.benepay.domain.Merchant;
import site.benepay.domain.UserCard;
import site.benepay.repository.CardRepository;
import site.benepay.repository.MerchantRepository;

import java.sql.Connection;
import java.util.List;

public class CatalogService {
    private final CardRepository cardRepository = new CardRepository();
    private final MerchantRepository merchantRepository = new MerchantRepository();

    public List<UserCard> cards(long userId) {
        try (Connection connection = Database.getConnection()) {
            return cardRepository.findActiveCards(connection, userId, false);
        } catch (Exception e) {
            throw new ServiceException(500, "CARD_LIST_FAILED", "카드 목록 조회에 실패했습니다.", e);
        }
    }

    public List<Merchant> merchants() {
        try (Connection connection = Database.getConnection()) {
            return merchantRepository.findAll(connection);
        } catch (Exception e) {
            throw new ServiceException(500, "MERCHANT_LIST_FAILED", "가맹점 목록 조회에 실패했습니다.", e);
        }
    }
}
