package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents an API response specification.
 * <p>
 * Defines the structure of an HTTP response including content types, headers,
 * and response body schema. Conforms to OpenAPI 3.1.0 Response Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String description;
    private Map<String, MediaType> content;
    private Map<String, Header> headers;
}