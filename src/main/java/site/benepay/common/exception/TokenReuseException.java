package site.benepay.common.exception;

public class TokenReuseException extends RuntimeException {

    public TokenReuseException(String message) {
        super(message);
    }
}
