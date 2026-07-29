package site.benepay.domain.card.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.benepay.domain.card.dto.CardDetailResponseDto;
import site.benepay.domain.card.dto.CardListResponseDto;
import site.benepay.domain.card.mapper.CardMapper;
import site.benepay.domain.card.vo.UserCardDetailVO;
import site.benepay.domain.card.vo.UserCardListVO;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardMapper cardMapper;

    public CardDetailResponseDto getCardDetail(Long userId, Long userCardId) {
        UserCardDetailVO card = cardMapper.findDetailByUserCardId(userId, userCardId)
                                          .orElseThrow(() -> new IllegalArgumentException("카드를 찾을 수 없습니다."));

        return CardDetailResponseDto.builder()
                .userCardId(card.getUserCardId())
                .cardId(card.getCardId())
                .cardName(card.getCardName())
                .cardType(card.getCardType())
                .cardImageUrl(card.getCardImageUrl())
                .description(card.getDescription())
                .cardNetwork(card.getCardNetwork())
                .annualFee(card.getAnnualFee())
                .maskedCardNumber(maskCardNumber(card.getPanLast4()))
                .tokenExpiryDate(card.getTokenExpiryDate())
                .status(card.getStatus())
                .primary(card.getPrimaryCard())
                .recommendationEnabled(card.getRecommendationEnabled())
                .supported(card.getSupported())
                .minBenefitAmount(card.getMinBenefitAmount())
                .build();
    }

    public List<CardListResponseDto> getCardList(Long userId) {

        List<UserCardListVO> cardList =
                cardMapper.findAllByUserId(userId);

        return cardList.stream()
                .map(this::toCardListResponseDto)
                .collect(Collectors.toList());
    }

    private CardListResponseDto toCardListResponseDto(
            UserCardListVO userCard
    ) {
        return CardListResponseDto.builder()
                .userCardId(userCard.getUserCardId())
                .cardId(userCard.getCardId())
                .cardName(userCard.getCardName())
                .cardType(userCard.getCardType())
                .cardImageUrl(userCard.getCardImageUrl())
                .cardNetwork(userCard.getCardNetwork())
                .annualFee(userCard.getAnnualFee())
                .panLast4(userCard.getPanLast4())
                .status(userCard.getStatus())
                .primary(userCard.getPrimaryCard())
                .recommendationEnabled(
                        userCard.getRecommendationEnabled()
                )
                .build();
    }
    private String maskCardNumber(String panLast4) {
        return "**** **** **** " + panLast4;
    }
}