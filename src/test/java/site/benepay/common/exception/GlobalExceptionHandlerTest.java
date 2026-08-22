package site.benepay.common.exception;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/test";

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn(PATH);
    }

    @Test
    void handleValidationUsesTheFirstFieldErrorMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("target", "loginId", "필수 항목입니다.")));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("loginId: 필수 항목입니다.");
        assertThat(response.getBody().path()).isEqualTo(PATH);
    }

    @Test
    void handleValidationFallsBackToADefaultMessageWhenThereAreNoFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getBody().message()).isEqualTo("잘못된 요청입니다.");
    }

    @Test
    void handleDuplicateUserReturnsConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDuplicateUser(new DuplicateUserException("이미 가입된 아이디입니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("이미 가입된 아이디입니다.");
    }

    @Test
    void handleInvalidCredentialsReturnsUnauthorized() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException("비밀번호가 일치하지 않습니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleAccountLockedReturnsLocked() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccountLocked(new AccountLockedException("계정이 잠겼습니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void handleInvalidTokenReturnsUnauthorizedForInvalidToken() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidToken(new InvalidTokenException("유효하지 않은 토큰입니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleInvalidTokenReturnsUnauthorizedForTokenReuse() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidToken(new TokenReuseException("이미 사용된 토큰입니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleInvalidPinFormatReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidPinFormat(new InvalidPinFormatException("PIN 형식이 올바르지 않습니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleKbCustomerNotFoundReturnsUnprocessableEntity() {
        ResponseEntity<ErrorResponse> response =
                handler.handleKbCustomerNotFound(new KbCustomerNotFoundException("KB에 등록된 회원이 아닙니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleVerificationCodeInvalidReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleVerificationCodeInvalid(
                        new VerificationCodeInvalidException("인증번호가 올바르지 않습니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleUserNotFoundReturnsNotFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUserNotFound(new UserNotFoundException("사용자를 찾을 수 없습니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handlePaymentNotFoundReturnsNotFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handlePaymentNotFound(new PaymentNotFoundException("결제 내역을 찾을 수 없습니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("결제 내역을 찾을 수 없습니다.");
        assertThat(response.getBody().path()).isEqualTo(PATH);
    }

    @Test
    void handleWithdrawalNotConfirmedReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleWithdrawalNotConfirmed(new WithdrawalNotConfirmedException("탈퇴 확인이 필요합니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlePinAlreadyRegisteredReturnsConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handlePinAlreadyRegistered(new PinAlreadyRegisteredException("이미 등록된 PIN입니다."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleDataIntegrityViolationReturnsBadRequestWithAGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(new DataIntegrityViolationException("FK constraint fails"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("요청에 유효하지 않거나 존재하지 않는 참조 값이 있습니다.");
    }

    @Test
    void handleUnexpectedReturnsInternalServerError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("예상치 못한 서버 오류가 발생했습니다.");
    }
}
