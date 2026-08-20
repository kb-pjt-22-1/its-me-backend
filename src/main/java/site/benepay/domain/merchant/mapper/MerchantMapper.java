package site.benepay.domain.merchant.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.merchant.vo.Merchant;

public interface MerchantMapper {

	List<Merchant> findAll(@Param("categoryCode") String categoryCode);

	Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

	// Redis GEO 검색(MerchantGeoQueryService)이 반경 검색으로 이미 추려낸 merchantId들의 상세
	// 정보만 가져올 때 쓴다. 거리 계산이나 정렬은 Redis 쪽에서 끝난 뒤라 여기서는 순서 없이
	// PK로만 조회한다.
	List<Merchant> findByIds(@Param("merchantIds") List<Long> merchantIds);
}
