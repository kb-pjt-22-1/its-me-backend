package site.benepay.domain.bookmark.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import site.benepay.domain.bookmark.vo.Bookmark;

@Getter
@Builder
public class BookmarkResponseDto {

	private Long bookmarkId;
	private Long merchantId;
	private LocalDateTime createdAt;

	public static BookmarkResponseDto from(Bookmark bookmark) {
		return BookmarkResponseDto.builder()
				.bookmarkId(bookmark.getBookmarkId())
				.merchantId(bookmark.getMerchantId())
				.createdAt(bookmark.getCreatedAt())
				.build();
	}
}
