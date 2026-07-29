package site.benepay.domain.card.mapper;

import org.apache.ibatis.annotations.Param;
import site.benepay.domain.card.vo.UserCardDetailVO;
import site.benepay.domain.card.vo.UserCardListVO;

import java.util.List;
import java.util.Optional;

public interface CardMapper {

    List<UserCardListVO> findAllByUserId(@Param("userId") Long userId);

    Optional<UserCardDetailVO> findDetailByUserCardId(@Param("userId") Long userId, @Param("userCardId") Long userCardId);
}