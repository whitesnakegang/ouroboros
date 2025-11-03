package kr.co.ouroboros.core.rest.tryit.exception;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for Try endpoints.
 * <p>
 * Handles package-specific exceptions for Try controllers and services.
 * Converts them into standardized {@link GlobalApiResponse} format with appropriate HTTP status codes.
 * <p>
 * This handler has higher priority than {@link kr.co.ouroboros.core.global.exception.GlobalExceptionHandler}
 * and only applies to controllers in the {@code kr.co.ouroboros.ui.rest.tryit.controller} package.
 * <p>
 * <b>Handled Exceptions:</b>
 * <ul>
 *   <li>{@link InvalidTryIdException} - 400 Bad Request (package-specific)</li>
 * </ul>
 * <p>
 * Common exceptions ({@link IllegalArgumentException}, {@link Exception}) are handled by
 * {@link kr.co.ouroboros.core.global.exception.GlobalExceptionHandler}.
 *
 * @since 0.0.1
 */
@RestControllerAdvice(basePackages = "kr.co.ouroboros.ui.rest.tryit.controller")
@Order(10) // Higher priority than GlobalExceptionHandler
@Slf4j
public class TryExceptionHandler {

    /**
     * Handles invalid tryId format exceptions.
     * <p>
     * Returns 400 Bad Request when tryId format is invalid (not a valid UUID).
     *
     * @param ex the invalid tryId exception
     * @return response entity with 400 status and error details
     */
    @ExceptionHandler(InvalidTryIdException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleInvalidTryId(InvalidTryIdException ex) {
        // Log detailed error for debugging
        log.warn("Invalid tryId format: {}", ex.getMessage());

        // Return error message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid tryId format",
                "INVALID_TRY_ID",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}

