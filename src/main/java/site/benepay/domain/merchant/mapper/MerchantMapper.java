package site.benepay.domain.merchant.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.merchant.vo.Merchant;

public interface MerchantMapper {

	List<Merchant> findAll(@Param("categoryCode") String categoryCode);

	Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

	// Redis GEO 검색(MerchantServiceImpl)이 좌표 순 merchant_id 목록을 뽑아준 뒤, 그 PK로
	// 나머지 컬럼을 채우는 용도 - 거리 계산은 이미 Redis가 끝냈으므로 여기선 안 한다. IN절이라
	// 반환 순서가 보장되지 않으므로, 호출부가 Redis가 준 순서대로 다시 정렬해서 써야 한다.
	List<Merchant> findByIds(@Param("merchantIds") List<Long> merchantIds);
}
