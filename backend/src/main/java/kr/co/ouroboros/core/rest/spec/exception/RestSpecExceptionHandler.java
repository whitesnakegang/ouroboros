package kr.co.ouroboros.core.rest.spec.exception;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for REST API specification endpoints.
 * <p>
 * Handles package-specific exceptions for REST spec controllers and services.
 * Converts them into standardized {@link GlobalApiResponse} format with appropriate HTTP status codes.
 * <p>
 * This handler has higher priority than {@link kr.co.ouroboros.core.global.exception.GlobalExceptionHandler}
 * and only applies to controllers in the {@code kr.co.ouroboros.ui.controller} package
 * (REST API spec and schema controllers).
 * <p>
 * <b>Handled Exceptions:</b>
 * <ul>
 *   <li>{@link DuplicateApiSpecException} - 409 Conflict (package-specific)</li>
 * </ul>
 * <p>
 * Common exceptions ({@link IllegalArgumentException}, {@link ClassCastException},
 * {@link NullPointerException}, {@link Exception}) are handled by
 * {@link kr.co.ouroboros.core.global.exception.GlobalExceptionHandler}.
 *
 * @since 0.0.1
 */
@RestControllerAdvice(basePackages = "kr.co.ouroboros.ui.controller")
@Order(10) // Higher priority than GlobalExceptionHandler
@Slf4j
public class RestSpecExceptionHandler {

    /**
     * Converts a DuplicateApiSpecException into a standardized 409 Conflict API response.
     *
     * @param ex the DuplicateApiSpecException that was thrown
     * @return a ResponseEntity containing a GlobalApiResponse<Void> with HTTP status 409 and error details
     */
    @ExceptionHandler(DuplicateApiSpecException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleDuplicateApiSpec(DuplicateApiSpecException ex) {
        // Log detailed error for debugging
        log.error("Duplicate API specification: {}", ex.getMessage());

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.CONFLICT.value(),
                "API specification already exists",
                "DUPLICATE_API",
                "An API specification with the same path and method already exists"
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}