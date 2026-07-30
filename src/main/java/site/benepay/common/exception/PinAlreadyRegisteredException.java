package site.benepay.common.exception;

public class PinAlreadyRegisteredException extends RuntimeException {

    public PinAlreadyRegisteredException(String message) {
        super(message);
    }
}
