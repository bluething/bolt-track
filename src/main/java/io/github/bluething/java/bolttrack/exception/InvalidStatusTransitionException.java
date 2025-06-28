package io.github.bluething.java.bolttrack.exception;

public class InvalidStatusTransitionException extends ApplicationException {
    public InvalidStatusTransitionException(String from, String to) {
        super(ErrorCode.BAD_REQUEST, "Cannot transition status from " + from + " to " + to);
    }
}
