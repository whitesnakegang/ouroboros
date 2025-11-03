package kr.co.ouroboros.core.rest.spec.dto;

import kr.co.ouroboros.core.rest.spec.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new REST API specification.
 * <p>
 * Contains all necessary information to define a complete REST API endpoint
 * including path, method, parameters, request/response structures, and security settings.
 * <p>
 * The {@code id} field is optional - if not provided, a UUID will be automatically generated.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestApiRequest {
    private String id;
    private String path;
    private String method;
    private String summary;
    private String description;
    private List<String> tags;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<String, ApiResponse> responses;
    private List<SecurityRequirement> security;
}
