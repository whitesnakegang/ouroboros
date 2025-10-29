package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an HTTP response header specification.
 * <p>
 * Defines response headers with their schemas, descriptions, and requirement status.
 * Conforms to OpenAPI 3.1.0 Header Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header {

    /**
     * Header description
     */
    private String description;

    /**
     * Whether this header is required in the response (default: false)
     */
    private boolean required;

    /**
     * Header value schema
     */
    private Schema schema;
}
