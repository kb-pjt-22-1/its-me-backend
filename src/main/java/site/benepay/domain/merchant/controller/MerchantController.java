package site.benepay.domain.merchant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.dto.NearbyMerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 매장 조회
     * 지도 화면에서 매장들을 보여주기 위해 매장 리스트를 반환할 때 사용
     * @return
     */
    @GetMapping
    public ResponseEntity<List<MerchantResponseDto>> getMerchantList() {
        return ResponseEntity.ok(merchantService.getMerchantList());
    }

    /**
     * 주변 매장 후보군 조회
     * 위치(위도/경도)와 반경(m)을 받아 그 안에 있는 모든 매장을 가까운 순으로 반환.
     * 추천 로직이 이 후보군을 받아 카드 혜택 기준으로 다시 추려내는 걸 전제로 한다.
     * @param lat 기준 위도
     * @param lng 기준 경도
     * @param radiusMeters 반경(m), 기본 1000m
     * @return
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyMerchantResponseDto>> getNearbyMerchants(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radiusMeters
    ) {
        return ResponseEntity.ok(merchantService.getNearbyMerchants(lat, lng, radiusMeters));
    }

    /**
     * 지도 화면 bounds(bbox) 안의 매장 조회
     * 지도를 이동/확대할 때마다 현재 화면(bounds)에 보이는 매장만 반환한다. 거리 개념이 없으므로
     * distanceMeters는 항상 null이다.
     * @param swLat 지도 화면 남서쪽 위도
     * @param swLng 지도 화면 남서쪽 경도
     * @param neLat 지도 화면 북동쪽 위도
     * @param neLng 지도 화면 북동쪽 경도
     * @return
     */
    @GetMapping("/within-bounds")
    public ResponseEntity<List<NearbyMerchantResponseDto>> getMerchantsWithinBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng
    ) {
        return ResponseEntity.ok(merchantService.getMerchantsWithinBounds(swLat, swLng, neLat, neLng));
    }

    /**
     * 매장 생성
     * 매장을 등록할 때 사용
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<MerchantResponseDto> createMerchant(@Valid @RequestBody MerchantRequestDto request) {
        MerchantResponseDto response = merchantService.createMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 매장 조회
     * @param merchantId
     * @return
     */
    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponseDto> getMerchant(@PathVariable Long merchantId) {
        return ResponseEntity.ok(merchantService.getMerchant(merchantId));
    }

    /**
     * 매장 수정
     * @param merchantId
     * @param request
     * @return
     */
    @PutMapping("/{merchantId}")
    public ResponseEntity<MerchantResponseDto> updateMerchant(
            @PathVariable Long merchantId,
            @Valid @RequestBody MerchantRequestDto request
    ) {
        return ResponseEntity.ok(merchantService.updateMerchant(merchantId, request));
    }

    /**
     * 매장 삭제
     * @param merchantId
     * @return
     */
    @DeleteMapping("/{merchantId}")
    public ResponseEntity<Void> deleteMerchant(@PathVariable Long merchantId) {
        merchantService.deleteMerchant(merchantId);
        return ResponseEntity.noContent().build();
    }
}
