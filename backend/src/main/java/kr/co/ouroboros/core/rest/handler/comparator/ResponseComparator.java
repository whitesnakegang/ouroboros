package kr.co.ouroboros.core.rest.handler.comparator;

import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper.HttpMethod;
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

    /**
     * Compares response definitions between a scanned operation and a file-based operation for a given URL and HTTP method.
     *
     * If the file operation is missing responses that exist in the scanned operation, those scanned responses are added
     * to the file operation's responses. If any response status, content type, or schema differs, the method marks the
     * endpoint as having a mismatch and updates the file operation's Ouroboros progress and diff fields accordingly.
     *
     * @param url the endpoint URL being compared
     * @param method the HTTP method of the endpoint
     * @param scannedOperation the operation obtained from the scan (baseline)
     * @param fileOperation the operation loaded from the file (reference) which may be modified
     * @param schemaMatchResults a map of schema names to booleans indicating precomputed schema match outcomes
     */
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
                log.debug("[RESPONSE MISSING] {} {} - Response-Status {}: File spec에 해당 상태코드 응답이 없습니다. 불일치로 판정합니다.",
                        url, method, statusCode);
                hasMismatch = true;
                break;
            }

            // 둘 다 같은 상태코드에 대한 Response가 있는 경우, 응답 스키마 비교
            boolean isMatch = compareResponseSchemas(scannedResponse, fileResponse, method, url, statusCode, schemaMatchResults);

            if (isMatch) {
                log.debug("[RESPONSE MATCH] {} {} - Status {}: 응답 형식이 일치합니다.", url, method, statusCode);
            } else {
                log.debug("[RESPONSE MISMATCH] {} {} - Status {}: 응답 형식이 일치하지 않습니다.", url, method, statusCode);
                hasMismatch = true;
                break;
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
     * Determine whether two Response objects have matching content definitions for a specific HTTP method, endpoint, and status code.
     *
     * Compares the responses' content types and their associated schemas; a match requires all compared content types to be present and their schemas to be considered equal (including reference checks via schemaMatchResults).
     *
     * @param scannedResponse     the scanned (baseline) response to compare; may be null
     * @param fileResponse        the file-based (reference) response to compare; may be null
     * @param method              the HTTP method for the endpoint being compared
     * @param endpoint            the endpoint URL being compared
     * @param statusCode          the HTTP status code for the response being compared
     * @param schemaMatchResults  map from schema name to boolean indicating whether that named schema matched earlier comparisons; used to resolve $ref-based schema equality
     * @return                    true if the responses' content types and schemas match, false otherwise
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

        // Content-type은 무시하고 schema만 비교
        // scan 스펙의 각 MediaType의 schema가 file 스펙의 모든 MediaType의 schema 중 하나와 일치하는지 확인
        for (Map.Entry<String, MediaType> scannedEntry : scannedContent.entrySet()) {
            MediaType scannedMediaType = scannedEntry.getValue();
            if (scannedMediaType == null || scannedMediaType.getSchema() == null) {
                continue;
            }
            
            Schema scannedSchema = scannedMediaType.getSchema();
            boolean schemaMatched = false;
            
            // file 스펙의 모든 MediaType의 schema와 비교
            for (Map.Entry<String, MediaType> fileEntry : fileContent.entrySet()) {
                MediaType fileMediaType = fileEntry.getValue();
                if (fileMediaType == null || fileMediaType.getSchema() == null) {
                    continue;
                }
                
                Schema fileSchema = fileMediaType.getSchema();
                if (compareSchemas(scannedSchema, fileSchema, method, endpoint, statusCode, null, schemaMatchResults)) {
                    schemaMatched = true;
                    break;
                }
            }
            
            // scan 스펙의 schema가 file 스펙의 어떤 schema와도 일치하지 않으면 false
            if (!schemaMatched) {
                log.debug("[SCHEMA MISMATCH] {} {} - Status {}: 스캔 스펙의 schema가 파일 스펙의 어떤 schema와도 일치하지 않습니다.",
                        endpoint, method, statusCode);
                return false;
            }
        }

        return true;
    }

    /**
     * Determine whether two response schemas are equivalent for a given endpoint response.
     *
     * If either schema uses a `$ref`, the `$ref` values must be identical and, when available, the referenced schema's prior match result from `schemaMatchResults` must be `true`; otherwise the schemas' `type` values must be equal.
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
            log.debug("[SCHEMA NULL MISMATCH] {} {} - Status {}: 한쪽 스키마가 null입니다.",
                    endpoint, method, statusCode);
            return false;
        }

        // $ref 비교 (객체 참조인 경우)
        if (scannedSchema.getRef() != null || fileSchema.getRef() != null) {
            if (!Objects.equals(scannedSchema.getRef(), fileSchema.getRef())) {
                log.debug("[SCHEMA REF MISMATCH] {} {} - Status {}: $ref가 다릅니다. (스캔: {}, 파일: {})",
                        endpoint, method, statusCode, scannedSchema.getRef(), fileSchema.getRef());
                return false;
            }

            // $ref가 같으면 schemaMatchResults에서 확인
            if (scannedSchema.getRef() != null) {
                String schemaName = extractSchemaNameFromRef(scannedSchema.getRef());
                if (schemaName != null && schemaMatchResults.containsKey(schemaName)) {
                    boolean schemaMatch = schemaMatchResults.get(schemaName);
                    if (!schemaMatch) {
                        log.debug("[SCHEMA REF MISMATCH] {} {} - Status {}: 참조하는 스키마 '{}'가 일치하지 않습니다.",
                                endpoint, method, statusCode, schemaName);
                        return false;
                    }
                }
            }
        }
        // type 비교 (기본 타입인 경우)
        else {
            if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
                log.debug("[SCHEMA TYPE MISMATCH] {} {} - Status {}: 타입이 다릅니다. (스캔: {}, 파일: {})",
                        endpoint, method, statusCode, scannedSchema.getType(), fileSchema.getType());
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