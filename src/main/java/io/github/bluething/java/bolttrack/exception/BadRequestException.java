package io.github.bluething.java.bolttrack.exception;

public class BadRequestException extends ApplicationException {
    protected BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
