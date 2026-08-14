package site.benepay.domain.merchant.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.vo.Merchant;

public interface MerchantMapper {

	List<Merchant> findAll(@Param("categoryCode") String categoryCode);

	Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

	List<Merchant> findWithinBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
		@Param("neLat") double neLat, @Param("neLng") double neLng, @Param("categoryCode") String categoryCode);

	// distanceMeters까지 포함해서 MerchantResponseDto와 필드가 완전히 같아서, 중간 VO 없이
	// MyBatis가 바로 응답 DTO로 매핑한다(resultType, MerchantMapper.xml 참고).
	List<MerchantResponseDto> findNearby(@Param("lat") double lat, @Param("lng") double lng,
		@Param("categoryCode") String categoryCode, @Param("limit") int limit);
}
