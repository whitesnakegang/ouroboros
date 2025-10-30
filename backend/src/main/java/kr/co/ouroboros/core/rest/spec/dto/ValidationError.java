package kr.co.ouroboros.core.rest.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error encountered during YAML import.
 * <p>
 * Provides detailed information about where the error occurred,
 * the type of error, and a descriptive message.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    /**
     * Location in the YAML where the error occurred.
     * Example: "paths./api/users.get", "info.version", "components.schemas.User"
     */
    private String location;

    /**
     * Error code identifying the type of validation failure.
     * Examples: "MISSING_REQUIRED_FIELD", "INVALID_HTTP_METHOD", "INVALID_DATA_TYPE"
     */
    private String errorCode;

    /**
     * Human-readable error message describing the problem
     */
    private String message;
}