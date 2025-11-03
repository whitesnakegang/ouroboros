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
 * If path or method is changed, the operation will be moved to the new location.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestApiRequest {

    /**
     * API endpoint path (e.g., "/api/users/{id}")
     * <p>
     * If changed, the operation will be moved from the old path to the new path
     */
    private String path;

    /**
     * HTTP method (GET, POST, PUT, DELETE, PATCH)
     * <p>
     * If changed, the operation will be moved from the old method to the new method
     */
    private String method;

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
     * Diff status (none, request, response, endpoint, both)
     */
    private String diff;
}