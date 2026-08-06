package site.benepay.domain.bookmark.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.bookmark.dto.BookmarkResponseDto;
import site.benepay.domain.bookmark.service.BookmarkService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long MERCHANT_ID = 100L;

	@Mock
	private BookmarkService bookmarkService;

	private BookmarkController controller;

	@BeforeEach
	void setUp() {
		controller = new BookmarkController(bookmarkService);
	}

	@Test
	void getBookmarksReturnsOkWithTheServiceResult() {
		List<BookmarkResponseDto> bookmarks = List.of(
				BookmarkResponseDto.builder()
						.bookmarkId(1L)
						.merchantId(MERCHANT_ID)
						.createdAt(LocalDateTime.of(2026, 8, 6, 12, 0))
						.build());
		when(bookmarkService.getBookmarks(USER_ID)).thenReturn(bookmarks);

		ResponseEntity<List<BookmarkResponseDto>> response = controller.getBookmarks(USER_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(bookmarks);
	}

	@Test
	void addBookmarkReturnsCreatedWithTheServiceResult() {
		BookmarkResponseDto created = BookmarkResponseDto.builder()
				.bookmarkId(1L)
				.merchantId(MERCHANT_ID)
				.createdAt(LocalDateTime.of(2026, 8, 6, 12, 0))
				.build();
		when(bookmarkService.addBookmark(USER_ID, MERCHANT_ID)).thenReturn(created);

		ResponseEntity<BookmarkResponseDto> response = controller.addBookmark(USER_ID, MERCHANT_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(created);
	}

	@Test
	void removeBookmarkDelegatesToTheServiceAndReturnsNoContent() {
		ResponseEntity<Void> response = controller.removeBookmark(USER_ID, MERCHANT_ID);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(bookmarkService).removeBookmark(USER_ID, MERCHANT_ID);
	}
}
