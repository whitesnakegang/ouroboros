package kr.co.ouroboros.core.rest.handler;

import java.util.*;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Parameter;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.common.dto.Schema;

public final class RequestDiffHelper {

    private RequestDiffHelper() {}

    // diff states
    public static final String DIFF_NONE     = "none";
    public static final String DIFF_REQUEST  = "request";
    public static final String DIFF_RESPONSE = "response";
    public static final String DIFF_ENDPOINT = "endpoint";
    public static final String DIFF_BOTH     = "both";

    // 메서드 타입
    public enum HttpMethod { GET, POST, PUT, PATCH, DELETE }

    /** 파일에 스캔의 특정 메서드를 추가하고 endpoint로 마킹 (null-safe) */
    public static void markEndpointAndOverwrite(OuroRestApiSpec file, OuroRestApiSpec scan, String path, HttpMethod method) {
        PathItem scanPI = safe(scan.getPaths()).get(path);
        if (scanPI == null) return;

        // 파일의 PathItem 가져오기 (없으면 새로 생성)
        PathItem filePI = file.getPaths().computeIfAbsent(path, k -> new PathItem());

        // 스캔에서 해당 메서드 가져오기
        Operation scanOp = getOp(scanPI, method);
        if (scanOp == null) return;

        // 파일의 PathItem에 스캔의 해당 메서드를 복사 (덮어쓰기)
        setOp(filePI, method, scanOp);

        // 해당 메서드를 endpoint로 마킹
        Operation fileOp = getOp(filePI, method);
        if (fileOp != null) {
            fileOp.setXOuroborosDiff(DIFF_ENDPOINT);
        }
    }

    /** 파일/스캔 Operation의 Request parameter만 비교하여 diff 마킹 */
    public static void compareAndMarkRequest(Operation fileOp, Operation scanOp) {
        if (scanOp == null) return;   // 스캔본에 메서드가 없으면 비교 스킵
        if (fileOp == null)  return;  // 파일에 메서드가 없을 때는 호출측에서 endpoint 처리

        boolean paramsDiff = diffPathQueryParams(fileOp.getParameters(), scanOp.getParameters());
        
        fileOp.setXOuroborosDiff(mergeDiff(fileOp.getXOuroborosDiff(), paramsDiff ? DIFF_REQUEST : DIFF_NONE));
    }

    // ==================== 내부 비교 유틸 ====================

    /** parameters 중 in=path|query만 비교
     * 1. parameter 개수가 다른 경우
     * 2. parameter 타입의 개수가 다른 경우 (확장성 고려)
     */
    private static boolean diffPathQueryParams(List<Parameter> fileParams, List<Parameter> scanParams) {
        List<Parameter> fileFiltered = filterPathOrQuery(safeList(fileParams));
        List<Parameter> scanFiltered = filterPathOrQuery(safeList(scanParams));

        // 1. parameter 개수 비교
        if (fileFiltered.size() != scanFiltered.size()) {
            return true;
        }

        // 2. parameter 타입별 개수 비교
        Map<String, Integer> fileTypeCounts = countTypesByType(fileFiltered);
        Map<String, Integer> scanTypeCounts = countTypesByType(scanFiltered);

        // 타입별 개수가 다르면 차이 있음
        if (!fileTypeCounts.equals(scanTypeCounts)) {
            return true;
        }

        return false;
    }

    /** 파라미터 리스트에서 타입별 개수를 세어 Map으로 반환 (확장성 고려)
     * key: 타입명 (예: "string", "integer", "number" 등)
     * value: 해당 타입의 개수
     */
    private static Map<String, Integer> countTypesByType(List<Parameter> params) {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (Parameter p : params) {
            if (p == null || p.getSchema() == null) {
                // 스키마가 없으면 "unknown"으로 처리 (확장성 고려)
                typeCounts.merge("unknown", 1, Integer::sum);
                continue;
            }
            
            Schema schema = p.getSchema();
            String type = extractType(schema);
            typeCounts.merge(type, 1, Integer::sum);
        }
        return typeCounts;
    }

