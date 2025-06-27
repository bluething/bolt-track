package io.github.bluething.java.bolttrack.exception;

import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;
    protected ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
