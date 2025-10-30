package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

        OuroRestApiSpec restFileSpec = (OuroRestApiSpec) fileSpec;
        OuroRestApiSpec restScannedSpec = (OuroRestApiSpec) scannedSpec;

        // 1. 스키마 비교 (components.schemas)
        Map<String, Boolean> schemaMatchResults = compareSchemas(restFileSpec, restScannedSpec);

        // 2. 엔드포인트별 요청/응답 비교
        Map<String, PathItem> pathsScanned = restScannedSpec.getPaths();

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
         * Compare request-side API definitions for the given path and update the file-based REST spec when differences are detected.
         *
         * Compares request parameters, request bodies, and related request-level metadata for the specified path key between the scanned spec (source of truth)
         * and the file-backed spec, and mutates restFileSpec to reflect accepted changes.
         *
         * @param key                 the path key to compare (for example "/users/{id}")
         * @param restFileSpec        the file-based REST API specification to compare against and modify as required
         * @param restScannedSpec     the scanned specification used as the source of truth
         * @param schemaMatchResults  map of schema name to `true` if the scanned and file schemas match, `false` otherwise
         */
    private void reqCompare(String key, OuroRestApiSpec restFileSpec, OuroRestApiSpec restScannedSpec, Map<String, Boolean> schemaMatchResults) {

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