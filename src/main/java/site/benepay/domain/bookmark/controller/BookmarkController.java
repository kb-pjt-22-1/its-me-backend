package site.benepay.domain.bookmark.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.bookmark.dto.BookmarkResponseDto;
import site.benepay.domain.bookmark.service.BookmarkService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {

	private final BookmarkService bookmarkService;

	/**
	 * 북마크 조회. 로그인한 사용자가 저장한 매장 목록을 최근 저장순으로 반환.
	 */
	@GetMapping
	public ResponseEntity<List<BookmarkResponseDto>> getBookmarks(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(bookmarkService.getBookmarks(userId));
	}

	/**
	 * 북마크 추가. 이미 저장했다가 해제한 매장을 다시 저장하는 경우도 처리한다.
	 */
	@PostMapping("/{merchantId}")
	public ResponseEntity<BookmarkResponseDto> addBookmark(@AuthenticationPrincipal Long userId,
			@PathVariable Long merchantId) {
		BookmarkResponseDto response = bookmarkService.addBookmark(userId, merchantId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 북마크 해제.
	 */
	@DeleteMapping("/{merchantId}")
	public ResponseEntity<Void> removeBookmark(@AuthenticationPrincipal Long userId,
			@PathVariable Long merchantId) {
		bookmarkService.removeBookmark(userId, merchantId);
		return ResponseEntity.noContent().build();
	}
}
