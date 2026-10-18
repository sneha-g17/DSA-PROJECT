package com.oneroute.api;

import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    public record ApiError(String error, Instant timestamp) { }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> invalidInput(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "Invalid request. Check station IDs, objective, mode, alternatives (1–3) and closure lists.", Instant.now()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> unavailable(IOException exception) {
        LOG.warn("Native routing request failed: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("Routing temporarily unavailable; retry shortly.", Instant.now()));
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<ApiError> interrupted(InterruptedException exception) {
        Thread.currentThread().interrupt();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("Request interrupted.", Instant.now()));
    }
}
