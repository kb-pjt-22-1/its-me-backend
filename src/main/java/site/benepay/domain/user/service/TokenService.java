package site.benepay.domain.user.service;

import site.benepay.domain.user.dto.TokenPairDto;
import site.benepay.domain.user.vo.User;

public interface TokenService {

	TokenPairDto issueTokenPair(User user);

	TokenPairDto rotateRefreshToken(String presentedRefreshToken);

	void blacklistAccessToken(String accessToken);

	void revokeRefreshToken(Long userId);
}
