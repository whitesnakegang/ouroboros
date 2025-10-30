package kr.co.ouroboros.core.rest.handler;

import java.util.*;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Parameter;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.common.dto.Schema;

public final class RequestDiffHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private RequestDiffHelper() {
    }

    // diff states
    public static final String DIFF_NONE = "none";
    public static final String DIFF_REQUEST = "request";
    public static final String DIFF_RESPONSE = "response";
    public static final String DIFF_ENDPOINT = "endpoint";
    public static final String DIFF_BOTH = "both";

    // 메서드 타입
    public enum HttpMethod {GET, POST, PUT, PATCH, DELETE}

    /**
     * Copy the operation for the given path and HTTP method from the scan spec into the file spec
     * and mark it as an endpoint.
     *
     * @param file   target API spec to update (modified in place)
     * @param scan   source API spec to copy the operation from
     * @param path   path key identifying the PathItem to operate on
     * @param method HTTP method identifying which operation to copy and mark
     */
    public static void markEndpointAndOverwrite(OuroRestApiSpec file, OuroRestApiSpec scan,
            String path, HttpMethod method) {
        PathItem scanPI = safe(scan.getPaths()).get(path);
        if (scanPI == null) {
            return;
        }

        // 파일의 PathItem 가져오기 (없으면 새로 생성)
        PathItem filePI = file.getPaths()
                .computeIfAbsent(path, k -> new PathItem());

        // 스캔에서 해당 메서드 가져오기
        Operation scanOp = getOp(scanPI, method);
        if (scanOp == null) {
            return;
        }

        // 파일의 PathItem에 스캔의 해당 메서드를 복사 (덮어쓰기)
        setOp(filePI, method, scanOp);

        // 해당 메서드를 endpoint로 마킹
        Operation fileOp = getOp(filePI, method);
        if (fileOp != null) {
            fileOp.setXOuroborosDiff(DIFF_ENDPOINT);
        }
    }

    /**
     * Compare request path/query parameters between two operations and update the diff state on the
     * file operation.
     * <p>
     * Compares only parameters whose `in` is "path" or "query". If a difference is detected, merges
     * `DIFF_REQUEST` into `fileOp`'s XOuroborosDiff; otherwise merges `DIFF_NONE`. Does nothing if
     * either argument is null.
     *
     * @param fileOp the Operation from the file spec whose diff marker will be updated
     * @param scanOp the Operation from the scanned spec to compare against
     */
    public static void compareAndMarkRequest(Operation fileOp, Operation scanOp) {
        if (scanOp == null) {
            return;   // 스캔본에 메서드가 없으면 비교 스킵
        }
        if (fileOp == null) {
            return;  // 파일에 메서드가 없을 때는 호출측에서 endpoint 처리
        }

        boolean paramsDiff = diffPathQueryParams(fileOp.getParameters(), scanOp.getParameters());

        fileOp.setXOuroborosDiff(
                mergeDiff(fileOp.getXOuroborosDiff(), paramsDiff ? DIFF_REQUEST : DIFF_NONE));
    }

    // ==================== 내부 비교 유틸 ====================

    /**
     * Compare path and query parameters between two parameter lists for any differences.
     * <p>
     * Compares only parameters whose `in` is "path" or "query". A difference is reported if the
     * filtered parameter counts differ or if the distribution of parameter schema types (including
     * refs, formats, and unknowns) differs between the two lists.
     *
     * @param fileParams parameters from the file specification (may be null)
     * @param scanParams parameters from the scanned specification (may be null)
     * @return `true` if the path/query parameters differ by count or by type distribution, `false`
     * otherwise
     */
    private static boolean diffPathQueryParams(List<Parameter> fileParams,
            List<Parameter> scanParams) {
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

    /**
     * Count parameters by their schema-derived type and return a map of type names to occurrence
     * counts.
     * <p>
     * This treats parameters with a null Parameter or null Schema as type "unknown". Types are
     * determined by the schema classification returned from {@code extractType}, which may produce
     * values such as "string", "integer", "ref", "type:format", or "unknown".
     *
     * @param params the list of parameters to analyze
     * @return a map where each key is a type identifier and each value is the number of parameters
     * of that type
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

    /**
     * Derives a compact type identifier from a Schema for use in diffing.
     * <p>
     * The identifier is: - "ref" if the schema contains a $ref, - "type:format" if the schema has
     * both type and format, - "type" if the schema has type but no format, - "unknown" if the
     * schema is null or lacks both $ref and type.
     *
     * @param schema the Schema to inspect
     * @return a short string identifying the schema ("ref", "unknown", "type" or "type:format")
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

    /**
     * Filter a list of parameters to those whose `in` value is "path" or "query".
     *
     * @param list the input list of Parameter objects to filter
     * @return a new list containing only parameters with `in` equal to "path" or "query", in
     * original order
     */
    private static List<Parameter> filterPathOrQuery(List<Parameter> list) {
        List<Parameter> out = new ArrayList<>();
        for (Parameter p : list) {
            String in = nz(p.getIn()).toLowerCase();
            if ("path".equals(in) || "query".equals(in)) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Produce a compact fingerprint string representing the salient characteristics of the given
     * Schema.
     * <p>
     * The fingerprint encodes observable schema identity in a short, stable form: - If `s` is null
     * the result is the literal string `"null"`. - If the schema has a `$ref` the result is
     * `ref{<ref>}`. - For an object schema the result includes sorted required property names and
     * sorted property keys, and includes the schema format if present:
     * `obj{req=<req-list>;props=<prop-list>|fmt=<format>}` (the `|fmt=...` part is omitted when
     * format is absent). - For an array schema the result describes the items by reference
     * (`arr{item=ref{<ref>}}`) or by type and optional format (`arr{item=<type>|fmt=<format>}`), or
     * `arr{item=}` when items are not specified. - For primitive schemas the result is
     * `prim{<type>|fmt=<format>}` with the `|fmt=...` part omitted when format is absent.
     *
     * @param s the Schema to fingerprint (may be null)
     * @return a short, human-readable fingerprint string that captures type/ref, and when
     * applicable required keys, property names, item type/ref, and format
     */
    private static String schemaFingerprint(Schema s) {
        if (s == null) {
            return "null";
        }

        // $ref가 있으면 $ref가 우선
        String ref = nz(s.getRef());
        if (!ref.isEmpty()) {
            return "ref{" + ref + "}";
        }

        String t = nz(s.getType());
        String f = nz(s.getFormat());

        if ("object".equalsIgnoreCase(t)) {
            var req = new ArrayList<>(safeList(s.getRequired()));
            Collections.sort(req);
            var keys = new ArrayList<>(safe(s.getProperties()).keySet());
            Collections.sort(keys);
            String formatPart = f.isEmpty() ? "" : "|fmt=" + f;
            return "obj{req=" + String.join(",", req) + ";props=" + String.join(",", keys)
                    + formatPart + "}";
        }
        if ("array".equalsIgnoreCase(t)) {
            if (s.getItems() != null) {
                // items의 $ref도 고려
                String itemRef = nz(s.getItems()
                        .getRef());
                if (!itemRef.isEmpty()) {
                    return "arr{item=ref{" + itemRef + "}}";
                }
                String itemType = nz(s.getItems()
                        .getType());
                String itemFormat = nz(s.getItems()
                        .getFormat());
                String formatPart = itemFormat.isEmpty() ? "" : "|fmt=" + itemFormat;
                return "arr{item=" + itemType + formatPart + "}";
            }
            return "arr{item=}";
        }
        String formatPart = f.isEmpty() ? "" : "|fmt=" + f;
        return "prim{" + t + formatPart + "}";
    }

    /**
     * Merge two diff state strings resolving precedence into a single canonical diff state.
     * <p>
     * The incoming state has higher precedence: `DIFF_ENDPOINT` always wins; `DIFF_REQUEST` merges
     * with an existing `DIFF_RESPONSE` to produce `DIFF_BOTH` and preserves an existing
     * `DIFF_ENDPOINT`; `DIFF_REQUEST` otherwise becomes the result. An incoming `DIFF_NONE` (or
     * unknown) yields the normalized existing state if present, or `DIFF_NONE` when nothing valid
     * exists.
     *
     * @param existing an existing diff state string (may be null or unnormalized)
     * @param incoming a new diff state string to merge (expected to be one of the DIFF_*
     *                 constants)
     * @return a canonical diff state: one of DIFF_ENDPOINT, DIFF_BOTH, DIFF_REQUEST, or DIFF_NONE
     */
    public static String mergeDiff(String existing, String incoming) {
        String curr = norm(existing);
        switch (incoming) {
            case DIFF_ENDPOINT:
                return DIFF_ENDPOINT;            // 가장 강함
            case DIFF_REQUEST:
                if (DIFF_RESPONSE.equals(curr)) {
                    return DIFF_BOTH;
                }
                if (DIFF_ENDPOINT.equals(curr)) {
                    return DIFF_ENDPOINT;
                }
                return DIFF_REQUEST;
            case DIFF_NONE:
            default:
                return curr.isEmpty() ? DIFF_NONE : curr; // 유지
        }
    }

    /**
     * Normalize a diff-state string to a canonical lowercase value.
     *
     * @param s the input diff-state string (expected values: {@code DIFF_NONE},
     *          {@code DIFF_REQUEST}, {@code DIFF_RESPONSE}, {@code DIFF_ENDPOINT},
     *          {@code DIFF_BOTH})
     * @return the normalized lowercase diff-state constant if recognized, otherwise an empty string
     */
    private static String norm(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim()
                .toLowerCase();
        return switch (v) {
            case DIFF_NONE, DIFF_REQUEST, DIFF_RESPONSE, DIFF_ENDPOINT, DIFF_BOTH -> v;
            default -> "";
        };
    }

    /**
     * Retrieve the Operation for the given HTTP method from a PathItem.
     *
     * @param p the PathItem to read the operation from (may be null)
     * @param m the HTTP method whose Operation to retrieve
     * @return the Operation for the specified method, or {@code null} if the PathItem is null or
     * the operation is not present
     */

    public static Operation getOp(PathItem p, HttpMethod m) {
        if (p == null) {
            return null;
        }
        return switch (m) {
            case GET -> p.getGet();
            case POST -> p.getPost();
            case PUT -> p.getPut();
            case PATCH -> p.getPatch();
            case DELETE -> p.getDelete();
        };
    }

    /**
     * Set the Operation for the specified HTTP method on the given PathItem.
     *
     * @param p  the PathItem to modify; no action is taken if this is {@code null}
     * @param m  the HTTP method whose operation should be set
     * @param op the Operation to assign for the method (may be {@code null} to clear)
     */
    public static void setOp(PathItem p, HttpMethod m, Operation op) {
        if (p == null) {
            return;
        }
        switch (m) {
            case GET -> p.setGet(op);
            case POST -> p.setPost(op);
            case PUT -> p.setPut(op);
            case PATCH -> p.setPatch(op);
            case DELETE -> p.setDelete(op);
        }
    }

    /**
     * Provide a non-null map, substituting an empty map when the input is null.
     *
     * @return the original map if non-null, otherwise an empty immutable map
     */

    public static <K, V> Map<K, V> safe(Map<K, V> m) {
        return (m == null) ? Collections.emptyMap() : m;
    }

    /**
     * Return an empty list when the input is null; otherwise return the original list.
     *
     * @param <T> the element type of the list
     * @param l   the list that may be null
     * @return the original list if non-null, otherwise an immutable empty list
     */
    public static <T> List<T> safeList(List<T> l) {
        return (l == null) ? Collections.emptyList() : l;
    }

    /**
     * Normalize a string to a non-null value.
     *
     * @param s the input string that may be null
     * @return an empty string if {@code s} is null, otherwise {@code s}
     */
    public static String nz(String s) {
        return (s == null) ? "" : s;
    }
}