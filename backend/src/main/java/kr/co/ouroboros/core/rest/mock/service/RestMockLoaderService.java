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
     * Parse OpenAPI YAML file and convert to EndpointMeta map.
     * Resolves all $ref references in response schemas.
     *
     * @return map of "METHOD:path" to EndpointMeta
     */
    public Map<String, EndpointMeta> loadFromYaml() {
        try {
            // 1. YAML 파일 존재 확인
            if (!parser.fileExists()) {
                log.info("YAML file does not exist");
                return Collections.emptyMap();
            }
            // 2. YAML 문서 읽기
            Map<String, Object> openApiDoc = parser.readDocument();

            // 3. components/schemas 추출
            Map<String, Object> schemas = parser.getSchemas(openApiDoc);
            if (schemas == null) {
                schemas = Collections.emptyMap();
            }

            // 3-1. components/securitySchemes 추출
            Map<String, Object> securitySchemes = extractSecuritySchemes(openApiDoc);

            // 4. paths 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> paths = (Map<String, Object>) openApiDoc.get("paths");
            if (paths == null || paths.isEmpty()) {
                log.info("No paths found in YAML");
                return Collections.emptyMap();
            }

            Map<String, EndpointMeta> endpoints = new LinkedHashMap<>();

            // 5. 각 path와 method를 순회하며 EndpointMeta 생성
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

                for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                    String method = methodEntry.getKey().toLowerCase();

                    // Skip non-HTTP method fields
                    if (!isHttpMethod(method)) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();
                    EndpointMeta meta = parseOperation(path, method, operation, schemas, securitySchemes);

                    if (meta != null) {
                        String key = method.toUpperCase() + ":" + path;
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
        // Extract Ouroboros custom fields
        String id = (String) operation.get("x-ouroboros-id");
        String progress = (String) operation.get("x-ouroboros-progress");
        if (!"mock".equalsIgnoreCase(progress)) {
            return null;  // mock이 아닌 endpoint는 registry에 등록하지 않음
        }

        // Extract required parameters
        List<String> requiredHeaders = new ArrayList<>();
        List<String> requiredParams = new ArrayList<>();
        List<String> authHeaders = new ArrayList<>();

        List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.get("parameters");
        if (parameters != null) {
            for (Map<String, Object> param : parameters) {
                Boolean required = (Boolean) param.get("required");
                if (Boolean.TRUE.equals(required)) {
                    String name = (String) param.get("name");
                    String in = (String) param.get("in");

                    if ("header".equals(in)) {
                        requiredHeaders.add(name);
                    } else if ("query".equals(in)) {
                        requiredParams.add(name);
                    }
                }
            }
        }

        // Parse security requirements and resolve actual header names from securitySchemes
        List<Map<String, Object>> security = (List<Map<String, Object>>) operation.get("security");
        if (security != null && !security.isEmpty()) {
            for (Map<String, Object> requirement : security) {
                for (String schemeName : requirement.keySet()) {
                    Map<String, Object> scheme = (Map<String, Object>) securitySchemes.getOrDefault(schemeName, Collections.emptyMap());
                    String type = (String) scheme.get("type");

                    if ("http".equals(type)) {
                        // HTTP authentication (Bearer, Basic, etc.): always uses Authorization header
                        authHeaders.add("Authorization");
                    } else if ("apiKey".equals(type) && "header".equals(scheme.get("in"))) {
                        // API Key authentication: use custom header name from scheme definition
                        authHeaders.add((String) scheme.get("name"));
                    } else if ("oauth2".equals(type) || "openIdConnect".equals(type)) {
                        // OAuth2 and OpenID Connect: use Authorization header
                        authHeaders.add("Authorization");
                    }
                }
            }
        }

        // Parse responses
        Map<Integer, ResponseMeta> responses = new HashMap<>();
        Map<String, Object> responsesMap = (Map<String, Object>) operation.get("responses");
        if (responsesMap != null) {
            for (Map.Entry<String, Object> responseEntry : responsesMap.entrySet()) {
                try {
                    int statusCode = Integer.parseInt(responseEntry.getKey());
                    Map<String, Object> responseObj = (Map<String, Object>) responseEntry.getValue();

                    ResponseMeta responseMeta = parseResponse(statusCode, responseObj, schemas);
                    if (responseMeta != null) {
                        responses.put(statusCode, responseMeta);
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric status codes (like "default")
                }
            }
        }

        return EndpointMeta.builder()
                .id(id)
                .path(path)
                .method(method.toUpperCase())
                .status(progress)
                .requiredHeaders(requiredHeaders)
                .authHeaders(authHeaders)
                .requiredParams(requiredParams)
                .responses(responses)
                .build();
    }

    /**
     * Parse a single response to create ResponseMeta.
     * Response 스키마 파싱
     */
    @SuppressWarnings("unchecked")
    private ResponseMeta parseResponse(int statusCode, Map<String, Object> response, Map<String, Object> schemas) {
        Map<String, Object> content = (Map<String, Object>) response.get("content");
        if (content == null) {
            return null;
        }

        // Try to get JSON or XML content
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

        // Resolve $ref if present
        Map<String, Object> resolvedSchema = resolveSchema(schema, schemas, new HashSet<>());

        return ResponseMeta.builder()
                .statusCode(statusCode)
                .body(resolvedSchema)
                .contentType(contentType)
                .build();
    }

    /**
     * Recursively resolve $ref in schema.
     *
     * @param schema the schema that may contain $ref
     * @param schemas the components/schemas map for lookup
     * @param visited set to track visited schemas and prevent circular references
     * @return resolved schema with all $ref replaced by actual schema definitions
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveSchema(Map<String, Object> schema, Map<String, Object> schemas, Set<String> visited) {
        if (schema == null) {
            return Collections.emptyMap();
        }

        // Handle $ref
        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref");

            // Prevent circular references
            if (visited.contains(ref)) {
                log.warn("Circular reference detected: {}", ref);
                return Collections.emptyMap();
            }
            visited.add(ref);

            // Parse ref: "#/components/schemas/User" -> "User"
            String schemaName = extractSchemaName(ref);
            if (schemaName == null) {
                log.warn("Invalid $ref format: {}", ref);
                return Collections.emptyMap();
            }

            // Get referenced schema
            Map<String, Object> referencedSchema = (Map<String, Object>) schemas.get(schemaName);
            if (referencedSchema == null) {
                log.warn("Schema not found: {}", schemaName);
                return Collections.emptyMap();
            }

            // Recursively resolve the referenced schema
            return resolveSchema(referencedSchema, schemas, visited);
        }

        // Create a new map to avoid modifying the original
        Map<String, Object> resolved = new LinkedHashMap<>(schema);

        // Recursively resolve properties
        if (resolved.containsKey("properties")) {
            Map<String, Object> properties = (Map<String, Object>) resolved.get("properties");
            Map<String, Object> resolvedProperties = new LinkedHashMap<>();

            for (Map.Entry<String, Object> prop : properties.entrySet()) {
                Map<String, Object> propSchema = (Map<String, Object>) prop.getValue();
                resolvedProperties.put(prop.getKey(), resolveSchema(propSchema, schemas, new HashSet<>(visited)));
            }

            resolved.put("properties", resolvedProperties);
        }

        // Recursively resolve array items
        if (resolved.containsKey("items")) {
            Map<String, Object> items = (Map<String, Object>) resolved.get("items");
            resolved.put("items", resolveSchema(items, schemas, new HashSet<>(visited)));
        }

        return resolved;
    }

    /**
     * Extract schema name from $ref string.
     *
     * @param ref the reference string (e.g., "#/components/schemas/User")
     * @return the schema name (e.g., "User"), or null if invalid format
     */
    private String extractSchemaName(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }

        String[] parts = ref.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }

        return null;
    }

    /**
     * Check if the string is an HTTP method.
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
