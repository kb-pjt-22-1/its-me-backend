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
import org.springframework.web.bind.annotation.RestController;
import site.benepay.domain.merchant.dto.MerchantRequestDto;
import site.benepay.domain.merchant.dto.MerchantResponseDto;
import site.benepay.domain.merchant.service.MerchantService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

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
