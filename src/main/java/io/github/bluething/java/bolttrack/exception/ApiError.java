package io.github.bluething.java.bolttrack.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class ApiError {
    public record ErrorDetail(String field, String message) {}
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<ErrorDetail> errors;

    private ApiError(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.path = builder.path;
        this.errors = builder.errors;
    }
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Instant timestamp = Instant.now();
        private int status;
        private String error;
        private String message;
        private String path;
        private List<ErrorDetail> errors = List.of();

        public Builder status(ErrorCode errorCode) {
            this.status = errorCode.getStatus();
            this.error = errorCode.getReason();
            return this;
        }
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        public Builder errors(List<ErrorDetail> errors) {
            this.errors = errors;
            return this;
        }
        public ApiError build() {
            return new ApiError(this);
        }
    }
}
