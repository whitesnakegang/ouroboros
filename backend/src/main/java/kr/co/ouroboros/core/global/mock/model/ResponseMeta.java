package kr.co.ouroboros.core.global.mock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Metadata for a mock response definition from OpenAPI spec.
 * <p>
 * Contains all information needed to generate a mock HTTP response,
 * including status code, headers, body schema, and content type.
 * <p>
 * The body schema is a fully resolved JSON schema (all $ref references expanded)
 * ready for mock data generation by {@link kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder}.
 *
 * @since 0.0.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseMeta {
    /** HTTP status code for this response (e.g., 200, 400, 404) */
    private int statusCode;

    /** Optional response headers to include in the mock response */
    private Map<String, Object> headers;

    /** JSON schema of the response body, fully resolved (no $ref) */
    private Map<String, Object> body;

    /** Content type of the response (e.g., "application/json", "application/xml") */
    private String contentType;
}
