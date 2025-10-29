package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a complete REST API specification.
 * <p>
 * This is the core domain model that encapsulates all aspects of a REST API endpoint
 * including path, method, parameters, request/response structures, and security requirements.
 * Based on OpenAPI 3.1.0 specification.
 * <p>
 * Each specification is assigned a unique ID (UUID) for identification in update/delete operations.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestApiSpec {
    private String id;
    private String path;
    private String method;
    private String summary;
    private String description;
    private boolean deprecated;
    private List<String> tags;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<String, ApiResponse> responses;
    private List<SecurityRequirement> security;

    // Ouroboros custom fields
    @Builder.Default
    private String progress = "mock";  // mock or completed
    @Builder.Default
    private String tag = "none";       // bugfix, implementing, or none
    @Builder.Default
    private String diff = "none";      // none, request, response, endpoint, or both
}
