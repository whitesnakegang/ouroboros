package kr.co.ouroboros.ui.websocket.spec.dto;

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
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {
    /**
     * Location in the YAML where the error occurred.
     * Example: "channels.userMessages", "operations.sendMessage.action", "components.schemas.User"
     */
    private String location;

    /**
     * Error code identifying the type of validation failure.
     * Examples: "MISSING_REQUIRED_FIELD", "INVALID_ACTION", "INVALID_DATA_TYPE"
     */
    private String errorCode;

    /**
     * Human-readable error message describing the problem
     */
    private String message;
}