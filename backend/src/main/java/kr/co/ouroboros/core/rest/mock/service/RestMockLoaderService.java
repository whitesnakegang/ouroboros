package kr.co.ouroboros.core.rest.mock.service;

import kr.co.ouroboros.core.global.mock.model.ResponseMeta;
import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service responsible for loading and parsing OpenAPI YAML definitions
 * into {@link EndpointMeta} objects used by the mock registry.
 *
 * <p>This service acts as the bridge between the raw OpenAPI spec
 * and the runtime mock system. It extracts paths, methods, parameters,
 * security requirements, and responses, resolving any schema references
 * ({@code $ref}) recursively into concrete mockable definitions.</p>
 *
 * <h2>Main Responsibilities</h2>
 * <ul>
 *     <li>Reads the OpenAPI YAML document via {@link RestApiYamlParser}.</li>
 *     <li>Resolves <b>$ref</b> schemas inside components/schemas.</li>
 *     <li>Extracts mock-only endpoints (with <b>x-ouroboros-progress: mock</b>).</li>
 *     <li>Builds {@link EndpointMeta} objects with all metadata required for mock generation.</li>
 * </ul>
 *
 * <p>Endpoints are keyed as {@code "METHOD:/path"} (e.g. {@code GET:/api/users/{id}}).</p>
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestMockLoaderService {
    private final RestApiYamlParser parser;

    /**
     * Load the OpenAPI YAML, parse its paths into EndpointMeta objects, and resolve all `$ref` schema references.
     *
     * The returned map is keyed by "METHOD:/path" (for example, "GET:/api/users"). If the YAML file is missing,
     * contains no paths, or parsing fails, an empty map is returned.
     *
     * @return a map from "METHOD:/path" to the corresponding EndpointMeta; may be empty if no endpoints are available
     */
    public Map<String, EndpointMeta> loadFromYaml() {
        try {
            // ===== 1. YAML 파일 존재 확인 =====
            if (!parser.fileExists()) {
                log.info("YAML file does not exist");
                return Collections.emptyMap();
            }
            // ===== 2. YAML 문서 읽기 =====
            Map<String, Object> openApiDoc = parser.readDocument();

            // ===== 3. components/schemas 추출 =====
            // $ref 참조를 해결하기 위해 필요
            Map<String, Object> schemas = parser.getSchemas(openApiDoc);
            if (schemas == null) {
                schemas = Collections.emptyMap();
            }

            // ===== 3-1. components/securitySchemes 추출 =====
            // 인증 헤더 이름 확인용 (Authorization, X-API-Key 등)
            Map<String, Object> securitySchemes = extractSecuritySchemes(openApiDoc);

            // ===== 4. paths 추출 =====
            @SuppressWarnings("unchecked")
            Map<String, Object> paths = (Map<String, Object>) openApiDoc.get("paths");
            if (paths == null || paths.isEmpty()) {
                log.info("No paths found in YAML");
                return Collections.emptyMap();
            }

            Map<String, EndpointMeta> endpoints = new LinkedHashMap<>();

            // ===== 5. 각 path와 method를 순회하며 EndpointMeta 생성 =====
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

                for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                    String method = methodEntry.getKey().toLowerCase();

                    // HTTP 메서드가 아닌 필드는 스킵
                    if (!isHttpMethod(method)) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();
                    // operation을 파싱하여 EndpointMeta 생성
                    EndpointMeta meta = parseOperation(path, method, operation, schemas, securitySchemes);

                    if (meta != null) {
                        String key = method.toUpperCase() + ":" + path; // 예: "GET:/api/users"
                        endpoints.put(key, meta);
                        log.debug("Parsed endpoint: {} {}", method.toUpperCase(), path);
                    }
                }
            }

            log.info("Parsed {} endpoints from YAML", endpoints.size());
            return endpoints;

        } catch (Exception e) {
            log.error("Failed to load endpoints from YAML", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 단일 OpenAPI operation을 파싱하여 EndpointMeta로 변환
     * Parses a single OpenAPI operation into EndpointMeta with security scheme resolution.
     * <p>
     * Only operations with {@code x-ouroboros-progress: mock} are parsed and registered.
     * <p>
     * Security scheme resolution:
     * <ul>
     *   <li><b>HTTP auth</b> (Bearer, Basic): Uses {@code Authorization} header</li>
     *   <li><b>API Key</b>: Uses custom header name from scheme definition (e.g., {@code X-API-Key})</li>
     *   <li><b>OAuth2/OpenID</b>: Uses {@code Authorization} header</li>
     * </ul>
     * <p>
     * Example security resolution:
     * <pre>
     * security: [{ ApiKeyAuth: [] }]
     * securitySchemes:
     *   ApiKeyAuth:
     *     type: apiKey
     *     in: header
     *     name: X-API-Key  ← Actual header name resolved
     * Result: authHeaders = ["X-API-Key"]
     * </pre>
     *
     * @param path the endpoint path
     * @param method the HTTP method
     * @param operation the OpenAPI operation object
     * @param schemas the components/schemas map for $ref resolution
     * @param securitySchemes the components/securitySchemes map for auth header resolution
     * @return parsed EndpointMeta, or null if not a mock endpoint
     */
    @SuppressWarnings("unchecked")
    private EndpointMeta parseOperation(String path, String method, Map<String, Object> operation,
                                       Map<String, Object> schemas, Map<String, Object> securitySchemes) {
        // ===== Ouroboros 커스텀 필드 추출 =====
        String id = (String) operation.get("x-ouroboros-id");
        String progress = (String) operation.get("x-ouroboros-progress");
        // mock이 아닌 endpoint는 registry에 등록하지 않음
        if (!"mock".equalsIgnoreCase(progress)) {
            return null;
        }

        // ===== 필수 파라미터 추출 =====
        List<String> requiredHeaders = new ArrayList<>(); // 일반 필수 헤더 (400)
        List<String> requiredParams = new ArrayList<>();  // 필수 쿼리 파라미터 (400)
        List<String> authHeaders = new ArrayList<>();  // 인증 헤더 (401)

        // parameters 필드에서 필수 헤더/파라미터 추출
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.get("parameters");
        if (parameters != null) {
            for (Map<String, Object> param : parameters) {
                Boolean required = (Boolean) param.get("required");
                if (Boolean.TRUE.equals(required)) {
                    String name = (String) param.get("name");
                    String in = (String) param.get("in");

                    if ("header".equals(in)) {
                        // 예: X-Request-ID, X-Tenant-ID
                        requiredHeaders.add(name);
                    } else if ("query".equals(in)) {
                        // 예: userId, role
                        requiredParams.add(name);
                    }
                }
            }
        }

        // ===== 보안 요구사항 파싱 및 실제 헤더 이름 해결 =====
        // security 필드에서 인증 헤더 추출
        List<Map<String, Object>> security = (List<Map<String, Object>>) operation.get("security");
        if (security != null && !security.isEmpty()) {
            for (Map<String, Object> requirement : security) {
                for (String schemeName : requirement.keySet()) {
                    // securitySchemes에서 실제 정의 가져오기
                    Map<String, Object> scheme = (Map<String, Object>) securitySchemes.getOrDefault(schemeName, Collections.emptyMap());
                    String type = (String) scheme.get("type");

                    if ("http".equals(type)) {
                        // HTTP 인증 (Bearer, Basic 등) → Authorization 헤더
                        authHeaders.add("Authorization");
                    } else if ("apiKey".equals(type) && "header".equals(scheme.get("in"))) {
                        // API Key 인증 → 스키마에 정의된 헤더 이름 사용
                        // 예: X-API-Key, X-Auth-Token 등
                        authHeaders.add((String) scheme.get("name"));
                    } else if ("oauth2".equals(type) || "openIdConnect".equals(type)) {
                        // OAuth2, OpenID Connect → Authorization 헤더
                        authHeaders.add("Authorization");
                    }
                }
            }
        }

        // ===== Request Body 파싱 =====
        boolean requestBodyRequired = false;  // body가 필수인지
        Map<String, Object> requestBodySchema = null;  // body의 스키마 (타입/필드 검증용)
        Map<String, Object> requestBody = (Map<String, Object>) operation.get("requestBody");
        if (requestBody != null) {
            requestBodyRequired = Boolean.TRUE.equals(requestBody.get("required"));
            // content.application/json.schema 추출
            Map<String, Object> content = (Map<String, Object>) requestBody.get("content");
            if (content != null) {
                Map<String, Object> mediaType = (Map<String, Object>) content.get("application/json");
                if (mediaType != null) {
                    Map<String, Object> schema = (Map<String, Object>) mediaType.get("schema");
                    if (schema != null) {
                        // $ref가 있으면 실제 스키마로 치환
                        // 예: {$ref: '#/components/schemas/User'} → {type: 'object', properties: {...}}
                        requestBodySchema = resolveSchema(schema, schemas, new HashSet<>());
                    }
                }
            }
        }

        // ===== Response 파싱 =====
        Map<Integer, ResponseMeta> responses = new HashMap<>();
        Map<String, Object> responsesMap = (Map<String, Object>) operation.get("responses");
        if (responsesMap != null) {
            for (Map.Entry<String, Object> responseEntry : responsesMap.entrySet()) {
                try {
                    // "200", "400" 등을 숫자로 변환
                    int statusCode = Integer.parseInt(responseEntry.getKey());
                    Map<String, Object> responseObj = (Map<String, Object>) responseEntry.getValue();

                    ResponseMeta responseMeta = parseResponse(statusCode, responseObj, schemas);
                    if (responseMeta != null) {
                        responses.put(statusCode, responseMeta);
                    }
                } catch (NumberFormatException e) {
                    // 비숫자 status code는 스킵
                }
            }
        }

        // ===== EndpointMeta 빌드 =====
        return EndpointMeta.builder()
                .id(id)
                .path(path)
                .method(method.toUpperCase())
                .status(progress)
                .requiredHeaders(requiredHeaders)
                .authHeaders(authHeaders)
                .requiredParams(requiredParams)
                .requestBodyRequired(requestBodyRequired)
                .requestBodySchema(requestBodySchema)
                .responses(responses)
                .build();
    }

    /**
     * Parse an OpenAPI response object and produce a ResponseMeta describing its status, body schema, and content type.
     *
     * @param statusCode the HTTP status code for the response
     * @param response   the OpenAPI response object (may contain a `content` map)
     * @param schemas    the components/schemas map used to resolve `$ref` references
     * @return the parsed ResponseMeta, or `null` if the response has no content or no resolvable schema
     */
    @SuppressWarnings("unchecked")
    private ResponseMeta parseResponse(int statusCode, Map<String, Object> response, Map<String, Object> schemas) {
        Map<String, Object> content = (Map<String, Object>) response.get("content");
        if (content == null) {
            return null;
        }

        // JSON 또는 XML content 타입 확인
        String contentType = null;
        Map<String, Object> schema = null;

        if (content.containsKey("application/json")) {
            contentType = "application/json";
            Map<String, Object> mediaType = (Map<String, Object>) content.get("application/json");
            schema = (Map<String, Object>) mediaType.get("schema");
        } else if (content.containsKey("application/xml")) {
            contentType = "application/xml";
            Map<String, Object> mediaType = (Map<String, Object>) content.get("application/xml");
            schema = (Map<String, Object>) mediaType.get("schema");
        }

        if (schema == null) {
            return null;
        }

        // $ref 있으면 실제 스키마로 치환
        Map<String, Object> resolvedSchema = resolveSchema(schema, schemas, new HashSet<>());

        return ResponseMeta.builder()
                .statusCode(statusCode)
                .body(resolvedSchema)
                .contentType(contentType)
                .build();
    }

    /**
     * Recursively resolves `$ref` references in an OpenAPI schema to their concrete schema definitions.
     *
     * <p>If `schema` is null, a referenced schema is missing, the `$ref` format is invalid, or a circular
     * reference is detected, an empty map is returned.</p>
     *
     * @param schema  the schema that may contain `$ref` entries
     * @param schemas the components/schemas map used to lookup referenced schemas by name
     * @param visited a set of visited `$ref` strings to detect and prevent circular references
     * @return a resolved schema map with `$ref` references replaced by their definitions, or an empty map on error or circular reference
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveSchema(Map<String, Object> schema, Map<String, Object> schemas, Set<String> visited) {
        if (schema == null) {
            return Collections.emptyMap();
        }

        // ===== $ref 처리 =====
        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref"); // 예: "#/components/schemas/User"

            // 순환 참조 방지 (A → B → A)
            if (visited.contains(ref)) {
                log.warn("Circular reference detected: {}", ref);
                return Collections.emptyMap();
            }
            visited.add(ref);

            // ref에서 스키마 이름 추출: "#/components/schemas/User" → "User"
            String schemaName = extractSchemaName(ref);
            if (schemaName == null) {
                log.warn("Invalid $ref format: {}", ref);
                return Collections.emptyMap();
            }

            // schemas 맵에서 실제 스키마 가져오기
            Map<String, Object> referencedSchema = (Map<String, Object>) schemas.get(schemaName);
            if (referencedSchema == null) {
                log.warn("Schema not found: {}", schemaName);
                return Collections.emptyMap();
            }

            // 참조된 스키마를 재귀적으로 해결 (중첩 $ref 처리)
            return resolveSchema(referencedSchema, schemas, visited);
        }

        // ===== 원본 스키마 복사 (수정 방지) =====
        Map<String, Object> resolved = new LinkedHashMap<>(schema);

        // ===== properties 재귀 해결 =====
        // properties: {id: {type: 'string'}, address: {$ref: '#/components/schemas/Address'}}
        if (resolved.containsKey("properties")) {
            Map<String, Object> properties = (Map<String, Object>) resolved.get("properties");
            Map<String, Object> resolvedProperties = new LinkedHashMap<>();

            for (Map.Entry<String, Object> prop : properties.entrySet()) {
                Map<String, Object> propSchema = (Map<String, Object>) prop.getValue();
                resolvedProperties.put(prop.getKey(), resolveSchema(propSchema, schemas, new HashSet<>(visited)));
            }

            resolved.put("properties", resolvedProperties);
        }

        // ===== array items 재귀 해결 =====
        // items: {$ref: '#/components/schemas/Address'}
        if (resolved.containsKey("items")) {
            Map<String, Object> items = (Map<String, Object>) resolved.get("items");
            resolved.put("items", resolveSchema(items, schemas, new HashSet<>(visited)));
        }

        return resolved;
    }

    /**
     * Extracts the schema name from an OpenAPI `$ref` string.
     *
     * @param ref the `$ref` string in the form "#/components/schemas/Name" (e.g. "#/components/schemas/User")
     * @return the schema name (e.g. "User"), or `null` if the `$ref` is null or not in the expected format
     */
    private String extractSchemaName(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }

        // "/" 기준으로 분리: ["#", "components", "schemas", "User"]
        String[] parts = ref.split("/");
        if (parts.length >= 4) {
            return parts[3]; // "User"
        }

        return null;
    }

    /**
     * Determine whether a string is one of the standard HTTP method names.
     *
     * @param method the method name to test (case-sensitive; expected values: "get", "post", "put", "delete", "patch", "options", "head", "trace")
     * @return `true` if the input matches one of the listed HTTP methods, `false` otherwise
     */
    private boolean isHttpMethod(String method) {
        return method.matches("get|post|put|delete|patch|options|head|trace");
    }

    /**
     * Extract securitySchemes from components.
     *
     * @param openApiDoc the OpenAPI document
     * @return map of security scheme name to scheme definition
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSecuritySchemes(Map<String, Object> openApiDoc) {
        Map<String, Object> components = (Map<String, Object>) openApiDoc.get("components");
        if (components == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> schemes = (Map<String, Object>) components.get("securitySchemes");
        return schemes != null ? schemes : Collections.emptyMap();
    }
}