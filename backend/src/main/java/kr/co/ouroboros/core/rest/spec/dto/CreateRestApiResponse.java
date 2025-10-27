package kr.co.ouroboros.core.rest.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for REST API specification creation.
 * <p>
 * Returns the result of the API specification creation operation including
 * success status, message, the generated ID, and the path to the generated YAML file.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestApiResponse {
    private boolean success;
    private String message;
    private String id;
    private String filePath;
}
