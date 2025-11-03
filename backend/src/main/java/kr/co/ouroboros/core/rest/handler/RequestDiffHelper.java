package kr.co.ouroboros.core.rest.handler;

import java.util.*;
import kr.co.ouroboros.core.rest.common.dto.MediaType;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.Parameter;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.common.dto.RequestBody;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RequestDiffHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private RequestDiffHelper() {}

    // diff states
    public static final String DIFF_NONE = "none";
    public static final String DIFF_REQUEST = "request";

    // 메서드 타입
    public enum HttpMethod {GET, POST, PUT, PATCH, DELETE}

    /**
     * Return an empty map when the given map is null, otherwise return the given map.
     *
     * @param m the map that may be null
     * @return the same map if non-null, otherwise an immutable empty map
     */
    public static <K, V> Map<K, V> safe(Map<K, V> m) {
        return (m == null) ? Collections.emptyMap() : m;
    }

    /**
     * Compare a file operation against a scanned operation for the same endpoint and mark the file operation's diff and progress.
     *
     * Compares path and query parameters and request body schemas; if any difference is detected, sets the file operation's XOuroborosDiff to "request" and progress to "MOCK", otherwise sets diff to "none" and progress to "COMPLETED".
     *
     * @param url the endpoint path being compared
     * @param fileOp the operation from the file specification; this object will be updated with diff and progress state
     * @param scanOp the operation from the scanned specification to compare against
     * @param method the HTTP method for the operation
     * @param schemaMatchResults a map of schema reference name to boolean indicating whether referenced schemas are considered equivalent; used when comparing referenced schemas
     */
    public static void compareAndMarkRequest(String url, Operation fileOp, Operation scanOp, HttpMethod method, Map<String, Boolean> schemaMatchResults) {
        log.info("=====REQUEST VALID FUNC==== [{}] [{}]", url, method);
        
        // 1. Parameter 검증
        boolean paramsDiff = compareParameters(fileOp.getParameters(), scanOp.getParameters());
        if (paramsDiff) {
            log.info("[{}], [{}]: 파라미터 다름", url, method);
            fileOp.setXOuroborosDiff(DIFF_REQUEST);
            fileOp.setXOuroborosProgress("MOCK");
            return;
        }
        log.info("[{}], [{}]: 파라미터 똑같음", url, method);
        
        // 2. Request Body 검증
        boolean bodyDiff = compareRequestBody(fileOp.getRequestBody(), scanOp.getRequestBody(), schemaMatchResults);
        if (bodyDiff) {
            log.info("[{}], [{}]: Request Body 다름", url, method);
            fileOp.setXOuroborosDiff(DIFF_REQUEST);
            fileOp.setXOuroborosProgress("MOCK");
            return;
        }
        log.info("[{}], [{}]: Request Body 똑같음", url, method);
        
        // 3. 모든 검증 통과
        fileOp.setXOuroborosDiff(DIFF_NONE);
        fileOp.setXOuroborosProgress("COMPLETED");
    }

    /**
     * Determine whether path or query parameters differ between the file and scanned parameter lists.
     *
     * @param fileParams parameters from the file specification
     * @param scanParams parameters from the scanned specification
     * @return `true` if either path or query parameters differ, `false` otherwise
     */
    private static boolean compareParameters(List<Parameter> fileParams, List<Parameter> scanParams) {
        List<Parameter> fileList = safeList(fileParams);
        List<Parameter> scanList = safeList(scanParams);

        // Path parameter와 Query parameter를 분리하여 비교
        // 1. Path parameter 비교
        boolean pathDiff = comparePathParams(fileList, scanList);
        if (pathDiff) {
            return true;
        }

        // 2. Query parameter 비교
        boolean queryDiff = compareQueryParams(fileList, scanList);
        if (queryDiff) {
            return true;
        }

        return false;
    }

    /**
     * Determine whether path parameters differ between the file and scanned operations.
     *
     * If the scanned operation has no path parameters, this method treats them as identical.
     *
     * @param fileParams list of parameters from the file specification
     * @param scanParams list of parameters from the scanned specification
     * @return `true` if a difference is found (count, names, presence, or type), `false` otherwise
     */
    private static boolean comparePathParams(List<Parameter> fileParams, List<Parameter> scanParams) {
        List<Parameter> filePathParams = filterByIn(fileParams, "path");
        List<Parameter> scanPathParams = filterByIn(scanParams, "path");

        // scan에 path가 없으면 비교하지 않음
        if (scanPathParams.isEmpty()) {
            return false;
        }

        // Path parameter 개수 비교
        if (filePathParams.size() != scanPathParams.size()) {
            return true;
        }

        // 이름으로 인덱싱
        Map<String, Parameter> fileIndexed = indexByName(filePathParams);
        Map<String, Parameter> scanIndexed = indexByName(scanPathParams);

        // 이름 집합 비교
        if (!fileIndexed.keySet().equals(scanIndexed.keySet())) {
            return true;
        }

        // 같은 이름의 파라미터에 대해 타입 비교
        for (String name : scanIndexed.keySet()) {
            Parameter fileParam = fileIndexed.get(name);
            Parameter scanParam = scanIndexed.get(name);
            
            if (fileParam == null || scanParam == null) {
                return true;
            }

            // 타입 비교
            String fileType = extractType(fileParam.getSchema());
            String scanType = extractType(scanParam.getSchema());
            
            if (!Objects.equals(fileType, scanType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether query parameters differ between the file specification and the scanned specification.
     *
     * If the scanned specification contains no query parameters, this method reports no difference.
     *
     * @param fileParams list of parameters from the file specification
     * @param scanParams list of parameters from the scanned specification
     * @return true if query parameters differ by count or by per-type distribution, false otherwise
     */
    private static boolean compareQueryParams(List<Parameter> fileParams, List<Parameter> scanParams) {
        List<Parameter> fileQueryParams = filterByIn(fileParams, "query");
        List<Parameter> scanQueryParams = filterByIn(scanParams, "query");

        // scan에 query가 없으면 비교하지 않음
        if (scanQueryParams.isEmpty()) {
            return false;
        }

        // Query parameter 개수 비교
        if (fileQueryParams.size() != scanQueryParams.size()) {
            return true;
        }

        // 타입별 개수 비교
        Map<String, Integer> fileTypeCounts = countTypesByType(fileQueryParams);
        Map<String, Integer> scanTypeCounts = countTypesByType(scanQueryParams);

        // 타입별 개수가 다르면 차이 있음
        return !fileTypeCounts.equals(scanTypeCounts);
    }

    /**
         * Produce counts of parameters grouped by their schema type.
         *
         * <p>Parameters that are null or whose schema is null are counted under the
         * "unknown" key.</p>
         *
         * @param params the list of parameters to analyze; may contain null entries
         * @return a map from type name to count (key "unknown" represents parameters with missing schemas)
         */
    private static Map<String, Integer> countTypesByType(List<Parameter> params) {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (Parameter p : params) {
            if (p == null || p.getSchema() == null) {
                typeCounts.merge("unknown", 1, Integer::sum);
                continue;
            }

            String type = extractType(p.getSchema());
            typeCounts.merge(type, 1, Integer::sum);
        }
        return typeCounts;
    }

    /**
     * Produce a compact type identifier for the given Schema.
     *
     * @param schema the Schema to inspect; may be null
     * @return `'ref'` if the schema is a `$ref`, the schema's `type` value if present, or `'unknown'` otherwise (format is ignored)
     */
    private static String extractType(Schema schema) {
        if (schema == null) {
            return "unknown";
        }

        // $ref 우선
        String ref = nullToEmpty(schema.getRef());
        if (!ref.isEmpty()) {
            return "ref";
        }

        String type = nullToEmpty(schema.getType());
        if (type.isEmpty()) {
            return "unknown";
        }

        // format은 무시하고 type만 반환
        return type;
    }

    /**
     * Determines whether the request bodies differ between the file specification and the scanned specification.
     *
     * <p>Comparison considers presence/absence of the request body, equality of media type keys, and per-media-type
     * schema differences. Referenced schemas are resolved using {@code schemaMatchResults} when available.</p>
     *
     * @param fileReqBody         request body from the file specification
     * @param scanReqBody         request body from the scanned specification
     * @param schemaMatchResults  map of referenced schema names to booleans indicating whether the file's referenced
     *                            schema is considered a match to the scanned schema; may be empty or null
     * @return                    `true` if the request bodies differ, `false` otherwise
     */
    private static boolean compareRequestBody(RequestBody fileReqBody, RequestBody scanReqBody, Map<String, Boolean> schemaMatchResults) {
        // 둘 다 없으면 동일
        if (fileReqBody == null && scanReqBody == null) {
        return false;
    }

        // 하나만 있으면 다름
        if (fileReqBody == null || scanReqBody == null) {
            return true;
        }
        
        // content 비교
        Map<String, MediaType> fileContent = safe(fileReqBody.getContent());
        Map<String, MediaType> scanContent = safe(scanReqBody.getContent());
        
        // content keySet이 다르면 다름
        if (!fileContent.keySet().equals(scanContent.keySet())) {
            return true;
        }
        
        // 각 media type별로 schema 비교
        for (String mediaType : scanContent.keySet()) {
            MediaType fileMedia = fileContent.get(mediaType);
            MediaType scanMedia = scanContent.get(mediaType);
            
            if (fileMedia == null || scanMedia == null) {
                return true;
            }
            
            boolean schemaDiff = compareRequestSchema(fileMedia.getSchema(), scanMedia.getSchema(), schemaMatchResults);
            if (schemaDiff) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determine whether two request body schemas differ.
     *
     * When both schemas use `$ref`, equal ref names are resolved using {@code schemaMatchResults}: a mapped
     * value of `true` means the schemas are considered identical, `false` means they differ; if no mapping
     * exists, the function defers (considers them identical only insofar as a direct comparison is required).
     *
     * @param fileSchema          the schema from the file specification
     * @param scanSchema          the schema from the scanned specification
     * @param schemaMatchResults  map from schema ref name to a Boolean indicating whether the referenced schemas match
     * @return                    `true` if the schemas differ, `false` otherwise
     */
    private static boolean compareRequestSchema(Schema fileSchema, Schema scanSchema, Map<String, Boolean> schemaMatchResults) {
        // 둘 다 없으면 동일
        if (fileSchema == null && scanSchema == null) {
            return false;
        }
        
        // 하나만 있으면 다름
        if (fileSchema == null || scanSchema == null) {
            return true;
        }
        
        // $ref 비교
        String fileRef = extractSchemaRef(fileSchema);
        String scanRef = extractSchemaRef(scanSchema);
        
        // 둘 다 ref가 있는 경우
        if (!fileRef.isEmpty() && !scanRef.isEmpty()) {
            // ref 이름만 추출해서 비교
            String fileRefName = extractRefName(fileRef);
            String scanRefName = extractRefName(scanRef);
            
            // 이름이 다르면 다름
            if (!fileRefName.equals(scanRefName)) {
                return true;
            }
            
            // 이름이 같으면 정답지 확인
            Boolean schemaMatch = schemaMatchResults.get(fileRefName);
            
            // 정답지가 없으면 스키마 직접 비교가 필요 (false)
            if (schemaMatch == null) {
                return false;
            }
            
            // 정답지가 true면 동일, false면 다름
            return !schemaMatch;
        }
        
        // 하나만 ref인 경우는 다름
        if (!fileRef.isEmpty() || !scanRef.isEmpty()) {
            return true;
        }
        
        // 둘 다 inline schema인 경우 - type 비교
        String fileType = nullToEmpty(fileSchema.getType());
        String scanType = nullToEmpty(scanSchema.getType());
        
        return !fileType.equals(scanType);
    }

    /**
     * Retrieve the `$ref` string from a Schema.
     *
     * @param schema the schema to inspect; may be null
     * @return the `$ref` value if present, otherwise an empty string
     */
    private static String extractSchemaRef(Schema schema) {
        if (schema == null) {
            return "";
        }
        return nullToEmpty(schema.getRef());
    }

    /**
     * Extracts the schema name from a JSON Reference (`$ref`) string.
     *
     * @param ref the `$ref` string (e.g., "#/components/schemas/User"); may be empty
     * @return the substring after the last '/' (e.g., "User"), the original `ref` if no '/' is present, or an empty string if `ref` is empty
     */
    private static String extractRefName(String ref) {
        if (ref.isEmpty()) {
            return "";
        }
        int lastSlash = ref.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < ref.length() - 1) {
            return ref.substring(lastSlash + 1);
        }
        return ref;
    }

    /**
         * Return parameters whose `in` property matches the given value (case-insensitive).
         *
         * @param list    list of parameters to filter; null elements within the list are ignored
         * @param inValue target `in` value to match (null is treated as empty)
         * @return        a new list containing parameters whose `in` equals `inValue` (case-insensitive)
         */
    private static List<Parameter> filterByIn(List<Parameter> list, String inValue) {
        List<Parameter> out = new ArrayList<>();
        String targetIn = nullToEmpty(inValue).toLowerCase();
        for (Parameter p : list) {
            if (p == null) continue;
            String in = nullToEmpty(p.getIn()).toLowerCase();
            if (targetIn.equals(in)) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Index parameters by their lowercase name.
     *
     * @param params the list of parameters to index; null entries are ignored
     * @return a map from lowercase parameter name to the corresponding Parameter; parameters with null or empty names are omitted
     */
    private static Map<String, Parameter> indexByName(List<Parameter> params) {
        Map<String, Parameter> indexed = new HashMap<>();
        for (Parameter p : params) {
            if (p == null) continue;
            String name = nullToEmpty(p.getName()).toLowerCase();
            if (!name.isEmpty()) {
                indexed.put(name, p);
            }
        }
        return indexed;
    }

    /**
     * Normalize a possibly-null list to a non-null list.
     *
     * @param l a list that may be null
     * @param <T> the list element type
     * @return the original list if non-null, otherwise an empty immutable list
     */

    private static <T> List<T> safeList(List<T> l) {
        return (l == null) ? Collections.emptyList() : l;
    }

    /**
     * Convert a possibly null string to an empty string.
     *
     * @param s the input string that may be null
     * @return `""` if `s` is null, otherwise the original `s`
     */
    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}