package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import kr.co.ouroboros.core.rest.handler.RequestDiffHelper.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * REST API 응답 스펙을 비교하는 컴포넌트.
 * <p>
 * 스캔된 문서를 기준으로 파일 기반 스펙과 응답 형식을 비교하여 일치 여부를 검사하고 상세한 불일치 사항을 로깅합니다.
 *
 * @since 0.0.1
 */
@Slf4j
@Component
public class ResponseComparator {

    public void compareResponsesForMethod(String url, HttpMethod method, Operation scannedOperation, Operation fileOperation, Map<String, Boolean> schemaMatchResults) {
        if (scannedOperation == null || fileOperation == null) {
            log.debug("Scan과 FIle 모두 없는 엔드포인트입니다.");
            return;
        }

        Map<String, Response> scannedResponses = scannedOperation.getResponses();
        Map<String, Response> fileResponses = fileOperation.getResponses();

        // response 필드가 아예 없는 경우
        if (scannedResponses == null || fileResponses == null) {
            log.debug("Responses가 null입니다.");
            return;
        }

        boolean hasMismatch = false;
        // 스캔된 스펙의 특정 엔드포인트의 응답을 순회하여 동일 엔드포인트의 파일 스펙의 응답과 비교
        for (Map.Entry<String, Response> scannedEntry : scannedResponses.entrySet()) {
            String statusCode = scannedEntry.getKey();
            Response scannedResponse = scannedEntry.getValue();
            Response fileResponse = fileResponses.get(statusCode);

            // 스캔 스펙에는 있는데 파일 스펙에는 없는 경우
            if (fileResponse == null) {
                log.info("[RESPONSE MISSING] {} {} - Response-Status {}: File spec에 해당 상태코드 응답이 없습니다. 스캔된 응답을 추가합니다.",
                        url, method, statusCode);
                fileResponses.put(statusCode, scannedResponse);
                continue;
            }

            // 둘 다 같은 상태코드에 대한 Response가 있는 경우, 응답 스키마 비교
            boolean isMatch = compareResponseSchemas(scannedResponse, fileResponse, method, url, statusCode, schemaMatchResults);

            if (isMatch) {
                log.debug("[RESPONSE MATCH] {} {} - Status {}: 응답 형식이 일치합니다.", url, method, statusCode);
            } else {
                log.debug("[RESPONSE MISMATCH] {} {} - Status {}: 응답 형식이 일치하지 않습니다.", url, method, statusCode);
                hasMismatch = true;
            }
        }

        // 파일에만 있는 상태코드(O/X) 확인 → 불일치로 간주
        for (String fileStatus : fileResponses.keySet()) {
            if (!scannedResponses.containsKey(fileStatus)) {
                log.debug("[RESPONSE EXTRA] {} {} - Response-Status {}: 스캔 스펙에 없는 상태코드 응답입니다.", url, method, fileStatus);
                hasMismatch = true;
            }
        }

        // 엔드포인트 단위로 최종 결과 설정
        if (hasMismatch) {
            // 검사 실패했을 경우, progress: mock으로 설정
            // diff는 상황에 따라 request, response, both로 설정
            fileOperation.setXOuroborosProgress("mock");
            if ("none".equals(fileOperation.getXOuroborosDiff())) {
                fileOperation.setXOuroborosDiff("response");
            } else if ("request".equals(fileOperation.getXOuroborosDiff())) {
                fileOperation.setXOuroborosDiff("both");
            }
        } else {
            // 검사 통과했으면 diff: none으로 설정하고 progress: completed로 설정0
            if("none".equals(fileOperation.getXOuroborosDiff())) {
                fileOperation.setXOuroborosProgress("completed");
            }
        }
    }

