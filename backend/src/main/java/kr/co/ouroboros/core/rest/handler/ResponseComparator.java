package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * REST API 응답 스펙을 비교하는 컴포넌트.
 * 
 * 스캔된 문서를 기준으로 파일 기반 스펙과 응답 형식을 비교하여
 * 일치 여부를 검사하고 상세한 불일치 사항을 로깅합니다.
 * 
 * @since 0.0.1
 */
@Slf4j
@Component
public class ResponseComparator {

    /**
     * Compares responses for a given HTTP method between a scanned operation and a file-based operation, logging matches and recording differences.
     *
     * @param method the HTTP method name (e.g., "GET", "POST")
     * @param scannedOperation the operation from the scanned (baseline) document
     * @param fileOperation the operation from the file (reference) document; may be annotated when differences are found
     * @param endpoint the endpoint path being compared
     * @param schemaMatchResults map from schema name to a boolean indicating whether that schema was determined to match
     */
    public void compareResponsesForMethod(String method, Operation scannedOperation, Operation fileOperation, String endpoint, Map<String, Boolean> schemaMatchResults) {
        if (scannedOperation == null || fileOperation == null) {
            return;
        }

        Map<String, Response> scannedResponses = scannedOperation.getResponses();
        Map<String, Response> fileResponses = fileOperation.getResponses();

        if (scannedResponses == null || fileResponses == null) {
            log.debug("Responses가 null입니다.");
            return;
        }

        // 스캔된 문서의 각 응답에 대해 파일 문서와 비교
        for (Map.Entry<String, Response> scannedEntry : scannedResponses.entrySet()) {
            String statusCode = scannedEntry.getKey();
            Response scannedResponse = scannedEntry.getValue();
            Response fileResponse = fileResponses.get(statusCode);

            if (fileResponse == null) {
                System.out.println(String.format("[RESPONSE MISSING] %s %s - Status %s: File spec에 해당 상태코드 응답이 없습니다. 스캔된 응답을 추가합니다.", 
                    method, endpoint, statusCode));
                // 파일 스펙에 스캔된 응답 추가
                fileResponses.put(statusCode, scannedResponse);
                continue;
            }

            // 응답 스키마 비교
            boolean isMatch = compareResponseSchemas(scannedResponse, fileResponse, method, endpoint, statusCode, schemaMatchResults);
            
            if (isMatch) {
                System.out.println(String.format("[RESPONSE MATCH] %s %s - Status %s: 응답 형식이 일치합니다.", 
                    method, endpoint, statusCode));
            } else {
                System.out.println(String.format("[RESPONSE MISMATCH] %s %s - Status %s: 응답 형식이 일치하지 않습니다.", 
                    method, endpoint, statusCode));
                // 불일치 => 파일 Operation에 표기
                fileOperation.setXOuroborosDiff("response");
            }
        }
    }

    /**
     * Compare the response schemas of two Response objects for a specific method, endpoint, and status code.
     *
     * @param schemaMatchResults map of schema name to a boolean indicating whether that named schema matched; used to consider referenced schemas during comparison
     * @return `true` if the responses' content (content types and their schemas) are considered matching, `false` otherwise
     */
    private boolean compareResponseSchemas(Response scannedResponse, Response fileResponse, String method, String endpoint, String statusCode, Map<String, Boolean> schemaMatchResults) {
        if (scannedResponse == null && fileResponse == null) {
            return true;
        }
        if (scannedResponse == null || fileResponse == null) {
            return false;
        }

        // Content 비교 (상태코드, content-type, schema만)
        if (!compareContent(scannedResponse.getContent(), fileResponse.getContent(), method, endpoint, statusCode, schemaMatchResults)) {
            return false;
        }

        return true;
    }

