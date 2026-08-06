package site.benepay.domain.bookmark.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {

	private Long bookmarkId;
	private Long userId;
	private Long merchantId;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
}
