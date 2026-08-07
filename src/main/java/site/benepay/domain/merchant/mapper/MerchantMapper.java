package site.benepay.domain.merchant.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.domain.merchant.vo.Merchant;
import site.benepay.domain.merchant.vo.MerchantNearbyVO;

import java.util.List;
import java.util.Optional;


public interface MerchantMapper {

    void insert(Merchant merchant);

    Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

    List<Merchant> findAll();

    List<MerchantNearbyVO> findWithinRadius(@Param("lat") double lat, @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters);

    List<MerchantNearbyVO> findWithinBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
            @Param("neLat") double neLat, @Param("neLng") double neLng);

    boolean existsByMerchantCode(@Param("merchantCode") String merchantCode);

    int update(Merchant merchant);

    int deleteByMerchantId(@Param("merchantId") Long merchantId);
}
