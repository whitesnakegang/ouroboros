package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static kr.co.ouroboros.core.rest.handler.MockApiHelper.isMockApi;
import static kr.co.ouroboros.core.rest.handler.RequestDiffHelper.*;
import static kr.co.ouroboros.core.rest.handler.EndpointDiffHelper.*;

@Slf4j
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
                if(isMockApi(url, fileOp, scanOp)) continue;

                // 3. endpoint diff가 있으면 reqCompare, resCompare는 스킵
                reqCompare(url, fileOp, scanOp, schemaMatchResults, httpMethod);
                resCompare(url, httpMethod, fileOp, scanOp, schemaMatchResults);

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
     * Compare request parameters between file and scan operations.
     *
     * This method is called only when both fileOp and scanOp exist for the same URL and HTTP method.
     * It compares parameters (path and query) and marks differences.
     *
     * @param fileOp the operation from the file specification
     * @param scanOp the operation from the scanned specification
     */
    private void reqCompare(String url, Operation fileOp, Operation scanOp, Map<String, Boolean> schemaMatchResults, HttpMethod method) {
        compareAndMarkRequest(url, fileOp, scanOp, method, schemaMatchResults);
    }

    private void resCompare(String url, HttpMethod method, Operation fileOp, Operation scanOp, Map<String, Boolean> schemaMatchResults) {
        // 이전 로직에 의해 fileOp과 scanOp은 endpoint랑 http-method가 똑같은 삳태가 보장됨.
        // scan은 무조건 null이 아님
        responseComparator.compareResponsesForMethod(url, method, scanOp, fileOp, schemaMatchResults);
    }
}