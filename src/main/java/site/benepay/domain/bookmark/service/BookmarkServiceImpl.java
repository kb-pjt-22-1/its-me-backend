package site.benepay.domain.bookmark.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.benepay.domain.bookmark.dto.BookmarkResponseDto;
import site.benepay.domain.bookmark.mapper.BookmarkMapper;
import site.benepay.domain.bookmark.vo.Bookmark;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

	private final BookmarkMapper bookmarkMapper;

	@Override
	@Transactional
	public BookmarkResponseDto addBookmark(Long userId, Long merchantId) {
		bookmarkMapper.upsertBookmark(userId, merchantId);

		Bookmark bookmark = bookmarkMapper.findByUserIdAndMerchantId(userId, merchantId)
				.orElseThrow(() -> new IllegalStateException("북마크 추가에 실패했습니다."));

		return BookmarkResponseDto.from(bookmark);
	}

	@Override
	@Transactional
	public void removeBookmark(Long userId, Long merchantId) {
		bookmarkMapper.softDeleteBookmark(userId, merchantId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookmarkResponseDto> getBookmarks(Long userId) {
		return bookmarkMapper.findActiveByUserId(userId).stream()
				.map(BookmarkResponseDto::from)
				.collect(Collectors.toList());
	}
}
