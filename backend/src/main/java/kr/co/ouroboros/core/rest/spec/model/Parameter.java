package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a request parameter in a REST API specification.
 * <p>
 * Parameters can be located in different parts of the request (query, path, header, cookie)
 * as specified by the {@code in} field. Conforms to OpenAPI 3.1.0 Parameter Object.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {
    private String name;
    private String in;
    private String description;
    private boolean required;
    private Schema schema;
}