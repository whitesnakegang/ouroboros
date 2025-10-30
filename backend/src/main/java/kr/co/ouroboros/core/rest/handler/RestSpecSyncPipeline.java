package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static kr.co.ouroboros.core.rest.handler.MockApiHelper.checkMockApi;
import static kr.co.ouroboros.core.rest.handler.RequestDiffHelper.*;
import static kr.co.ouroboros.core.rest.handler.EndpointDiffHelper.*;

@Component
public class RestSpecSyncPipeline implements SpecSyncPipeline {

    @Autowired
    private ResponseComparator responseComparator;

    @Autowired
    private SchemaComparator schemaComparator;

    /**
     * Synchronizes the file-based REST API specification with the scanned runtime specification.
     * <p>
     * Compares the scanned specification's paths against the provided file specification and applies synchronization logic so the returned spec reflects alignment with the scanned runtime state.
     *
     * @param fileSpec    the file-based API specification to validate and update
     * @param scannedSpec the runtime-scanned API specification used as the source of truth
     * @return the file-based specification after synchronization with the scanned spec
     */
    @Override
    public OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {

        // TODO 파이프라인 구성
        OuroRestApiSpec restFileSpec = (OuroRestApiSpec) fileSpec;
        OuroRestApiSpec restScannedSpec = (OuroRestApiSpec) scannedSpec;

        Map<String, Boolean> schemaMatchResults = compareSchemas(restFileSpec, restScannedSpec);

        Map<String, PathItem> pathsScanned = safe(restScannedSpec.getPaths());
        Map<String, PathItem> pathsFile = safe(restFileSpec.getPaths());


        for (String url : pathsScanned.keySet()) {

            // url이 다른가 먼저 봄
            if(isDiffUrl(url, pathsFile, pathsScanned)) continue;

            PathItem fileItem = pathsFile.get(url);
            PathItem scanItem = pathsScanned.get(url);

            for(HttpMethod httpMethod : HttpMethod.values()) {
                // method 별로 봄
                Operation fileOp = getOperationByMethod(fileItem, httpMethod);
                Operation scanOp = getOperationByMethod(scanItem, httpMethod);

                // scan이 없으면 볼 필요 없음 (미구현 상태)
                if(scanOp == null) continue;

                // 명세에 없는 endpoint를 만듦
                if (fileOp == null) {
                    // method 복사 후 diff enpoint로 상태 변경
                    // req res 검사 필요 없음
                    markDiffEndpoint(url, scanOp, pathsFile, httpMethod);
                    continue;
                }

                // rest API 똑같은 경우

                // scan의 x-ouroboros-progress가 MOCK이면 file에 그대로 마킹만 해주고 넘어감
                if(checkMockApi(url, restFileSpec, restScannedSpec)) continue;

                // 3. endpoint diff가 있으면 reqCompare, resCompare는 스킵
                reqCompare(url, fileOp, scanOp, schemaMatchResults);
                resCompare(url, fileOp, scanOp, schemaMatchResults);

            }
        }

        return restFileSpec;
    }

