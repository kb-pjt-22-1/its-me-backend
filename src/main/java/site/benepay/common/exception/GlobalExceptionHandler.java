package site.benepay.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return errorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUser(DuplicateUserException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.LOCKED, ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidTokenException.class, TokenReuseException.class})
    public ResponseEntity<ErrorResponse> handleInvalidToken(RuntimeException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPinFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPinFormat(InvalidPinFormatException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(PortOneVerificationException.class)
    public ResponseEntity<ErrorResponse> handlePortOneVerification(PortOneVerificationException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMerchantNotFound(MerchantNotFoundException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateMerchantException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateMerchant(DuplicateMerchantException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DevLoginDisabledException.class)
    public ResponseEntity<ErrorResponse> handleDevLoginDisabled(DevLoginDisabledException ex, HttpServletRequest request) {
        // 예외 메시지를 그대로 쓰지 않는다. 없는 경로를 쳤을 때와 응답이 같아야 감추는 의미가 있다.
        return errorResponse(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", request);
    }

    @ExceptionHandler(WithdrawalNotConfirmedException.class)
    public ResponseEntity<ErrorResponse> handleWithdrawalNotConfirmed(WithdrawalNotConfirmedException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(PinAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handlePinAlreadyRegistered(PinAlreadyRegisteredException ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // MyBatis(SqlSessionTemplate)가 SQL 예외를 이 타입으로 변환해서 던진다.
    // FK 제약 위반(존재하지 않는 categoryId/brandId 등), NOT NULL 위반 등이 여기로 들어온다.
    // 안 잡으면 아래 500 핸들러로 떨어져 클라이언트가 서버 에러로 오인하게 된다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("데이터 무결성 제약 위반: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, "요청에 유효하지 않거나 존재하지 않는 참조 값이 있습니다.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("서버 오류", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 서버 오류가 발생했습니다.", request);
    }

    private static ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, request.getRequestURI(), LocalDateTime.now()));
    }
}
