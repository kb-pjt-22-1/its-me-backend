package site.benepay.domain.card.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.benepay.domain.card.dto.CardDetailResponseDto;
import site.benepay.domain.card.dto.CardListResponseDto;
import site.benepay.domain.card.service.CardService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

//    @GetMapping
//    public ResponseEntity<List<CardListResponseDto>> getCardList() {
//        // 로그인 기능 연결 전 임시 사용자
//        Long userId = 1L;
//        List<CardListResponseDto> response = cardService.getCardList(userId);
//        return ResponseEntity.ok(response);
//    }
//    @GetMapping("/{userCardId}")
//    public ResponseEntity<CardDetailResponseDto> getCardDetail(@PathVariable Long userCardId) {
//        // 임시값
//        // 나중에는 JWT 인증 정보에서 userId를 꺼내야 함
//        Long userId = 1L;
//        CardDetailResponseDto response = cardService.getCardDetail(userId, userCardId);
//        return ResponseEntity.ok(response);
//    }

//    @GetMapping
//    public ResponseEntity<List<CardListResponseDto>> getCardList(
//            @AuthenticationPrincipal Long userId
//    ) {
//        List<CardListResponseDto> response =
//                cardService.getCardList(userId);
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/{userCardId}")
    public ResponseEntity<CardDetailResponseDto> getCardDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userCardId
    ) {
        CardDetailResponseDto response =
                cardService.getCardDetail(userId, userCardId);

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<CardListResponseDto>> getCardList(
            @AuthenticationPrincipal Long userId
    ) {
        System.out.println("JWT userId = " + userId);

        return ResponseEntity.ok(
                cardService.getCardList(userId)
        );
    }
}