package site.benepay.domain.bookmark.dto;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import site.benepay.domain.bookmark.vo.Bookmark;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkResponseDtoTest {

	@Test
	void fromMapsEveryFieldFromBookmark() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 12, 0);
		Bookmark bookmark = Bookmark.builder()
				.bookmarkId(1L)
				.userId(10L)
				.merchantId(100L)
				.createdAt(createdAt)
				.isDeleted(false)
				.build();

		BookmarkResponseDto response = BookmarkResponseDto.from(bookmark);

		assertThat(response.getBookmarkId()).isEqualTo(1L);
		assertThat(response.getMerchantId()).isEqualTo(100L);
		assertThat(response.getCreatedAt()).isEqualTo(createdAt);
	}
}
