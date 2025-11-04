package kr.co.ouroboros.core.rest.mock.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata information for a single mock endpoint
 * managed by the {@code RestMockRegistry}.
 *
 * <p>This class contains the essential information required to
 * identify, validate, and generate mock responses for a specific
 * API endpoint defined in the OpenAPI specification (YAML or JSON).</p>
 *
 * <p>Each {@link EndpointMeta} instance corresponds to a unique
 * combination of HTTP method and request path, and may include
 * required headers, authentication requirements, and multiple
 * possible response definitions by status code.</p>
 *
 * <p>Example:
 * <pre>{@code
 * EndpointMeta meta = EndpointMeta.builder()
 *      .id("user-get-001")
 *      .path("/api/users/{id}")
 *      .method("GET")
 *      .status("mock")
 *      .requiredHeaders(List.of("X-Client-Id"))
 *      .authHeaders(List.of("Authorization"))
 *      .responses(Map.of(
 *          200, new ResponseMeta(200, "OK", Map.of("id", 1, "name", "Alice")),
 *          401, new ResponseMeta(401, "Unauthorized", Map.of("error", "Unauthorized"))
 *      ))
 *      .build();
 * }</pre>
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointMeta {
    private String id;
    private String path;
    private String method;
    private String status;
    private List<String> requiredHeaders;      // 일반 필수 헤더 (400 반환)
    private List<String> authHeaders;          // 인증 필수 헤더 (401 반환)
    private List<String> requiredParams;
    private Map<Integer, RestResponseMeta> responses;

    // Request body validation metadata
    private boolean requestBodyRequired;       // requestBody의 required 필드
    private Map<String, Object> requestBodySchema;  // resolved schema (타입, 필수 필드 검증용)
}
