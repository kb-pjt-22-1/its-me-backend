package site.benepay.domain.merchant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantCategoryResponseDto;
import site.benepay.domain.merchant.mapper.MerchantCategoryMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantCategoryServiceImpl implements MerchantCategoryService {

    private final MerchantCategoryMapper merchantCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MerchantCategoryResponseDto> getCategoryList() {
        return merchantCategoryMapper.findAll().stream()
                .map(MerchantCategoryResponseDto::from)
                .collect(Collectors.toList());
    }
}
