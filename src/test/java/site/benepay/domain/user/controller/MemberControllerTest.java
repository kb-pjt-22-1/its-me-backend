package site.benepay.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import site.benepay.domain.user.service.UserService;

// @AuthenticationPrincipal은 standalone MockMvc가 못 풀어주므로(LocationControllerTest와 동일한 이유),
// 컨트롤러 메서드를 직접 호출한다.
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

	private static final Long USER_ID = 1L;

	@Mock
	private UserService userService;

	@Mock
	private HttpServletRequest servletRequest;

	@Test
	void withdrawExtractsTheAccessTokenAndDelegatesToTheService() {
		MemberController controller = new MemberController(userService);
		when(servletRequest.getHeader("Authorization")).thenReturn("Bearer access-token");

		ResponseEntity<Void> response = controller.withdraw(USER_ID, true, servletRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(userService).withdraw(USER_ID, "access-token", true);
	}

	@Test
	void withdrawPassesNullAccessTokenWhenNoAuthorizationHeaderIsPresent() {
		MemberController controller = new MemberController(userService);
		when(servletRequest.getHeader("Authorization")).thenReturn(null);

		controller.withdraw(USER_ID, false, servletRequest);

		verify(userService).withdraw(USER_ID, null, false);
	}
}
