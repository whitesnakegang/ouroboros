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
 * Each key in the requirements map is a security scheme name (defined in components/securitySchemes),
 * and the value is a list of required scopes for that scheme.
 * <p>
 * Example:
 * <pre>
 * {
 *   "api_key": [],
 *   "oauth2": ["read:users", "write:users"]
 * }
 * </pre>
 * Conforms to OpenAPI 3.1.0 Security Requirement Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityRequirement {

    /**
     * Map of security scheme names to required scopes.
     * Key: security scheme name (e.g., "api_key", "oauth2")
     * Value: list of required scopes (empty list for schemes that don't use scopes)
     */
    private Map<String, List<String>> requirements;
}