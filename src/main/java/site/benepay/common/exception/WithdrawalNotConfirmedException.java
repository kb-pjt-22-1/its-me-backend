package site.benepay.common.exception;

public class WithdrawalNotConfirmedException extends RuntimeException {

    public WithdrawalNotConfirmedException(String message) {
        super(message);
    }
}
