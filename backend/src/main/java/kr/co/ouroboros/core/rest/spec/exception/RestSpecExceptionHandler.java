package kr.co.ouroboros.core.rest.spec.exception;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST API specification endpoints.
 * <p>
 * Catches exceptions thrown by REST spec controllers and services, then converts them into
 * standardized {@link GlobalApiResponse} format with appropriate HTTP status codes.
 * <p>
 * This handler only applies to controllers in the {@code kr.co.ouroboros.ui.controller} package
 * (REST API spec and schema controllers).
 * <p>
 * <b>Handled Exceptions:</b>
 * <ul>
 *   <li>{@link DuplicateApiSpecException} - 409 Conflict</li>
 *   <li>{@link IllegalArgumentException} - 400 Bad Request</li>
 *   <li>{@link ClassCastException} - 400 Bad Request (YAML format error)</li>
 *   <li>{@link NullPointerException} - 400 Bad Request (YAML null value error)</li>
 *   <li>{@link Exception} - 500 Internal Server Error (catch-all)</li>
 * </ul>
 *
 * @since 0.0.1
 */
@RestControllerAdvice(basePackages = "kr.co.ouroboros.ui.controller")
@Slf4j
public class RestSpecExceptionHandler {

    /**
     * Handles duplicate API specification exceptions.
     * <p>
     * Returns 409 Conflict when attempting to create an API spec that already exists.
     *
     * @param ex the duplicate API spec exception
     * @return response entity with 409 status and error details
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

    /**
     * Handles illegal argument exceptions (business logic validation errors).
     * <p>
     * Returns 400 Bad Request when request parameters or data are invalid.
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
     * Handles class cast exceptions (YAML parsing errors).
     * <p>
     * Returns 400 Bad Request when YAML structure contains type mismatches.
     * Logs detailed error information but returns generic message to client.
     *
     * @param ex the class cast exception
     * @return response entity with 400 status and error details
     */
    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleClassCast(ClassCastException ex) {
        // Log detailed error for debugging
        log.error("Specification format error (type mismatch): {}", ex.getMessage(), ex);

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Failed to retrieve API specifications",
                "INVALID_FORMAT",
                "Invalid specification format"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles null pointer exceptions (specification structure errors).
     * <p>
     * Returns 400 Bad Request when specification contains unexpected null values.
     * Logs detailed error information but returns generic message to client.
     *
     * @param ex the null pointer exception
     * @return response entity with 400 status and error details
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<GlobalApiResponse<Void>> handleNullPointer(NullPointerException ex) {
        // Log detailed error for debugging
        log.error("Specification structure error (null value): {}", ex.getMessage(), ex);

        // Return generic message to client
        GlobalApiResponse<Void> response = GlobalApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Failed to retrieve API specifications",
                "INVALID_STRUCTURE",
                "Invalid specification structure"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all other uncaught exceptions.
     * <p>
     * Returns 500 Internal Server Error as a catch-all for unexpected errors.
     * Logs detailed error information but returns generic message to client.
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