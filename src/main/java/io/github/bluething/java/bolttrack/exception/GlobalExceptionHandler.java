package io.github.bluething.java.bolttrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public final class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handle(ApplicationException ex, HttpServletRequest request) {
        ApiError error = ApiError.builder()
                .status(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(error);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,
            BindException.class})
    public ResponseEntity<ApiError> handle(Exception ex, HttpServletRequest request) {
        List<ApiError.ErrorDetail> details = ex instanceof MethodArgumentNotValidException mex
                ? mex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.ErrorDetail(
                        fe.getField(),
                        fe.getDefaultMessage()
                ))
                .toList()
                : ((BindException) ex).getBindingResult().getAllErrors().stream()
                .map(err -> {
                    String field = (err instanceof FieldError fe) ? fe.getField() : null;
                    return new ApiError.ErrorDetail(
                            field,
                            err.getDefaultMessage()
                    );
                })
                .toList();

        ApiError error = ApiError.builder()
                .status(ErrorCode.BAD_REQUEST)
                .message("Validation failed")
                .errors(details)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> onConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiError.ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String[] parts = cv.getPropertyPath().toString().split("\\.");
                    String field   = parts[parts.length - 1];
                    return new ApiError.ErrorDetail(field, cv.getMessage());
                })
                .toList();

        ApiError error = ApiError.builder()
                .status(ErrorCode.BAD_REQUEST)
                .message("Validation failed")
                .path(request.getRequestURI())
                .errors(details)
                .build();

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest request) {
        log.error(ex.getMessage(), ex);

        ApiError error = ApiError.builder()
                .status(ErrorCode.INTERNAL_ERROR)
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(500).body(error);
    }

}