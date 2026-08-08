package site.benepay.domain.merchant.mapper;

import java.util.List;

import site.benepay.domain.merchant.vo.MerchantCategory;

public interface MerchantCategoryMapper {

	List<MerchantCategory> findAll();
}
