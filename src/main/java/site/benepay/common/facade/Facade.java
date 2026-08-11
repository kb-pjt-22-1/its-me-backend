package site.benepay.common.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantRecommendationResponseDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;

@Component
@RequiredArgsConstructor
public class Facade {

	/*
	 * TODO: 추천 로직이 연동되면 사용자 보유 카드로 지금 당장 혜택을 주는 매장만
	 * recommended=true로 표시하도록 교체한다. 지금은 후보 매장을 그대로 반환한다(전부 recommended=false).
	 */
	public List<MerchantRecommendationResponseDto> getRecommendedMerchants(Long userId, List<MerchantResponseDto> merchants) {
		return merchants.stream()
			.map(MerchantRecommendationResponseDto::from)
			.toList();
	}
}
