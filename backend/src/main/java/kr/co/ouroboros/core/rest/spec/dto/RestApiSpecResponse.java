package kr.co.ouroboros.core.rest.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.ouroboros.core.rest.spec.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for REST API specification operations.
 * <p>
 * Returns the complete REST API specification including all endpoint details
 * and Ouroboros custom metadata fields.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestApiSpecResponse {

    /**
     * Unique identifier (UUID)
     */
    private String id;

    /**
     * API endpoint path (e.g., "/api/users/{id}")
     */
    private String path;

    /**
     * HTTP method (GET, POST, PUT, DELETE, PATCH)
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
     * Validation flag
     */
    private Boolean isValid;
}