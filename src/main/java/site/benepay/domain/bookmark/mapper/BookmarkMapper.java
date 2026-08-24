package site.benepay.domain.bookmark.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import site.benepay.domain.bookmark.vo.Bookmark;

public interface BookmarkMapper {

	// 이미 있던 매장을 북마크 해제(is_deleted=1)했다가 다시 북마크하는 경우까지 한 번에 처리한다.
	// (user_id, merchant_id) 유니크 제약이 있어서 단순 INSERT로는 재북마크 시 중복키 에러가 난다.
	int upsertBookmark(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

	int softDeleteBookmark(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

	List<Bookmark> findActiveByUserId(@Param("userId") Long userId);

	Optional<Bookmark> findByUserIdAndMerchantId(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

	// 회원 탈퇴 시 북마크 일괄 소프트삭제
	int softDeleteAllByUserId(@Param("userId") Long userId);
}
