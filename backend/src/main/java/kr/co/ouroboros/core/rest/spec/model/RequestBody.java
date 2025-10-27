package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a request body specification.
 * <p>
 * Defines the structure and content types of an HTTP request body.
 * Conforms to OpenAPI 3.1.0 Request Body Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestBody {
    private String description;
    private boolean required;
    private Map<String, MediaType> content;
}