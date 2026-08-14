package site.benepay.domain.merchant.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.merchant.vo.Merchant;
import site.benepay.domain.merchant.vo.NearbyMerchantVO;

public interface MerchantMapper {

	List<Merchant> findAll(@Param("categoryCode") String categoryCode);

	Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

	List<Merchant> findWithinBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
		@Param("neLat") double neLat, @Param("neLng") double neLng, @Param("categoryCode") String categoryCode);

	List<NearbyMerchantVO> findNearby(@Param("lat") double lat, @Param("lng") double lng,
		@Param("categoryCode") String categoryCode, @Param("limit") int limit);
}
