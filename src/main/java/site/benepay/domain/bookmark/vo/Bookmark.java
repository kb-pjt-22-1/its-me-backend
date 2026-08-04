package site.benepay.domain.bookmark.vo;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Bookmark {

	private Long bookmarkId;
	private Long userId;
	private Long merchantId;
	private LocalDateTime createdAt;
}
