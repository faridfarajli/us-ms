package az.ingress.common.exception.exceptions;

public class UnAuthorizedException extends RuntimeException {

    public UnAuthorizedException(Throwable throwable) {
        super(throwable);
    }

    public UnAuthorizedException(String message) {
        super(message);
    }
}
