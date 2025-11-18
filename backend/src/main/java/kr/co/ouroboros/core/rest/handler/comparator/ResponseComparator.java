package kr.co.ouroboros.core.rest.handler.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Response;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper;
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
     * Compares response definitions of a scanned operation and a file-based operation for a specific URL and HTTP method.
     *
     * Accumulates per-status-code mismatch reasons and updates the provided fileOperation's XOuroborosProgress,
     * XOuroborosDiff, and XOuroborosResLog to reflect detected differences; when no differences are found the
     * method clears the response log and may mark progress as completed.
     *
     * @param url the endpoint URL being compared
     * @param method the HTTP method of the endpoint
     * @param scannedOperation the operation obtained from the scan (baseline)
     * @param fileOperation the operation loaded from the file (reference) which may be modified to record diffs/progress
     * @param schemaMatchResults map of referenced schema names to booleans indicating previously computed schema match outcomes (`true` = matched, `false` = mismatched)
     */
    public void compareResponsesForMethod(String url, HttpMethod method, Operation scannedOperation, Operation fileOperation, Map<String, Boolean> schemaMatchResults) {
        if (scannedOperation == null || fileOperation == null) {
            log.debug("Scan과 FIle 모두 없는 엔드포인트입니다.");
            return;
        }

        Map<String, Response> scannedResponses = scannedOperation.getResponses();
        Map<String, Response> fileResponses = fileOperation.getResponses();

        List<String> mismatchReasons = new ArrayList<>();

        boolean scannedEmpty = scannedResponses == null || scannedResponses.isEmpty();
        boolean fileEmpty = fileResponses == null || fileResponses.isEmpty();

        if (!scannedEmpty && fileEmpty) {
            mismatchReasons.add("스캔 명세에는 응답이 있지만 파일 명세에는 응답이 없습니다.");
        } else if (scannedEmpty && !fileEmpty) {
            mismatchReasons.add("파일 명세에는 응답이 정의되어 있지만 스캔 명세에서는 확인되지 않았습니다.");
        }

        if (!scannedEmpty && !fileEmpty) {
            // 스캔된 스펙의 특정 엔드포인트의 응답을 순회하여 동일 엔드포인트의 파일 스펙의 응답과 비교
            for (Map.Entry<String, Response> scannedEntry : scannedResponses.entrySet()) {
                String statusCode = scannedEntry.getKey();
                Response scannedResponse = scannedEntry.getValue();
                Response fileResponse = fileResponses.get(statusCode);

                // 스캔 스펙에는 있는데 파일 스펙에는 없는 경우
                if (fileResponse == null) {
                    String reason = String.format("Status %s: 파일 명세에는 해당 응답이 없습니다.", statusCode);
                    log.debug("[RESPONSE MISSING] {} {} - Response-Status {}: File spec에 해당 상태코드 응답이 없습니다. 불일치로 판정합니다.",
                            url, method, statusCode);
                    mismatchReasons.add(reason);
                    continue;
                }

                // 둘 다 같은 상태코드에 대한 Response가 있는 경우, 응답 스키마 비교
                String mismatch = compareResponseSchemas(scannedResponse, fileResponse, method, url, statusCode, schemaMatchResults);

                if (mismatch == null) {
                    log.debug("[RESPONSE MATCH] {} {} - Status {}: 응답 형식이 일치합니다.", url, method, statusCode);
                } else {
                    log.debug("[RESPONSE MISMATCH] {} {} - Status {}: {}", url, method, statusCode, mismatch);
                    mismatchReasons.add(String.format("Status %s: %s", statusCode, mismatch));
                }
            }

            // 파일에만 있는 상태코드(O/X) 확인 → 불일치로 간주
            for (String fileStatus : fileResponses.keySet()) {
                if (!scannedResponses.containsKey(fileStatus)) {
                    String reason = String.format("Status %s: 스캔 명세에는 존재하지 않는 응답입니다.", fileStatus);
                    log.debug("[RESPONSE EXTRA] {} {} - Response-Status {}: 스캔 스펙에 없는 상태코드 응답입니다.", url, method, fileStatus);
                    mismatchReasons.add(reason);
                }
            }
        }

        // 엔드포인트 단위로 최종 결과 설정
        if (!mismatchReasons.isEmpty()) {
            fileOperation.setXOuroborosProgress("mock");
            if ("none".equals(fileOperation.getXOuroborosDiff())) {
                fileOperation.setXOuroborosDiff("response");
            } else if ("request".equals(fileOperation.getXOuroborosDiff())) {
                fileOperation.setXOuroborosDiff("both");
            }
            fileOperation.setXOuroborosResLog(String.join("\n", mismatchReasons));
        } else {
            if ("none".equals(fileOperation.getXOuroborosDiff())) {
                fileOperation.setXOuroborosProgress("completed");
            }
            fileOperation.setXOuroborosResLog(null);
        }
    }

    /**
     * Compare the content schemas of two Response objects for a given HTTP method, endpoint, and status code and report any mismatch reason.
     *
     * If both responses are absent, they are considered matching. If one response is missing, a localized message indicating the missing definition is returned. When both are present, their content types and associated schemas are compared; referenced schemas are resolved against the provided schemaMatchResults map to determine equality.
     *
     * @param scannedResponse     the scanned (baseline) response to compare; may be null
     * @param fileResponse        the file-based (reference) response to compare; may be null
     * @param method              the HTTP method for the endpoint being compared
     * @param endpoint            the endpoint URL being compared
     * @param statusCode          the HTTP status code for the response being compared
     * @param schemaMatchResults  map from schema name to boolean indicating whether that named schema matched earlier comparisons; used to evaluate equality of `$ref`-referenced schemas (may be null)
     * @return                    `null` when the responses' contents and schemas match; otherwise a descriptive mismatch reason string
     */
    private String compareResponseSchemas(Response scannedResponse, Response fileResponse, HttpMethod method, String endpoint, String statusCode, Map<String, Boolean> schemaMatchResults) {
        if (scannedResponse == null && fileResponse == null) {
            return null;
        }
        if (scannedResponse == null) {
            return "스캔 명세에서 응답 정의가 누락되었습니다.";
        }
        if (fileResponse == null) {
            return "파일 명세에서 응답 정의가 누락되었습니다.";
        }

        // Content 비교 (상태코드, content-type, schema만)
        return compareContent(scannedResponse.getContent(), fileResponse.getContent(), method, endpoint, statusCode, schemaMatchResults);
    }

    /**
     * Compare response content schemas between a scanned operation and a file-based operation for a given endpoint response.
     *
     * This comparison ignores content-type keys and treats each media type's schema in one side as matching if it equals any schema on the other side.
     *
     * @param scannedContent      media-type map of the scanned operation's response content
     * @param fileContent         media-type map of the file operation's response content
     * @param method              the HTTP method of the operation being compared
     * @param endpoint            the endpoint (URL) of the operation being compared
     * @param statusCode          the response status code being compared
     * @param schemaMatchResults  optional map of referenced schema names to booleans indicating whether that referenced schema was previously determined to match; used when comparing `$ref` schemas
     * @return                    `null` if all content schemas are mutually matched; otherwise a descriptive mismatch message explaining the first detected difference
     */
    private String compareContent(Map<String, MediaType> scannedContent, Map<String, MediaType> fileContent, HttpMethod method, String endpoint, String statusCode,
            Map<String, Boolean> schemaMatchResults) {
        if (scannedContent == null && fileContent == null) {
            return null;
        }
        if ((scannedContent == null || scannedContent.isEmpty()) && (fileContent == null || fileContent.isEmpty())) {
            return null;
        }
        if (scannedContent == null || scannedContent.isEmpty()) {
            return "스캔 명세에서 응답 본문이 확인되지 않았습니다.";
        }
        if (fileContent == null || fileContent.isEmpty()) {
            return "파일 명세에서 응답 본문이 확인되지 않았습니다.";
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
            String lastMismatch = null;

            // file 스펙의 모든 MediaType의 schema와 비교
            for (Map.Entry<String, MediaType> fileEntry : fileContent.entrySet()) {
                MediaType fileMediaType = fileEntry.getValue();
                if (fileMediaType == null || fileMediaType.getSchema() == null) {
                    continue;
                }

                Schema fileSchema = fileMediaType.getSchema();
                String mismatch = compareSchemas(scannedSchema, fileSchema, method, endpoint, statusCode, scannedEntry.getKey(), schemaMatchResults);
                if (mismatch == null) {
                    schemaMatched = true;
                    break;
                } else {
                    lastMismatch = mismatch;
                }
            }

            // scan 스펙의 schema가 file 스펙의 어떤 schema와도 일치하지 않으면 이유 반환
            if (!schemaMatched) {
                if (lastMismatch != null) {
                    return lastMismatch;
                }
                return String.format("Content '%s': 스캔 명세의 스키마가 파일 명세와 일치하지 않습니다.", scannedEntry.getKey());
            }
        }

        // file 스펙에만 존재하는 schema 확인
        for (Map.Entry<String, MediaType> fileEntry : fileContent.entrySet()) {
            MediaType fileMediaType = fileEntry.getValue();
            if (fileMediaType == null || fileMediaType.getSchema() == null) {
                continue;
            }

            Schema fileSchema = fileMediaType.getSchema();
            boolean schemaMatched = false;
            String lastMismatch = null;

            for (Map.Entry<String, MediaType> scannedEntry : scannedContent.entrySet()) {
                MediaType scannedMediaType = scannedEntry.getValue();
                if (scannedMediaType == null || scannedMediaType.getSchema() == null) {
                    continue;
                }

                Schema scannedSchema = scannedMediaType.getSchema();
                String mismatch = compareSchemas(scannedSchema, fileSchema, method, endpoint, statusCode, scannedEntry.getKey(), schemaMatchResults);
                if (mismatch == null) {
                    schemaMatched = true;
                    break;
                } else {
                    lastMismatch = mismatch;
                }
            }

            if (!schemaMatched) {
                if (lastMismatch != null) {
                    return lastMismatch;
                }
                return String.format("Content '%s': 파일 명세에만 존재하는 스키마입니다.", fileEntry.getKey());
            }
        }

        return null;
    }

    /**
     * Compare two response schemas and produce a human-readable mismatch reason if they differ.
     *
     * Compares by `$ref` when either schema uses a `$ref`: the `$ref` values must be identical, and when `schemaMatchResults`
     * is provided the referenced schema's entry must be `true`. If neither schema uses a `$ref`, their `type` fields are compared.
     *
     * @param scannedSchema      the scanned (baseline) Schema to compare
     * @param fileSchema         the file-based (reference) Schema to compare against
     * @param method             the HTTP method associated with the response being compared
     * @param endpoint           the endpoint path associated with the response being compared
     * @param statusCode         the HTTP status code associated with the response being compared
     * @param contentType        the response Content-Type associated with the schema being compared (used to format the message)
     * @param schemaMatchResults map from schema name (extracted from a `$ref` like "#/components/schemas/Name") to a boolean indicating whether that referenced schema matched previously; may be null
     * @return null if the schemas are considered matching; otherwise a descriptive mismatch message suitable for logging or inclusion in a diff log.
     */
    private String compareSchemas(Schema scannedSchema, Schema fileSchema, HttpMethod method, String endpoint, String statusCode, String contentType, Map<String, Boolean> schemaMatchResults) {
        if (scannedSchema == null && fileSchema == null) {
            return null;
        }
        if (scannedSchema == null || fileSchema == null) {
            log.debug("[SCHEMA NULL MISMATCH] {} {} - Status {}: 한쪽 스키마가 null입니다.",
                    endpoint, method, statusCode);
            return formatContentReason(contentType, "스키마 정의가 한쪽에만 존재합니다.");
        }

        // $ref 비교 (객체 참조인 경우)
        if (scannedSchema.getRef() != null || fileSchema.getRef() != null) {
            // Normalize both refs to simple class names for comparison
            String normalizedScannedRef = normalizeSchemaRef(scannedSchema.getRef());
            String normalizedFileRef = normalizeSchemaRef(fileSchema.getRef());

            if (!Objects.equals(normalizedScannedRef, normalizedFileRef)) {
                log.debug("[SCHEMA REF MISMATCH] {} {} - Status {}: $ref가 다릅니다. (스캔: {}, 파일: {})",
                        endpoint, method, statusCode, scannedSchema.getRef(), fileSchema.getRef());
                return formatContentReason(contentType,
                        String.format("$ref가 다릅니다 (scan=%s, spec=%s)", scannedSchema.getRef(), fileSchema.getRef()));
            }

            // $ref가 같으면 schemaMatchResults에서 확인
            if (scannedSchema.getRef() != null && schemaMatchResults != null) {
                String schemaName = extractSchemaNameFromRef(scannedSchema.getRef());
                if (schemaName != null && schemaMatchResults.containsKey(schemaName)) {
                    boolean schemaMatch = schemaMatchResults.get(schemaName);
                    if (!schemaMatch) {
                        log.debug("[SCHEMA REF MISMATCH] {} {} - Status {}: 참조하는 스키마 '{}'가 일치하지 않습니다.",
                                endpoint, method, statusCode, schemaName);
                        return formatContentReason(contentType,
                                String.format("참조 스키마 '%s'가 일치하지 않습니다.", schemaName));
                    }
                }
            }
        }
        // type 비교 (기본 타입인 경우)
        else {
            if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
                log.debug("[SCHEMA TYPE MISMATCH] {} {} - Status {}: 타입이 다릅니다. (구현: {}, 명세: {})",
                        endpoint, method, statusCode, scannedSchema.getType(), fileSchema.getType());
                return formatContentReason(contentType,
                        String.format("타입이 다릅니다 (구현=%s, 명세=%s)", scannedSchema.getType(), fileSchema.getType()));
            }
        }

        return null;
    }

    /**
     * Prefixes a message with "Content 'contentType': " when a content type is provided.
     *
     * @param contentType the media type string to include as a prefix (e.g., "application/json"); if null or empty the message is returned unchanged
     * @param message the base message to format
     * @return the original message prefixed with "Content '...': " when contentType is non-empty, otherwise the original message
     */
    private String formatContentReason(String contentType, String message) {
        if (contentType == null || contentType.isEmpty()) {
            return message;
        }
        return String.format("Content '%s': %s", contentType, message);
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

    /**
     * Normalizes a schema reference to its simple class name for comparison.
     * <p>
     * Extracts the schema name from a $ref and converts FQCN to simple class name.
     * Example: "#/components/schemas/com.c102.ourotest.dto.User" -> "User"
     *
     * @param ref a $ref string expected in the form "#/components/schemas/SchemaName"
     * @return the simple class name, or {@code null} if {@code ref} is {@code null} or not in the expected form
     */
    private String normalizeSchemaRef(String ref) {
        String schemaName = extractSchemaNameFromRef(ref);
        if (schemaName == null) {
            return null;
        }

        // Extract simple class name from FQCN using common helper
        return RequestDiffHelper.extractClassNameFromFullName(schemaName);
    }

}