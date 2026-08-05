package site.benepay.domain.bookmark.service;

import java.util.List;

import site.benepay.domain.bookmark.dto.BookmarkResponseDto;

public interface BookmarkService {

	BookmarkResponseDto addBookmark(Long userId, Long merchantId);

	void removeBookmark(Long userId, Long merchantId);

	List<BookmarkResponseDto> getBookmarks(Long userId);
}
