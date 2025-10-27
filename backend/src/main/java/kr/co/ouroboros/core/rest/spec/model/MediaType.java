package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a media type definition.
 * <p>
 * Associates a schema with a specific content type (e.g., application/json).
 * Conforms to OpenAPI 3.1.0 Media Type Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaType {
    private Schema schema;
}