    private Operation getOperationByMethod(PathItem item, HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> item.getGet();
            case POST -> item.getPost();
            case PUT -> item.getPut();
            case PATCH -> item.getPatch();
            case DELETE -> item.getDelete();
        };
    }

    /**
     * Compare component schemas between the file-backed and scanned REST API specifications.
     *
     * @param restFileSpec    the file-based REST API specification to update
     * @param restScannedSpec the runtime-scanned REST API specification to compare against
     * @return a map keyed by schema name with `true` if the scanned schema matches the file schema, `false` otherwise
     */
    private Map<String, Boolean> compareSchemas(OuroRestApiSpec restFileSpec, OuroRestApiSpec restScannedSpec) {
        return schemaComparator.compareSchemas(restScannedSpec.getComponents(), restFileSpec.getComponents());
    }

    /**
     * Synchronizes and marks differences in request definitions for a single API path
     * between the file-based and scanned REST specifications.
     *
     * If the path is missing in the file spec, scanned methods for the path are marked
     * as endpoints and prepared for overwrite. If the path exists in the file spec,
     * each HTTP method (GET, POST, PUT, PATCH, DELETE) is compared: scanned methods
     * are either merged into the file spec with request diffs marked or marked as
     * endpoints when the file operation is absent. Methods present only in the file
     * spec are marked as file-only endpoints.
     *
     * @param path the API path to compare (e.g., "/users/{id}")
     * @param file the file-based REST API specification to update/mark
     * @param scan the scanned runtime REST API specification to compare against
     */
    private void reqCompare(String path, OuroRestApiSpec file, OuroRestApiSpec scan, Map<String, Boolean> schemaMatchResults) {
        Map<String, PathItem> filePaths = safe(file.getPaths());
        Map<String, PathItem> scanPaths = safe(scan.getPaths());

        PathItem fp = filePaths.get(path);
        PathItem sp = scanPaths.get(path);

        // 메서드별 요청 비교
        // 1. 스캔에 있는 메서드: 파일과 비교하여 request diff 마킹
        comparePair(fp.getGet(),    sp.getGet(),   file, scan, path, HttpMethod.GET);
        comparePair(fp.getPost(),   sp.getPost(),  file, scan, path, HttpMethod.POST);
        comparePair(fp.getPut(),    sp.getPut(),   file, scan, path, HttpMethod.PUT);
        comparePair(fp.getPatch(),  sp.getPatch(), file, scan, path, HttpMethod.PATCH);
        comparePair(fp.getDelete(), sp.getDelete(),file, scan, path, HttpMethod.DELETE);
    }

    /**
     * Compare a single HTTP method's operation between the file-based spec and the scanned spec and mark differences.
     *
     * If the scanned operation is absent, no action is taken. If the scanned operation exists but the file operation
     * is missing, the method on the file spec is marked as a differing endpoint and overwritten from the scanned spec.
     * If both operations exist, the request definitions are compared and differences are marked.
     *
     * @param fileOp the operation from the file-based specification, or {@code null} if absent
     * @param scanOp the operation from the scanned runtime specification, or {@code null} if absent
     * @param file the file-based REST API specification to update/mark
     * @param scan the scanned REST API specification used as the source of truth for comparisons
     * @param path the API path being compared (e.g., "/users/{id}")
     * @param m the HTTP method being compared (GET, POST, PUT, PATCH, DELETE)
     */
    private void comparePair(Operation fileOp, Operation scanOp,
            OuroRestApiSpec file, OuroRestApiSpec scan, String path, HttpMethod m) {
        if (scanOp == null) return;
        if (fileOp == null) {
            markEndpointAndOverwrite(file, scan, path, m);
            return;
        }
        compareAndMarkRequest(fileOp, scanOp);
    }


    /**
     * Synchronizes response definitions for a specific API path by comparing the scanned spec against the file-backed spec.
     *
     * For each HTTP method present in the scanned path, compares responses and updates the file-based definition as needed,
     * using schemaMatchResults to determine per-schema compatibility.
     *
     * @param key                the path key identifying the endpoint (for example "/users/{id}")
     * @param restFileSpec       the file-based REST API specification to be updated
     * @param restScannedSpec    the scanned REST API specification used as the source of truth
     * @param schemaMatchResults a map of schema name to boolean indicating whether each schema matches between scanned and file specs
     */
    private void resCompare(String key, OuroRestApiSpec restFileSpec, OuroRestApiSpec restScannedSpec, Map<String, Boolean> schemaMatchResults) {
        Map<String, PathItem> pathsScanned = restScannedSpec.getPaths();
        Map<String, PathItem> pathsFile = restFileSpec.getPaths();

        PathItem pathItemScanned = pathsScanned.get(key);
        PathItem pathItemFile = pathsFile.get(key);

        if (pathItemScanned == null || pathItemFile == null) {
            return;
        }

        System.out.println(String.format("[RESPONSE COMPARE] '%s' - 스키마 매칭 결과: %s", key, schemaMatchResults));

        // pathItemScanned를 기준으로 응답 비교 (스캔된 것만 확인)
        if (pathItemScanned.getGet() != null) {
            responseComparator.compareResponsesForMethod("GET", pathItemScanned.getGet(), pathItemFile.getGet(), key, schemaMatchResults);
        }

        if (pathItemScanned.getPost() != null) {
            responseComparator.compareResponsesForMethod("POST", pathItemScanned.getPost(), pathItemFile.getPost(), key, schemaMatchResults);
        }

        if (pathItemScanned.getPut() != null) {
            responseComparator.compareResponsesForMethod("PUT", pathItemScanned.getPut(), pathItemFile.getPut(), key, schemaMatchResults);
        }

        if (pathItemScanned.getPatch() != null) {
            responseComparator.compareResponsesForMethod("PATCH", pathItemScanned.getPatch(), pathItemFile.getPatch(), key, schemaMatchResults);
        }

        if (pathItemScanned.getDelete() != null) {
            responseComparator.compareResponsesForMethod("DELETE", pathItemScanned.getDelete(), pathItemFile.getDelete(), key, schemaMatchResults);
        }
    }
}