    /**
     * Compare the response schemas of two Response objects for a specific method, endpoint, and status code.
     *
     * @param schemaMatchResults map of schema name to a boolean indicating whether that named schema matched; used to consider referenced schemas during comparison
     * @return `true` if the responses' content (content types and their schemas) are considered matching, `false` otherwise
     */
    private boolean compareResponseSchemas(Response scannedResponse, Response fileResponse, HttpMethod method, String endpoint, String statusCode, Map<String, Boolean> schemaMatchResults) {
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


    private boolean compareContent(Map<String, MediaType> scannedContent, Map<String, MediaType> fileContent, HttpMethod method, String endpoint, String statusCode,
            Map<String, Boolean> schemaMatchResults) {
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
                    log.debug("[CONTENT TYPE WILDCARD] {} {} - Status {}: Content-Type '*/*'는 모든 타입과 일치합니다.",
                            endpoint, method, statusCode);
                    continue;
                }
                log.debug("[CONTENT TYPE MISSING] {} {} - Status {}: Content-Type '%s'가 파일 스펙에 없습니다.",
                        endpoint, method, statusCode, contentType);
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
     * @return `true` if the MediaType schemas match, `false` otherwise
     */
    private boolean compareMediaTypes(MediaType scannedMediaType, MediaType fileMediaType, HttpMethod method, String endpoint, String statusCode, String contentType,
            Map<String, Boolean> schemaMatchResults) {
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
     * <p>
     * Compares the scanned (baseline) schema and the file-based (reference) schema: if either schema uses a `$ref` the `$ref` values must match and referenced schema match status is consulted via
     * `schemaMatchResults`; otherwise the schemas' `type` values must be equal.
     *
     * @param scannedSchema      the scanned (baseline) Schema to compare
     * @param fileSchema         the file-based (reference) Schema to compare against
     * @param method             the HTTP method associated with the response being compared
     * @param endpoint           the endpoint path associated with the response being compared
     * @param statusCode         the HTTP status code associated with the response being compared
     * @param contentType        the response Content-Type associated with the schema being compared
     * @param schemaMatchResults map from schema name (extracted from `$ref`) to a boolean indicating whether that referenced schema matched previously
     * @return `true` if the schemas are considered matching, `false` otherwise
     */
    private boolean compareSchemas(Schema scannedSchema, Schema fileSchema, HttpMethod method, String endpoint, String statusCode, String contentType, Map<String, Boolean> schemaMatchResults) {
        if (scannedSchema == null && fileSchema == null) {
            return true;
        }
        if (scannedSchema == null || fileSchema == null) {
            log.debug("[SCHEMA NULL MISMATCH] {} {} - Status {}, Content-Type '{}': 한쪽 스키마가 null입니다.",
                    endpoint, method, statusCode, contentType);
            return false;
        }

        // $ref 비교 (객체 참조인 경우)
        if (scannedSchema.getRef() != null || fileSchema.getRef() != null) {
            if (!Objects.equals(scannedSchema.getRef(), fileSchema.getRef())) {
                log.debug("[SCHEMA REF MISMATCH] {} {} - Status {}, Content-Type '{}': $ref가 다릅니다. (스캔: {}, 파일: {})",
                        endpoint, method, statusCode, contentType, scannedSchema.getRef(), fileSchema.getRef());
                return false;
            }

            // $ref가 같으면 schemaMatchResults에서 확인
            if (scannedSchema.getRef() != null) {
                String schemaName = extractSchemaNameFromRef(scannedSchema.getRef());
                if (schemaName != null && schemaMatchResults.containsKey(schemaName)) {
                    boolean schemaMatch = schemaMatchResults.get(schemaName);
                    if (!schemaMatch) {
                        log.debug("[SCHEMA REF MISMATCH] {} {} - Status {}, Content-Type '{}': 참조하는 스키마 '{}'가 일치하지 않습니다.",
                                endpoint, method, statusCode, contentType, schemaName);
                        return false;
                    }
                }
            }
        }
        // type 비교 (기본 타입인 경우)
        else {
            if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
                log.debug("[SCHEMA TYPE MISMATCH] {} {} - Status {}, Content-Type '{}': 타입이 다릅니다. (스캔: {}, 파일: {})",
                        endpoint, method, statusCode, contentType, scannedSchema.getType(), fileSchema.getType());
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
