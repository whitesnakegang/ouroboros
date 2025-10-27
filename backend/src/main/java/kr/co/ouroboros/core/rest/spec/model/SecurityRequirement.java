package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents security requirements for an API operation.
 * <p>
 * Defines authentication and authorization schemes required to access an endpoint.
 * Conforms to OpenAPI 3.1.0 Security Requirement Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityRequirement {
    private Map<String, List<String>> schemes;
}