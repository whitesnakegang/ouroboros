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
     * 두 스펙의 components.schemas를 비교합니다.
     *
     * @param restFileSpec    파일 기반 스펙
     * @param restScannedSpec 스캔된 스펙
     * @return 스키마명을 키로 하고 일치 여부를 값으로 하는 Map
     */
    private Map<String, Boolean> compareSchemas(OuroRestApiSpec restFileSpec, OuroRestApiSpec restScannedSpec) {
        return schemaComparator.compareSchemas(restScannedSpec.getComponents(), restFileSpec.getComponents());
    }

    /**
     * Compare request-side API definitions for the given path key and update the provided file-based spec as needed.
     *
     * @param key                the path key to compare (for example "/users/{id}")
     * @param restFileSpec       the file-based REST API specification to compare against and modify as required
     * @param restScannedSpec    the scanned specification used as the source of truth
     * @param schemaMatchResults 스키마별 일치 여부 Map
     */
    private void reqCompare(String key, OuroRestApiSpec restFileSpec, OuroRestApiSpec restScannedSpec, Map<String, Boolean> schemaMatchResults) {

    }

    /**
     * 특정 HTTP 메서드에 대한 요청을 비교합니다.
     *
     * @param method             HTTP 메서드명
     * @param scannedOperation   스캔된 Operation (기준)
     * @param fileOperation      파일 기반 Operation (비교 대상)
     * @param endpoint           엔드포인트 경로
     * @param schemaMatchResults 스키마별 일치 여부 Map
     */
    private void compareRequestForMethod(String method, kr.co.ouroboros.core.rest.common.dto.Operation scannedOperation,
            kr.co.ouroboros.core.rest.common.dto.Operation fileOperation,
            String endpoint, Map<String, Boolean> schemaMatchResults) {
        // TODO: 요청 비교 로직 구현 예정 (스키마 매칭 결과 활용)
        System.out.println(String.format("[REQUEST METHOD COMPARE] %s %s - 스키마 매칭 결과: %s",
                method, endpoint, schemaMatchResults));
    }

    /**
     * Compare and synchronize response definitions for the given API path in the file-backed REST spec.
     *
     * @param key                the path key identifying the endpoint (for example "/users/{id}")
     * @param restFileSpec       the file-based REST API specification to compare and update
     * @param restScannedSpec    the scanned specification used as the source of truth
     * @param schemaMatchResults 스키마별 일치 여부 Map
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