package site.benepay.domain.bookmark.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.benepay.domain.bookmark.dto.BookmarkResponseDto;
import site.benepay.domain.bookmark.mapper.BookmarkMapper;
import site.benepay.domain.bookmark.vo.Bookmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceImplTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 100L;

	@Mock
	private BookmarkMapper bookmarkMapper;

	private BookmarkService bookmarkService;

	@BeforeEach
	void setUp() {
		bookmarkService = new BookmarkServiceImpl(bookmarkMapper);
	}

	private Bookmark bookmark() {
		return Bookmark.builder()
				.bookmarkId(1L)
				.userId(USER_ID)
				.merchantId(MERCHANT_ID)
				.createdAt(LocalDateTime.of(2026, 8, 6, 12, 0))
				.isDeleted(false)
				.build();
	}

	// ---- addBookmark ----

	@Test
	void addBookmarkUpsertsThenReturnsTheStoredBookmark() {
		when(bookmarkMapper.findByUserIdAndMerchantId(USER_ID, MERCHANT_ID))
				.thenReturn(Optional.of(bookmark()));

		BookmarkResponseDto response = bookmarkService.addBookmark(USER_ID, MERCHANT_ID);

		assertThat(response.getBookmarkId()).isEqualTo(1L);
		assertThat(response.getMerchantId()).isEqualTo(MERCHANT_ID);

		InOrder inOrder = inOrder(bookmarkMapper);
		inOrder.verify(bookmarkMapper).upsertBookmark(USER_ID, MERCHANT_ID);
		inOrder.verify(bookmarkMapper).findByUserIdAndMerchantId(USER_ID, MERCHANT_ID);
	}

	@Test
	void addBookmarkThrowsWhenTheUpsertedRowCannotBeFoundBack() {
		when(bookmarkMapper.findByUserIdAndMerchantId(USER_ID, MERCHANT_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> bookmarkService.addBookmark(USER_ID, MERCHANT_ID))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("북마크 추가에 실패했습니다.");
	}

	// ---- removeBookmark ----

	@Test
	void removeBookmarkSoftDeletesTheBookmark() {
		bookmarkService.removeBookmark(USER_ID, MERCHANT_ID);

		verify(bookmarkMapper).softDeleteBookmark(USER_ID, MERCHANT_ID);
	}

	// ---- getBookmarks ----

	@Test
	void getBookmarksReturnsTheMappedListForTheUser() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of(bookmark()));

		List<BookmarkResponseDto> bookmarks = bookmarkService.getBookmarks(USER_ID);

		assertThat(bookmarks).hasSize(1);
		assertThat(bookmarks.get(0).getMerchantId()).isEqualTo(MERCHANT_ID);
	}

	@Test
	void getBookmarksReturnsAnEmptyListWhenTheUserHasNoBookmarks() {
		when(bookmarkMapper.findActiveByUserId(USER_ID)).thenReturn(List.of());

		assertThat(bookmarkService.getBookmarks(USER_ID)).isEmpty();
	}
}
