package site.benepay.repository;

import site.benepay.domain.Merchant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookmarkRepository {
    public List<Merchant> findByUser(Connection connection, long userId) throws SQLException {
        String sql = "SELECT m.merchant_id, m.merchant_code, m.merchant_name, m.brand_name, " +
                "mc.category_code, mc.category_name, m.address, m.latitude, m.longitude, m.phone " +
                "FROM bookmarked_stores bs " +
                "JOIN merchants m ON m.merchant_id = bs.merchant_id " +
                "JOIN merchant_categories mc ON mc.category_id = m.category_id " +
                "WHERE bs.user_id = ? AND bs.is_deleted = FALSE " +
                "ORDER BY bs.created_at DESC";
        List<Merchant> merchants = new ArrayList<Merchant>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) merchants.add(map(rs));
            }
        }
        return merchants;
    }

    public boolean isBookmarked(Connection connection, long userId, long merchantId) throws SQLException {
        String sql = "SELECT 1 FROM bookmarked_stores WHERE user_id = ? AND merchant_id = ? AND is_deleted = FALSE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, merchantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void save(Connection connection, long userId, long merchantId) throws SQLException {
        String update = "UPDATE bookmarked_stores SET is_deleted = FALSE, created_at = CURRENT_TIMESTAMP " +
                "WHERE user_id = ? AND merchant_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(update)) {
            ps.setLong(1, userId);
            ps.setLong(2, merchantId);
            if (ps.executeUpdate() > 0) return;
        }
        String insert = "INSERT INTO bookmarked_stores (user_id, merchant_id, created_at, is_deleted) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, FALSE)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setLong(1, userId);
            ps.setLong(2, merchantId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection connection, long userId, long merchantId) throws SQLException {
        String sql = "UPDATE bookmarked_stores SET is_deleted = TRUE WHERE user_id = ? AND merchant_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, merchantId);
            ps.executeUpdate();
        }
    }

    private Merchant map(ResultSet rs) throws SQLException {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(rs.getLong("merchant_id"));
        merchant.setMerchantCode(rs.getString("merchant_code"));
        merchant.setMerchantName(rs.getString("merchant_name"));
        merchant.setBrandName(rs.getString("brand_name"));
        merchant.setCategoryCode(rs.getString("category_code"));
        merchant.setCategoryName(rs.getString("category_name"));
        merchant.setAddress(rs.getString("address"));
        merchant.setLatitude(rs.getBigDecimal("latitude"));
        merchant.setLongitude(rs.getBigDecimal("longitude"));
        merchant.setPhone(rs.getString("phone"));
        return merchant;
    }
}
