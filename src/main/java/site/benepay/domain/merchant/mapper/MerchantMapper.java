package site.benepay.domain.merchant.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.domain.merchant.vo.Merchant;

import java.util.List;
import java.util.Optional;


public interface MerchantMapper {

    void insert(Merchant merchant);

    Optional<Merchant> findByMerchantId(@Param("merchantId") Long merchantId);

    List<Merchant> findAll();

    boolean existsByMerchantCode(@Param("merchantCode") String merchantCode);

    int update(Merchant merchant);

    int deleteByMerchantId(@Param("merchantId") Long merchantId);
}