    /**
     * Compare two content maps (content type -> MediaType) for a response and determine if they match.
     *
     * Compares each content type in the scanned map against the file-based map, treating the scanned entry
     * with content type "*/*" as a wildcard that matches any file content type. If any required content
     * type or media type schema differs, the method returns false.
     *
     * @param scannedContent    map of content types to MediaType from the scanned (baseline) operation
     * @param fileContent       map of content types to MediaType from the file-based (reference) operation
     * @param method            the HTTP method being compared (for logging/context)
     * @param endpoint          the endpoint being compared (for logging/context)
     * @param statusCode        the response status code being compared (for logging/context)
     * @param schemaMatchResults map that records per-schema match results keyed by schema name; used to consult
     *                           previously computed schema comparisons when resolving $ref references
     * @return                  `true` if the content maps are considered equivalent, `false` otherwise
     */
    private boolean compareContent(Map<String, MediaType> scannedContent, Map<String, MediaType> fileContent, String method, String endpoint, String statusCode, Map<String, Boolean> schemaMatchResults) {
        if (scannedContent == null && fileContent == null) {
            return true;
        }
        if (scannedContent == null || fileContent == null) {
            return false;
        }

        // 스캔된 문서의 각 Content Type에 대해 비교
        for (Map.Entry<String, MediaType> scannedEntry : scannedContent.entrySet()) {
            String contentType = scannedEntry.getKey();
            MediaType scannedMediaType = scannedEntry.getValue();
            MediaType fileMediaType = fileContent.get(contentType);

            if (fileMediaType == null) {
                // */*는 모든 content-type과 일치한다고 간주
                if ("*/*".equals(contentType)) {
                    System.out.println(String.format("[CONTENT TYPE WILDCARD] %s %s - Status %s: Content-Type '*/*'는 모든 타입과 일치합니다.", 
                        method, endpoint, statusCode));
                    continue;
                }
                System.out.println(String.format("[CONTENT TYPE MISSING] %s %s - Status %s: Content-Type '%s'가 파일 스펙에 없습니다.", 
                    method, endpoint, statusCode, contentType));
                return false;
            }

            if (!compareMediaTypes(scannedMediaType, fileMediaType, method, endpoint, statusCode, contentType, schemaMatchResults)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare two MediaType objects' schemas for a given response context.
     *
     * @param scannedMediaType   the scanned (baseline) MediaType
     * @param fileMediaType      the file-based MediaType to compare against
     * @param method             the HTTP method of the response
     * @param endpoint           the endpoint path of the response
     * @param statusCode         the HTTP status code of the response
     * @param contentType        the Content-Type being compared
     * @param schemaMatchResults a map of schema name to match result used to short-circuit referenced-schema comparisons
     * @return                   `true` if the MediaType schemas match, `false` otherwise
     */
    private boolean compareMediaTypes(MediaType scannedMediaType, MediaType fileMediaType, String method, String endpoint, String statusCode, String contentType, Map<String, Boolean> schemaMatchResults) {
        if (scannedMediaType == null && fileMediaType == null) {
            return true;
        }
        if (scannedMediaType == null || fileMediaType == null) {
            return false;
        }

        return compareSchemas(scannedMediaType.getSchema(), fileMediaType.getSchema(), method, endpoint, statusCode, contentType, schemaMatchResults);
    }

    /**
     * Compare two response schemas by their `$ref` reference or primitive `type`.
     *
     * Compares the scanned (baseline) schema and the file-based (reference) schema: if either schema uses a `$ref`
     * the `$ref` values must match and referenced schema match status is consulted via `schemaMatchResults`; otherwise
     * the schemas' `type` values must be equal.
     *
     * @param scannedSchema the scanned (baseline) Schema to compare
     * @param fileSchema the file-based (reference) Schema to compare against
     * @param method the HTTP method associated with the response being compared
     * @param endpoint the endpoint path associated with the response being compared
     * @param statusCode the HTTP status code associated with the response being compared
     * @param contentType the response Content-Type associated with the schema being compared
     * @param schemaMatchResults map from schema name (extracted from `$ref`) to a boolean indicating whether that referenced schema matched previously
     * @return `true` if the schemas are considered matching, `false` otherwise
     */
    private boolean compareSchemas(Schema scannedSchema, Schema fileSchema, String method, String endpoint, String statusCode, String contentType, Map<String, Boolean> schemaMatchResults) {
        if (scannedSchema == null && fileSchema == null) {
            return true;
        }
        if (scannedSchema == null || fileSchema == null) {
            System.out.println(String.format("[SCHEMA NULL MISMATCH] %s %s - Status %s, Content-Type '%s': 한쪽 스키마가 null입니다.", 
                method, endpoint, statusCode, contentType));
            return false;
        }

        // $ref 비교 (객체 참조인 경우)
        if (scannedSchema.getRef() != null || fileSchema.getRef() != null) {
            if (!Objects.equals(scannedSchema.getRef(), fileSchema.getRef())) {
                System.out.println(String.format("[SCHEMA REF MISMATCH] %s %s - Status %s, Content-Type '%s': $ref가 다릅니다. (스캔: %s, 파일: %s)", 
                    method, endpoint, statusCode, contentType, scannedSchema.getRef(), fileSchema.getRef()));
                return false;
            }
            
            // $ref가 같으면 schemaMatchResults에서 확인
            if (scannedSchema.getRef() != null) {
                String schemaName = extractSchemaNameFromRef(scannedSchema.getRef());
                if (schemaName != null && schemaMatchResults.containsKey(schemaName)) {
                    boolean schemaMatch = schemaMatchResults.get(schemaName);
                    if (!schemaMatch) {
                        System.out.println(String.format("[SCHEMA REF MISMATCH] %s %s - Status %s, Content-Type '%s': 참조하는 스키마 '%s'가 일치하지 않습니다.", 
                            method, endpoint, statusCode, contentType, schemaName));
                        return false;
                    }
                }
            }
        }
        // type 비교 (기본 타입인 경우)
        else {
            if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
                System.out.println(String.format("[SCHEMA TYPE MISMATCH] %s %s - Status %s, Content-Type '%s': 타입이 다릅니다. (스캔: %s, 파일: %s)", 
                    method, endpoint, statusCode, contentType, scannedSchema.getType(), fileSchema.getType()));
                return false;
            }
        }

        return true;
    }
    
    /**
     * Extracts the schema name from a JSON Reference ($ref) string.
     *
     * @param ref a $ref string expected in the form "#/components/schemas/SchemaName"
     * @return the schema name (for example, "User"), or {@code null} if {@code ref} is {@code null} or not in the expected form
     */
    private String extractSchemaNameFromRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        return ref.substring("#/components/schemas/".length());
    }

}