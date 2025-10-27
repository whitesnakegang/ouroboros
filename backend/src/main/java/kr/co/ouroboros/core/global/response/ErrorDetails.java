package kr.co.ouroboros.core.global.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error details for API responses.
 * <p>
 * Contains error code and detailed error message information.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {

    /**
     * Error code (e.g., "DUPLICATE_API", "VALIDATION_ERROR")
     */
    private String code;

    /**
     * Detailed error description
     */
    private String details;
}