    /** Schema에서 타입을 추출 (확장성 고려)
     * - $ref가 있으면 "ref"로 처리
     * - type이 있으면 type 사용 (format이 있으면 함께 고려)
     * - 둘 다 없으면 "unknown"
     */
    private static String extractType(Schema schema) {
        if (schema == null) {
            return "unknown";
        }

        // $ref가 우선
        String ref = nz(schema.getRef());
        if (!ref.isEmpty()) {
            return "ref";
        }

        // type 추출
        String type = nz(schema.getType());
        if (type.isEmpty()) {
            return "unknown";
        }

        // format이 있으면 함께 고려 (확장성 고려)
        String format = nz(schema.getFormat());
        if (!format.isEmpty()) {
            return type + ":" + format;
        }

        return type;
    }

    private static List<Parameter> filterPathOrQuery(List<Parameter> list) {
        List<Parameter> out = new ArrayList<>();
        for (Parameter p : list) {
            String in = nz(p.getIn()).toLowerCase();
            if ("path".equals(in) || "query".equals(in)) out.add(p);
        }
        return out;
    }

    // 얕은 fingerprint: type + format + (object=required/properties키), (array=items.type), (prim=type)
    private static String schemaFingerprint(Schema s) {
        if (s == null) return "null";
        
        // $ref가 있으면 $ref가 우선
        String ref = nz(s.getRef());
        if (!ref.isEmpty()) {
            return "ref{" + ref + "}";
        }
        
        String t = nz(s.getType());
        String f = nz(s.getFormat());

        if ("object".equalsIgnoreCase(t)) {
            var req  = new ArrayList<>(safeList(s.getRequired()));
            Collections.sort(req);
            var keys = new ArrayList<>(safe(s.getProperties()).keySet());
            Collections.sort(keys);
            String formatPart = f.isEmpty() ? "" : "|fmt=" + f;
            return "obj{req=" + String.join(",", req) + ";props=" + String.join(",", keys) + formatPart + "}";
        }
        if ("array".equalsIgnoreCase(t)) {
            if (s.getItems() != null) {
                // items의 $ref도 고려
                String itemRef = nz(s.getItems().getRef());
                if (!itemRef.isEmpty()) {
                    return "arr{item=ref{" + itemRef + "}}";
                }
                String itemType = nz(s.getItems().getType());
                String itemFormat = nz(s.getItems().getFormat());
                String formatPart = itemFormat.isEmpty() ? "" : "|fmt=" + itemFormat;
                return "arr{item=" + itemType + formatPart + "}";
            }
            return "arr{item=}";
        }
        String formatPart = f.isEmpty() ? "" : "|fmt=" + f;
        return "prim{" + t + formatPart + "}";
    }

    /** diff 병합: none|request|response|endpoint|both */
    public static String mergeDiff(String existing, String incoming) {
        String curr = norm(existing);
        switch (incoming) {
            case DIFF_ENDPOINT:
                return DIFF_ENDPOINT;            // 가장 강함
            case DIFF_REQUEST:
                if (DIFF_RESPONSE.equals(curr)) return DIFF_BOTH;
                if (DIFF_ENDPOINT.equals(curr)) return DIFF_ENDPOINT;
                return DIFF_REQUEST;
            case DIFF_NONE:
            default:
                return curr.isEmpty() ? DIFF_NONE : curr; // 유지
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        String v = s.trim().toLowerCase();
        return switch (v) {
            case DIFF_NONE, DIFF_REQUEST, DIFF_RESPONSE, DIFF_ENDPOINT, DIFF_BOTH -> v;
            default -> "";
        };
    }

    // ==================== 공통 접근 유틸 ====================

    public static Operation getOp(PathItem p, HttpMethod m) {
        if (p == null) return null;
        return switch (m) {
            case GET    -> p.getGet();
            case POST   -> p.getPost();
            case PUT    -> p.getPut();
            case PATCH  -> p.getPatch();
            case DELETE -> p.getDelete();
        };
    }

    public static void setOp(PathItem p, HttpMethod m, Operation op) {
        if (p == null) return;
        switch (m) {
            case GET    -> p.setGet(op);
            case POST   -> p.setPost(op);
            case PUT    -> p.setPut(op);
            case PATCH  -> p.setPatch(op);
            case DELETE -> p.setDelete(op);
        }
    }

    // ==================== null-safe helpers ====================

    public static <K,V> Map<K,V> safe(Map<K,V> m) {
        return (m == null) ? Collections.emptyMap() : m;
    }
    public static <T> List<T> safeList(List<T> l) {
        return (l == null) ? Collections.emptyList() : l;
    }
    public static String nz(String s) {
        return (s == null) ? "" : s;
    }
}
