package kr.co.ouroboros.core.global.exception;

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
@RestControllerAdvice
@Order(100) // Lower priority - package-specific handlers have higher priority (lower order number)
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles illegal argument exceptions (validation errors).
     * <p>
     * Returns 400 Bad Request when request parameters or data are invalid.
     * <p>
     * Note: Detailed exception message is logged but not exposed to client for security.
     *
     * @param ex the illegal argument exception
     * @return response entity with 400 status and error details
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
     * Handles class cast exceptions (type mismatch errors).
     * <p>
     * Returns 400 Bad Request when data structure contains type mismatches.
     * Logs detailed error information but returns generic message to client.
     *
     * @param ex the class cast exception
     * @return response entity with 400 status and error details
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
     * Handles null pointer exceptions (null value errors).
     * <p>
     * Returns 400 Bad Request when data contains unexpected null values.
     * Logs detailed error information but returns generic message to client.
     *
     * @param ex the null pointer exception
     * @return response entity with 400 status and error details
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
     * Handles all other uncaught exceptions.
     * <p>
     * Returns 500 Internal Server Error as a catch-all for unexpected errors.
     * Logs detailed error information but returns generic message to client.
     * <p>
     * This handler catches any exceptions not handled by package-specific handlers.
     *
     * @param ex the exception
     * @return response entity with 500 status and error details
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

