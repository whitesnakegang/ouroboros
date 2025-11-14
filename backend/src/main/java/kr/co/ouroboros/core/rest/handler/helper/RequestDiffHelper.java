package kr.co.ouroboros.core.rest.handler.helper;

import java.util.*;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Parameter;
import kr.co.ouroboros.core.rest.common.dto.RequestBody;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import kr.co.ouroboros.core.rest.handler.comparator.SchemaComparator.TypeCnts;

public final class RequestDiffHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private RequestDiffHelper() {}

    // diff states
    public static final String DIFF_NONE = "none";
    public static final String DIFF_REQUEST = "request";
    public static final String DIFF_MOCK = "mock";
    public static final String DIFF_COMPLETED = "completed";

    // 메서드 타입
    public enum HttpMethod {GET, POST, PUT, PATCH, DELETE}

    /**
     * Compare request parameter and body type counts between a file operation and a scanned operation,
     * and update the file operation's XOuroboros diff, progress, and tag fields accordingly.
     *
     * If the aggregated type counts differ, the file operation is marked with DIFF_REQUEST and DIFF_MOCK.
     * If they are identical, the file operation is marked with DIFF_NONE and DIFF_COMPLETED.
     *
     * @param url the endpoint path being compared
     * @param fileOp the operation from the file specification; this object will be mutated to reflect diff/progress/tag
     * @param scanOp the operation from the scanned specification to compare against
     * @param method the HTTP method for the operation
     * @param fileFlattenedSchemas flattened schema map for resolving referenced schemas from the file specification
     * @param scanFlattenedSchemas flattened schema map for resolving referenced schemas from the scanned specification
     */
    public static void compareAndMarkRequest(String url, Operation fileOp, Operation scanOp, HttpMethod method,
            Map<String, TypeCnts> fileFlattenedSchemas,
            Map<String, TypeCnts> scanFlattenedSchemas
    ) {


        // 1. fileOp의 타입별 개수 수집
        Map<String, Integer> fileTypeCounts = new HashMap<>();
        collectTypeCounts(fileOp, fileTypeCounts, fileFlattenedSchemas);

        // 2. scanOp의 타입별 개수 수집
        Map<String, Integer> scanTypeCounts = new HashMap<>();
        collectTypeCounts(scanOp, scanTypeCounts, scanFlattenedSchemas);

        // 3. 타입별 개수 비교
        boolean typeDiff = !fileTypeCounts.equals(scanTypeCounts);
        String diffLog = typeDiff ? buildTypeDiffLog(fileTypeCounts, scanTypeCounts) : null;

        if (typeDiff) {
            fileOp.setXOuroborosDiff(DIFF_REQUEST);
            fileOp.setXOuroborosProgress(DIFF_MOCK);
            fileOp.setXOuroborosTag(DIFF_NONE);
            fileOp.setXOuroborosReqLog(diffLog);
        } else {
            fileOp.setXOuroborosDiff(DIFF_NONE);
            fileOp.setXOuroborosProgress(DIFF_COMPLETED);
            fileOp.setXOuroborosTag(DIFF_NONE);
            fileOp.setXOuroborosReqLog(null);
        }
    }

    /**
     * Aggregate request-related type counts from the given Operation into the provided map.
     *
     * This inspects the operation's parameters (excluding path parameters) and its request body,
     * accumulating type occurrences into {@code typeCounts}. When a schema reference is encountered
     * the corresponding entry from {@code flattenedSchemas} is used.
     *
     * @param operation the operation to analyze; if null the method returns without modification
     * @param typeCounts a mutable map that will be updated with aggregated type-to-count entries
     * @param flattenedSchemas a map from schema name to precomputed TypeCnts used to resolve `$ref` references
     */
    private static void collectTypeCounts(Operation operation, Map<String, Integer> typeCounts, 
                                          Map<String, TypeCnts> flattenedSchemas) {
        if (operation == null) {
            return;
        }

        // 1. parameters 에서 타입 수집
        collectTypeCountsFromParameters(operation.getParameters(), typeCounts, flattenedSchemas);

        // 2. requestBody 에서 타입 수집
        collectTypeCountsFromRequestBody(operation.getRequestBody(), typeCounts, flattenedSchemas);
    }

    /**
         * Accumulates type counts for non-path parameters into the provided map.
         *
         * This examines each parameter (skipping null entries and parameters with `in` equal to "path"),
         * ignores parameters without a schema or a name, and increments type counts by delegating to
         * countSchemaType. Resolved/flattened schema type counts from `flattenedSchemas` are used for `$ref` schemas.
         *
         * @param parameters      list of parameters to analyze; may contain nulls
         * @param typeCounts      map to store and increment type count keys (modified by this method)
         * @param flattenedSchemas map of schema name to flattened type counts used to resolve `$ref` schemas
         */
    private static void collectTypeCountsFromParameters(List<Parameter> parameters, Map<String, Integer> typeCounts,
                                                       Map<String, TypeCnts> flattenedSchemas) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        for (Parameter param : parameters) {
            if (param == null) {
                continue;
            }

            // in이 path인 경우 스킵
            String in = param.getIn();
            if ("path".equalsIgnoreCase(in)) {
                continue;
            }

            Schema schema = param.getSchema();
            if (schema == null) {
                continue;
            }

            String paramName = param.getName();
            if (paramName == null || paramName.isEmpty()) {
                continue;
            }

            countSchemaType(schema, paramName, typeCounts, flattenedSchemas);
        }
    }

    /**
         * Aggregate schema type occurrences from a request body's media types into the provided typeCounts map.
         *
         * Counts named properties when a media type schema exposes properties (e.g., multipart form data);
         * otherwise counts the media type's top-level schema using the name "body". Referenced schemas are
         * resolved using the provided flattenedSchemas map and their type counts are merged into typeCounts.
         *
         * @param requestBody     the request body to analyze
         * @param typeCounts      map to store and accumulate type counts; this map is modified by the method
         * @param flattenedSchemas mapping of schema name to precomputed type counts used to resolve `$ref` references
         */
    private static void collectTypeCountsFromRequestBody(RequestBody requestBody, Map<String, Integer> typeCounts,
                                                        Map<String, TypeCnts> flattenedSchemas) {
        if (requestBody == null) {
            return;
        }

        Map<String, MediaType> content = requestBody.getContent();
        if (content == null || content.isEmpty()) {
            return;
        }

        // 각 media type별로 schema 분석
        for (MediaType mediaType : content.values()) {
            if (mediaType == null) {
                continue;
            }

            Schema schema = mediaType.getSchema();
            if (schema == null) {
                continue;
            }

            // schema가 properties를 가지고 있는 경우 (multipart/form-data 등)
            if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                    String propertyName = entry.getKey();
                    Schema propSchema = entry.getValue();
                    if (propertyName != null && !propertyName.isEmpty() && propSchema != null) {
                        countSchemaType(propSchema, propertyName, typeCounts, flattenedSchemas);
                    }
                }
            } else {
                // schema에 직접 타입이나 $ref가 있는 경우 - 이름이 없으므로 "body"를 기본 이름으로 사용
                countSchemaType(schema, "body", typeCounts, flattenedSchemas);
            }
        }
    }

    /**
     * Updates typeCounts with type occurrences described by schema, resolving $ref references using flattenedSchemas when present.
     *
     * If schema contains a $ref and flattenedSchemas has a matching entry, merges that entry's type counts into typeCounts.
     * For inline schemas, increments the count for "name:binary" when format equals "binary", otherwise for "name:type".
     *
     * @param schema the schema to analyze; may be a reference or an inline schema
     * @param name the field or parameter name used to construct type keys; must be non-empty
     * @param typeCounts map that will be updated with merged or incremented type counts
     * @param flattenedSchemas optional map of schema name to precomputed TypeCnts used to resolve $ref entries
     */
    private static void countSchemaType(Schema schema, String name, Map<String, Integer> typeCounts,
                                       Map<String, TypeCnts> flattenedSchemas) {
        if (schema == null || name == null || name.isEmpty()) {
            return;
        }

        // $ref가 있는 경우 - flattenedSchemas에서 조회하여 타입 카운트 추가
        String ref = schema.getRef();
        if (ref != null && !ref.isEmpty()) {
            String refName = extractRefName(ref);
            
            // flattenedSchemas에서 해당 스키마의 타입 카운트 조회
            if (flattenedSchemas != null && flattenedSchemas.containsKey(refName)) {
                TypeCnts refTypeCnts = flattenedSchemas.get(refName);
                if (refTypeCnts != null && refTypeCnts.getTypeCounts() != null) {
                    // 참조된 스키마의 모든 타입 카운트를 현재 typeCounts에 추가
                    for (Map.Entry<String, Integer> entry : refTypeCnts.getTypeCounts().entrySet()) {
                        typeCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }
            }
            return;
        }

        // inline schema인 경우 type으로 카운트
        String type = schema.getType();
        if (type != null && !type.isEmpty()) {
            // format이 binary인 경우 파일로 처리
            String format = schema.getFormat();
            String key;
            if ("binary".equals(format)) {
                key = name + ":binary";
            } else {
                key = name + ":" + type;
            }
            typeCounts.merge(key, 1, Integer::sum);
        }
    }

    private static String buildTypeDiffLog(Map<String, Integer> fileTypeCounts,
            Map<String, Integer> scanTypeCounts) {
        if ((fileTypeCounts == null || fileTypeCounts.isEmpty())
                && (scanTypeCounts == null || scanTypeCounts.isEmpty())) {
            return "";
        }

        Set<String> allKeys = new TreeSet<>();
        if (fileTypeCounts != null) {
            allKeys.addAll(fileTypeCounts.keySet());
        }
        if (scanTypeCounts != null) {
            allKeys.addAll(scanTypeCounts.keySet());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("요청 타입 카운트 불일치");

        for (String key : allKeys) {
            int fileCount = fileTypeCounts != null ? fileTypeCounts.getOrDefault(key, 0) : 0;
            int scanCount = scanTypeCounts != null ? scanTypeCounts.getOrDefault(key, 0) : 0;
            if (fileCount == scanCount) {
                continue;
            }

            String fieldName;
            String fieldType = null;
            int delimiterIdx = key.indexOf(':');
            if (delimiterIdx >= 0) {
                fieldName = key.substring(0, delimiterIdx);
                fieldType = key.substring(delimiterIdx + 1);
            } else {
                fieldName = key;
            }

            String displayName = fieldType == null || fieldType.isEmpty()
                    ? fieldName
                    : fieldName + "(" + fieldType + ")";

            if (fileCount == 0) {
                sb.append("\n - ").append(displayName)
                        .append("은(는) 명세에 없는 필드입니다 (scan=").append(scanCount).append(")");
            } else if (scanCount == 0) {
                sb.append("\n - ").append(displayName)
                        .append("은(는) 스캔 결과에서 확인되지 않았습니다 (spec=").append(fileCount).append(")");
            } else {
                sb.append("\n - ").append(displayName)
                        .append("의 개수가 서로 다릅니다 (spec=").append(fileCount)
                        .append(", scan=").append(scanCount).append(")");
            }
        }

        return sb.toString();
    }

    /**
     * Derives the schema name from a JSON Reference ($ref) string.
     *
     * @param ref the $ref string (e.g., "#/components/schemas/User")
     * @return the substring after the last '/' (e.g., "User"); returns an empty string if ref is null or empty, or the original ref if it contains no '/' or ends with '/'
     */
    private static String extractRefName(String ref) {
        if (ref == null || ref.isEmpty()) {
            return "";
        }
        int lastSlash = ref.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < ref.length() - 1) {
            return ref.substring(lastSlash + 1);
        }
        return ref;
    }
}