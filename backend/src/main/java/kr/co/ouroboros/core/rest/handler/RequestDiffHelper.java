package kr.co.ouroboros.core.rest.handler;

import java.util.*;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Parameter;
import kr.co.ouroboros.core.rest.common.dto.RequestBody;
import kr.co.ouroboros.core.rest.common.dto.Schema;

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
     * Compare a file operation against a scanned operation and mark the file operation's diff and progress.
     *
     * Compares the count of each data type in request parameters and request body.
     * If counts differ, sets diff to "request" and progress to "MOCK".
     * Otherwise sets diff to "none" and progress to "COMPLETED".
     *
     * @param url the endpoint path being compared
     * @param fileOp the operation from the file specification; this object will be updated with diff and progress state
     * @param scanOp the operation from the scanned specification to compare against
     * @param method the HTTP method for the operation
     */
    public static void compareAndMarkRequest(String url, Operation fileOp, Operation scanOp, HttpMethod method,
            Map<String, SchemaComparator.TypeCnts> fileFlattenedSchemas,
            Map<String, SchemaComparator.TypeCnts> scanFlattenedSchemas
    ) {


        // 1. fileOp의 타입별 개수 수집
        Map<String, Integer> fileTypeCounts = new HashMap<>();
        collectTypeCounts(fileOp, fileTypeCounts, fileFlattenedSchemas);

        // 2. scanOp의 타입별 개수 수집
        Map<String, Integer> scanTypeCounts = new HashMap<>();
        collectTypeCounts(scanOp, scanTypeCounts, scanFlattenedSchemas);

        // 3. 타입별 개수 비교
        boolean typeDiff = !fileTypeCounts.equals(scanTypeCounts);

        if (typeDiff) {
            fileOp.setXOuroborosDiff(DIFF_REQUEST);
            fileOp.setXOuroborosProgress(DIFF_MOCK);
            fileOp.setXOuroborosTag(DIFF_NONE);
        } else {
            fileOp.setXOuroborosDiff(DIFF_NONE);
            fileOp.setXOuroborosProgress(DIFF_COMPLETED);
            fileOp.setXOuroborosTag(DIFF_NONE);
        }
    }

    /**
     * Collect type counts from an operation's parameters and request body.
     *
     * @param operation the operation to analyze
     * @param typeCounts map to store type counts (will be modified)
     * @param flattenedSchemas map of schema name to flattened type counts
     */
    private static void collectTypeCounts(Operation operation, Map<String, Integer> typeCounts, 
                                          Map<String, SchemaComparator.TypeCnts> flattenedSchemas) {
        if (operation == null) {
            return;
        }

        // 1. parameters 에서 타입 수집
        collectTypeCountsFromParameters(operation.getParameters(), typeCounts, flattenedSchemas);

        // 2. requestBody 에서 타입 수집
        collectTypeCountsFromRequestBody(operation.getRequestBody(), typeCounts, flattenedSchemas);
    }

    /**
     * Collect type counts from parameters list.
     *
     * @param parameters list of parameters to analyze
     * @param typeCounts map to store type counts (will be modified)
     * @param flattenedSchemas map of schema name to flattened type counts
     */
    private static void collectTypeCountsFromParameters(List<Parameter> parameters, Map<String, Integer> typeCounts,
                                                       Map<String, SchemaComparator.TypeCnts> flattenedSchemas) {
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
     * Collect type counts from request body.
     *
     * @param requestBody the request body to analyze
     * @param typeCounts map to store type counts (will be modified)
     * @param flattenedSchemas map of schema name to flattened type counts
     */
    private static void collectTypeCountsFromRequestBody(RequestBody requestBody, Map<String, Integer> typeCounts,
                                                        Map<String, SchemaComparator.TypeCnts> flattenedSchemas) {
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
     * Count the type of a single schema and add to the type counts map.
     *
     * Handles both inline schemas (with type field) and referenced schemas ($ref).
     * Creates a key in the format "name:type" (e.g., "page:integer", "user:ref:User").
     *
     * @param schema the schema to analyze
     * @param name the name of the field/parameter
     * @param typeCounts map to store type counts (will be modified)
     * @param flattenedSchemas map of schema name to flattened type counts
     */
    private static void countSchemaType(Schema schema, String name, Map<String, Integer> typeCounts,
                                       Map<String, SchemaComparator.TypeCnts> flattenedSchemas) {
        if (schema == null || name == null || name.isEmpty()) {
            return;
        }

        // $ref가 있는 경우 - flattenedSchemas에서 조회하여 타입 카운트 추가
        String ref = schema.getRef();
        if (ref != null && !ref.isEmpty()) {
            String refName = extractRefName(ref);
            
            // flattenedSchemas에서 해당 스키마의 타입 카운트 조회
            if (flattenedSchemas != null && flattenedSchemas.containsKey(refName)) {
                SchemaComparator.TypeCnts refTypeCnts = flattenedSchemas.get(refName);
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

    /**
     * Extracts the schema name from a JSON Reference ($ref) string.
     *
     * @param ref the $ref string (e.g., "#/components/schemas/User")
     * @return the substring after the last '/' (e.g., "User"), or the original ref if no '/' is present
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