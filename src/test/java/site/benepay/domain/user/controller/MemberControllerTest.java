package site.benepay.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import site.benepay.domain.user.service.UserService;

// MemberController.currentUserId()는 @AuthenticationPrincipal이 아니라 SecurityContextHolder를
// 직접 읽으므로(AuthController.logout과 동일한 패턴), MockMvc 대신 SecurityContext를 직접 채우고
// 컨트롤러 메서드를 바로 호출한다 - NotificationControllerTest/LocationControllerTest와 같은 이유의
// 직접 호출 패턴이다.
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

	private static final Long USER_ID = 1L;

	@Mock
	private UserService userService;

	@Mock
	private HttpServletRequest servletRequest;

	@BeforeEach
	void setUpSecurityContext() {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null));
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void withdrawExtractsTheAccessTokenAndDelegatesToTheService() {
		MemberController controller = new MemberController(userService);
		when(servletRequest.getHeader("Authorization")).thenReturn("Bearer access-token");

		ResponseEntity<Void> response = controller.withdraw(true, servletRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(userService).withdraw(USER_ID, "access-token", true);
	}

	@Test
	void withdrawPassesNullAccessTokenWhenNoAuthorizationHeaderIsPresent() {
		MemberController controller = new MemberController(userService);
		when(servletRequest.getHeader("Authorization")).thenReturn(null);

		controller.withdraw(false, servletRequest);

		verify(userService).withdraw(USER_ID, null, false);
	}
}
