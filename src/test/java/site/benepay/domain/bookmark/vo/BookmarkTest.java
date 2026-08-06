package site.benepay.domain.bookmark.vo;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkTest {

	@Test
	void builderCreatesBookmarkWithAllFields() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 12, 0);

		Bookmark bookmark = Bookmark.builder()
				.bookmarkId(1L)
				.userId(10L)
				.merchantId(100L)
				.createdAt(createdAt)
				.isDeleted(false)
				.build();

		assertThat(bookmark.getBookmarkId()).isEqualTo(1L);
		assertThat(bookmark.getUserId()).isEqualTo(10L);
		assertThat(bookmark.getMerchantId()).isEqualTo(100L);
		assertThat(bookmark.getCreatedAt()).isEqualTo(createdAt);
		assertThat(bookmark.getIsDeleted()).isFalse();
	}

	@Test
	void noArgsConstructorLeavesEveryFieldNull() {
		Bookmark bookmark = new Bookmark();

		assertThat(bookmark.getBookmarkId()).isNull();
		assertThat(bookmark.getUserId()).isNull();
		assertThat(bookmark.getMerchantId()).isNull();
		assertThat(bookmark.getCreatedAt()).isNull();
		assertThat(bookmark.getIsDeleted()).isNull();
	}
}
