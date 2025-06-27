package io.github.bluething.java.bolttrack.exception;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
    BAD_REQUEST(400, "BAD_REQUEST"),
    NOT_FOUND(404, "NOT_FOUND"),
    INTERNAL_ERROR(500, "INTERNAL_SERVER_ERROR");

    private final int status;
    private final String reason;

    ErrorCode(int status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
