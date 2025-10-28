package kr.co.ouroboros.core.rest.spec.dto;

import kr.co.ouroboros.core.rest.spec.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing REST API specification.
 * <p>
 * All fields are optional - only provided fields will be updated.
 * The path and method cannot be changed (use delete + create instead).
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestApiRequest {

    /**
     * Brief endpoint description
     */
    private String summary;

    /**
     * Detailed endpoint description
     */
    private String description;

    /**
     * Whether this endpoint is deprecated
     */
    private Boolean deprecated;

    /**
     * Grouping tags
     */
    private List<String> tags;

    /**
     * Query/path/header parameters
     */
    private List<Parameter> parameters;

    /**
     * Request body specification
     */
    private RequestBody requestBody;

    /**
     * Response definitions (key: status code)
     */
    private Map<String, ApiResponse> responses;

    /**
     * Security requirements
     */
    private List<SecurityRequirement> security;

    /**
     * Development progress (mock or completed)
     */
    private String progress;

    /**
     * Development tag (none, implementing, bugfix)
     */
    private String tag;

    /**
     * Validation flag
     */
    private Boolean isValid;
}