package site.benepay.domain.merchant.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.merchant.vo.Merchant;

public interface MerchantMapper {

	List<Merchant> findAll(@Param("categoryCode") String categoryCode);

	List<Merchant> findWithinBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
		@Param("neLat") double neLat, @Param("neLng") double neLng, @Param("categoryCode") String categoryCode);
}
