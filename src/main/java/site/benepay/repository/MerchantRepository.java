package site.benepay.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import site.benepay.domain.Merchant;

public class MerchantRepository {
	private static final String BASE_SELECT =
		"SELECT m.merchant_id, m.merchant_code, m.merchant_name, m.brand_name, " +
			"mc.category_code, mc.category_name, m.address, m.latitude, m.longitude, m.phone FROM merchants m " +
			"JOIN merchant_categories mc ON mc.category_id = m.category_id ";

	public List<Merchant> findAll(Connection connection) throws SQLException {
		String sql = BASE_SELECT + "ORDER BY m.merchant_id";
		List<Merchant> merchants = new ArrayList<Merchant>();
		try (PreparedStatement ps = connection.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				merchants.add(map(rs));
		}
		return merchants;
	}

	public Merchant findById(Connection connection, long merchantId) throws SQLException {
		String sql = BASE_SELECT + "WHERE m.merchant_id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setLong(1, merchantId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? map(rs) : null;
			}
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
