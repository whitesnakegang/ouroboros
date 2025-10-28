package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an HTTP header specification.
 * <p>
 * Defines response headers with their schemas and descriptions.
 * Conforms to OpenAPI 3.1.0 Header Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header {
    private String description;
    private Schema schema;
}
