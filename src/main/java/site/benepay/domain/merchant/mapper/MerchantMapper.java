package site.benepay.domain.merchant.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.vo.Merchant;

public interface MerchantMapper {

	List<Merchant> findAll(@Param("categoryCode") String categoryCode);

	Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

	// findNearby와 마찬가지로 distanceMeters까지 포함해서 MerchantResponseDto와 필드가 완전히
	// 같아서, 중간 VO 없이 MyBatis가 바로 응답 DTO로 매핑한다(resultType, MerchantMapper.xml
	// 참고). bounds가 넓어져도 응답이 무한정 커지지 않도록 거리순으로 limit개까지만 자른다.
	// centerLat/centerLng를 @Param("lat")/@Param("lng")로 바인딩하는 이유: findNearby와 같은
	// haversineDistanceMeters SQL 조각(#{lat}/#{lng} 참조)을 그대로 재사용하기 위해서다 -
	// 여기서의 의미는 "사용자 위치"가 아니라 "bounds 중심"이지만 계산식은 같다.
	List<MerchantResponseDto> findWithinBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
		@Param("neLat") double neLat, @Param("neLng") double neLng,
		@Param("lat") double centerLat, @Param("lng") double centerLng,
		@Param("categoryCode") String categoryCode, @Param("limit") int limit);

	// distanceMeters까지 포함해서 MerchantResponseDto와 필드가 완전히 같아서, 중간 VO 없이
	// MyBatis가 바로 응답 DTO로 매핑한다(resultType, MerchantMapper.xml 참고).
	List<MerchantResponseDto> findNearby(@Param("lat") double lat, @Param("lng") double lng,
		@Param("categoryCode") String categoryCode, @Param("limit") int limit);
}
