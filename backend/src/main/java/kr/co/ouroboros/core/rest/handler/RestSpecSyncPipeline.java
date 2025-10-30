package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static kr.co.ouroboros.core.rest.handler.RequestDiffHelper.*;

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


        for (String key : pathsScanned.keySet()) {
            if (restFileSpec.getPaths()
                    .get(key) == null) {
                System.out.println(String.format("[ENDPOINT MISSING] '%s': 파일 스펙에 해당 엔드포인트가 없습니다.", key));
            }
            reqCompare(key, restFileSpec, restScannedSpec, schemaMatchResults);
            resCompare(key, restFileSpec, restScannedSpec, schemaMatchResults);
        }

        return restFileSpec;
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

        if (fp == null) {
            // 파일에 path 자체가 없으면: 스캔에 있는 메서드만 endpoint로 마킹하며 덮어쓰기
            comparePair(null, sp.getGet(),    file, scan, path, HttpMethod.GET);
            comparePair(null, sp.getPost(),   file, scan, path, HttpMethod.POST);
            comparePair(null, sp.getPut(),    file, scan, path, HttpMethod.PUT);
            comparePair(null, sp.getPatch(),  file, scan, path, HttpMethod.PATCH);
            comparePair(null, sp.getDelete(), file, scan, path, HttpMethod.DELETE);
            return;
        }

        // 메서드별 요청 비교
        // 1. 스캔에 있는 메서드: 파일과 비교하여 request diff 마킹 또는 endpoint로 마킹
        comparePair(fp.getGet(),    sp.getGet(),   file, scan, path, HttpMethod.GET);
        comparePair(fp.getPost(),   sp.getPost(),  file, scan, path, HttpMethod.POST);
        comparePair(fp.getPut(),    sp.getPut(),   file, scan, path, HttpMethod.PUT);
        comparePair(fp.getPatch(),  sp.getPatch(), file, scan, path, HttpMethod.PATCH);
        comparePair(fp.getDelete(), sp.getDelete(),file, scan, path, HttpMethod.DELETE);

        // 2. 파일에만 있는 메서드: 스캔에는 없으므로 endpoint로 마킹
        markFileOnlyMethods(fp, sp);
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
     * Marks HTTP methods that exist in the file path but are absent in the scanned path as endpoint differences.
     *
     * For each of GET, POST, PUT, PATCH, and DELETE: if the file's operation exists and the scanned operation is null,
     * merges the `DIFF_ENDPOINT` marker into the operation's `XOuroborosDiff`.
     *
     * @param fp the PathItem from the file-based spec to update
     * @param sp the PathItem from the scanned runtime spec to compare against; if null no changes are made
     */
    private void markFileOnlyMethods(PathItem fp, PathItem sp) {
        if (fp == null || sp == null) return;

        // 각 메서드가 파일에만 있는지 확인하여 endpoint로 마킹
        if (fp.getGet() != null && sp.getGet() == null) {
            fp.getGet().setXOuroborosDiff(mergeDiff(fp.getGet().getXOuroborosDiff(), DIFF_ENDPOINT));
        }
        if (fp.getPost() != null && sp.getPost() == null) {
            fp.getPost().setXOuroborosDiff(mergeDiff(fp.getPost().getXOuroborosDiff(), DIFF_ENDPOINT));
        }
        if (fp.getPut() != null && sp.getPut() == null) {
            fp.getPut().setXOuroborosDiff(mergeDiff(fp.getPut().getXOuroborosDiff(), DIFF_ENDPOINT));
        }
        if (fp.getPatch() != null && sp.getPatch() == null) {
            fp.getPatch().setXOuroborosDiff(mergeDiff(fp.getPatch().getXOuroborosDiff(), DIFF_ENDPOINT));
        }
        if (fp.getDelete() != null && sp.getDelete() == null) {
            fp.getDelete().setXOuroborosDiff(mergeDiff(fp.getDelete().getXOuroborosDiff(), DIFF_ENDPOINT));
        }
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