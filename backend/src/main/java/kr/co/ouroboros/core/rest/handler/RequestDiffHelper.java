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

    public static <K, V> Map<K, V> safe(Map<K, V> m) {
        return (m == null) ? Collections.emptyMap() : m;
    }

    /**
     * Compare request parameters between file and scan operations and mark differences.
     * 
     * This is called only when both fileOp and scanOp exist for the same URL and HTTP method.
     * Compares path and query parameters, and marks DIFF_REQUEST if different, DIFF_NONE otherwise.
     *
     * @param fileOp the operation from the file specification (will be updated)
     * @param scanOp the operation from the scanned specification
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
     * Compare parameters between file and scan.
     * Path와 Query parameter를 분리하여 비교한다.
     * 
     * @param fileParams parameters from file specification
     * @param scanParams parameters from scanned specification
     * @return true if parameters differ, false otherwise
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
     * Compare path parameters between file and scan.
     * Path parameter는 이름과 타입을 모두 비교한다.
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
     * Compare query parameters between file and scan.
     * Query parameter는 타입별 개수만 비교한다.
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
     * Count parameters by their schema type.
     * 
     * @param params the list of parameters to analyze
     * @return map of type -> count
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
     * Extract type identifier from Schema.
     * 
     * @param schema the Schema to extract type from
     * @return type identifier string (schema type만 사용, format 무시)
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
     * Compare request body between file and scan operations.
     * 
     * @param fileReqBody request body from file specification
     * @param scanReqBody request body from scanned specification
     * @param schemaMatchResults schema match results map
     * @return true if request body differs, false otherwise
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
     * Compare schemas from request body.
     * 
     * @param fileSchema schema from file specification
     * @param scanSchema schema from scanned specification
     * @param schemaMatchResults schema match results map
     * @return true if schemas differ, false otherwise
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
     * Extract $ref from Schema
     */
    private static String extractSchemaRef(Schema schema) {
        if (schema == null) {
            return "";
        }
        return nullToEmpty(schema.getRef());
    }

    /**
     * Extract schema name from $ref
     * e.g., "#/components/schemas/User" -> "User"
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
     * Filter parameters by 'in' value
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
     * Index parameters by name (lowercase)
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

    // ==================== null-safe helpers ====================

    private static <T> List<T> safeList(List<T> l) {
        return (l == null) ? Collections.emptyList() : l;
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}
