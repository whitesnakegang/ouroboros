package kr.co.ouroboros.core.global.exception;

import jakarta.validation.ConstraintViolationException;
import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all Ouroboros SDK endpoints.
 * <p>
 * Handles common exceptions that apply to all Ouroboros SDK controllers.
 * This handler has lower priority (higher order number) than package-specific handlers
 * to allow specialized exception handling to take precedence.
 * <p>
 * <b>Handled Exceptions:</b>
 * <ul>
 *   <li>{@link ConstraintViolationException} - 400 Bad Request (validation errors)</li>
 *   <li>{@link IllegalArgumentException} - 400 Bad Request</li>
 *   <li>{@link ClassCastException} - 400 Bad Request (type mismatch error)</li>
 *   <li>{@link NullPointerException} - 400 Bad Request (null value error)</li>
 *   <li>{@link Exception} - 500 Internal Server Error (catch-all)</li>
 * </ul>
 * <p>
 * <b>Exception Handling Priority:</b>
 * <ol>
 *   <li>Package-specific handlers (e.g., RestSpecExceptionHandler, TryExceptionHandler) - handle specialized exceptions</li>
 *   <li>This global handler - handles common exceptions for all SDK endpoints</li>
 * </ol>
 * <p>
 * <p>
 * <b>Note:</b> This handler applies to all controllers in the application.
 * Package-specific handlers (e.g., RestSpecExceptionHandler, TryExceptionHandler) take precedence
 * for their respective packages due to higher priority (@Order(10)).
 *
 * @since 0.0.1
 */
@RestControllerAdvice(basePackages = "kr.co.ouroboros")
@Order(100) // Lower priority - package-specific handlers have higher priority (lower order number)
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles constraint violation exceptions (Bean Validation errors).
     * <p>
     * Returns 400 Bad Request when request parameters fail validation (e.g., @Min, @Max).
     * <p>
     * Note: Detailed exception message is logged but not exposed to client for security.
     *
     * @param ex the constraint violation exception
     * @return response entity with 400 status and error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        // Log detailed error for debugging
        log.error("Validation constraint violation: {}", ex.getMessage());

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request data",
                "VALIDATION_ERROR",
                "The request parameters are invalid"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
         * Handle IllegalArgumentException thrown for invalid request parameters or data.
         *
         * Returns an HTTP 400 Bad Request with a generic error payload; detailed exception information is logged but not exposed to the client.
         *
         * @param ex the IllegalArgumentException caused by invalid input
         * @return a ResponseEntity containing a GlobalApiResponse<Void> with HTTP 400 and an error code indicating an invalid request
         */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        // Log detailed error for debugging
        log.error("Invalid argument: {}", ex.getMessage());

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request data",
                "INVALID_REQUEST",
                "The request contains invalid data"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Produce a standardized 400 Bad Request response for type mismatch errors represented by ClassCastException.
     *
     * @param ex the caught ClassCastException
     * @return a ResponseEntity containing a GlobalApiResponse<Void> with HTTP 400 and an `INVALID_FORMAT` error indicating an invalid data format
     */
    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleClassCast(ClassCastException ex) {
        // Log detailed error for debugging
        log.error("Type mismatch error: {}", ex.getMessage(), ex);

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid data format",
                "INVALID_FORMAT",
                "The request contains invalid data format"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle NullPointerException thrown during request processing and return a 400 Bad Request response.
     *
     * @param ex the NullPointerException that was thrown
     * @return a GlobalApiResponse<Void> with HTTP status 400 and an error payload indicating an invalid data structure
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleNullPointer(NullPointerException ex) {
        // Log detailed error for debugging
        log.error("Null value error: {}", ex.getMessage(), ex);

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid data structure",
                "INVALID_STRUCTURE",
                "The request contains invalid data structure"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Catch-all handler for uncaught exceptions thrown by controllers.
     *
     * Constructs a standardized error response with HTTP 500 and error code `INTERNAL_ERROR`.
     *
     * @param ex the uncaught exception
     * @return a ResponseEntity containing a GlobalApiResponse<Void> with HTTP 500 and error code `INTERNAL_ERROR`
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleGeneral(Exception ex) {
        // Log detailed error for debugging
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Failed to process request",
                "INTERNAL_ERROR",
                "An internal error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
