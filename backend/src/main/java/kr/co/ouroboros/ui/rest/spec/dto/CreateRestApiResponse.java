package kr.co.ouroboros.ui.rest.spec.dto;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data object for REST API specification creation response.
 * <p>
 * Contains the generated ID and file path of the created API specification.
 * Used as the data field in the standard {@link GlobalApiResponse}.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestApiResponse {
    /**
     * Generated unique ID (UUID) for the API specification
     */
    private String id;

    /**
     * File path where the OpenAPI YAML was saved
     */
    private String filePath;
}
