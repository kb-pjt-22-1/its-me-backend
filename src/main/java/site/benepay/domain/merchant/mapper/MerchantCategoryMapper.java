package site.benepay.domain.merchant.mapper;

import site.benepay.domain.merchant.vo.MerchantCategory;

import java.util.List;

public interface MerchantCategoryMapper {

    List<MerchantCategory> findAll();
}
