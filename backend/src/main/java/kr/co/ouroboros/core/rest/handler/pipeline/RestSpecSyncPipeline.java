package kr.co.ouroboros.core.rest.handler.pipeline;

import static kr.co.ouroboros.core.rest.handler.helper.EndpointDiffHelper.isDiffStatusEndpoint;
import static kr.co.ouroboros.core.rest.handler.helper.EndpointDiffHelper.isDiffUrl;
import static kr.co.ouroboros.core.rest.handler.helper.EndpointDiffHelper.markDiffEndpoint;
import static kr.co.ouroboros.core.rest.handler.helper.MockApiHelper.isMockApi;
import static kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper.HttpMethod;
import static kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper.compareAndMarkRequest;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.handler.comparator.ResponseComparator;
import kr.co.ouroboros.core.rest.handler.comparator.SchemaComparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

        OuroRestApiSpec restFileSpec = (OuroRestApiSpec) fileSpec;
        OuroRestApiSpec restScannedSpec = (OuroRestApiSpec) scannedSpec;

        if (fileSpec == null && !restScannedSpec.getPaths().isEmpty()) {
            restFileSpec = restScannedSpec;
            Map<String, PathItem> paths = restFileSpec.getPaths();
            for(String url : paths.keySet()){
                PathItem pathItem = paths.get(url);
                for(HttpMethod httpMethod : HttpMethod.values()){
                    Operation operationByMethod = getOperationByMethod(pathItem, httpMethod);
                    if(operationByMethod != null){
                        // Generate x-ouroboros-id if not present
                        if (operationByMethod.getXOuroborosId() == null) {
                            operationByMethod.setXOuroborosId(java.util.UUID.randomUUID().toString());
                            log.debug("Generated x-ouroboros-id for {} {}: {}", httpMethod, url, operationByMethod.getXOuroborosId());
                        }
                        operationByMethod.setXOuroborosDiff("endpoint");
                        operationByMethod.setXOuroborosTag("none");
                    }
                }
            }
            return restFileSpec;
        }

        // File 스펙과 Scan 스펙에 대해 각각 평탄화하여 타입별 개수 구하기
        Map<String, SchemaComparator.TypeCnts> fileFlattenedSchemas = schemaComparator.flattenSchemas(
                restFileSpec != null ? restFileSpec.getComponents() : null);
        Map<String, SchemaComparator.TypeCnts> scanFlattenedSchemas = schemaComparator.flattenSchemas(
                restScannedSpec != null ? restScannedSpec.getComponents() : null);

        // Scan 스펙을 기준으로 일치 여부 판단
        Map<String, Boolean> scanSchemaResults = schemaComparator.compareFlattenedSchemas(scanFlattenedSchemas, fileFlattenedSchemas);

        // Preserve components.securitySchemes from fileSpec (scannedSpec doesn't have securitySchemes from annotation)
        if (restFileSpec != null && restFileSpec.getComponents() != null && 
            restFileSpec.getComponents().getSecuritySchemes() != null) {
            // scannedSpec에 fileSpec의 securitySchemes 복사
            if (restScannedSpec.getComponents() == null) {
                restScannedSpec.setComponents(new kr.co.ouroboros.core.rest.common.dto.Components());
            }
            restScannedSpec.getComponents().setSecuritySchemes(restFileSpec.getComponents().getSecuritySchemes());
            log.info("✓ Preserved {} security scheme(s) from file spec: {}", 
                restFileSpec.getComponents().getSecuritySchemes().size(),
                restFileSpec.getComponents().getSecuritySchemes().keySet());
        }

        Map<String, PathItem> pathsScanned = restScannedSpec.getPaths();
        if (pathsScanned == null) {
            pathsScanned = new LinkedHashMap<>();
        }
        Map<String, PathItem> pathsFile = restFileSpec.getPaths();

        if (pathsFile == null) {
            pathsFile = new LinkedHashMap<>();
            restFileSpec.setPaths(pathsFile);
        }

        Iterator<Entry<String, PathItem>> it = pathsFile.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, PathItem> e = it.next();
            PathItem fileItem = e.getValue();

            int cnt = 0;
            for (HttpMethod httpMethod : HttpMethod.values()) {
                Operation fileOp = getOperationByMethod(fileItem, httpMethod);
                if (fileOp == null) continue;

                if ("endpoint".equals(fileOp.getXOuroborosDiff())) {
                    setOperationByMethodToNull(fileItem, httpMethod);
                } else {
                    cnt++;
                    fileOp.setXOuroborosDiff("none");
                    fileOp.setXOuroborosProgress("mock");
                    fileOp.setXOuroborosTag("none");
                }
            }

            if (cnt == 0) {
                it.remove();
            }
        }

        for (String url : pathsScanned.keySet()) {

            // url이 다른가 먼저 봄
            if(isDiffUrl(url, pathsFile, pathsScanned)) continue;

            PathItem fileItem = pathsFile.get(url);
            PathItem scanItem = pathsScanned.get(url);

            for (HttpMethod httpMethod : HttpMethod.values()) {
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

                // 각 method가 endpoint인지 확인
                if(isDiffStatusEndpoint(fileOp)) continue;



                // scan의 x-ouroboros-progress가 MOCK이면 file에 그대로 마킹만 해주고 넘어감
                if(isMockApi(fileOp, scanOp)) continue;

                // 3. endpoint diff가 있으면 reqCompare, resCompare는 스킵
                reqCompare(url, fileOp, scanOp, httpMethod, fileFlattenedSchemas, scanFlattenedSchemas);

                // 시영지기 @ApiResponse를 사용해서 명세를 정확히 작성했을 때만 response 검증
                if (scanOp.getXOuroborosResponse() != null && scanOp.getXOuroborosResponse()
                        .equals("use")) {
                    resCompare(url, httpMethod, fileOp, scanOp, scanSchemaResults);
                }
            }
        }

        return restFileSpec;
    }

    /**
     * Get the Operation for the specified HTTP method from a PathItem.
     *
     * @param item the PathItem containing operations for different HTTP methods
     * @param httpMethod the HTTP method whose Operation should be returned
     * @return the Operation corresponding to the given method, or null if none is defined
     */
    private Operation getOperationByMethod(PathItem item, HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> item.getGet();
            case POST -> item.getPost();
            case PUT -> item.getPut();
            case PATCH -> item.getPatch();
            case DELETE -> item.getDelete();
        };
    }

    private void setOperationByMethodToNull(PathItem item, HttpMethod httpMethod) {
        switch (httpMethod) {
            case GET -> item.setGet(null);
            case POST -> item.setPost(null);
            case PUT -> item.setPut(null);
            case PATCH -> item.setPatch(null);
            case DELETE -> item.setDelete(null);
        }
        ;
    }


    /**
     * Compare request definitions between the file and scanned operations for a specific URL and HTTP method and mark any differences.
     *
     * @param url the request path being compared
     * @param fileOp the operation from the file specification
     * @param scanOp the operation from the scanned specification
     * @param method the HTTP method for which parameters are compared
     * @param fileFlattenedSchemas flattened schema type counts extracted from the file specification, used to resolve and compare referenced types
     * @param scanFlattenedSchemas flattened schema type counts extracted from the scanned specification, used to resolve and compare referenced types
     */
    private void reqCompare(String url, Operation fileOp, Operation scanOp, HttpMethod method,
            Map<String, SchemaComparator.TypeCnts> fileFlattenedSchemas,
            Map<String, SchemaComparator.TypeCnts> scanFlattenedSchemas
    ) {
        compareAndMarkRequest(url, fileOp, scanOp, method, fileFlattenedSchemas, scanFlattenedSchemas);
    }

    /**
     * Compare responses for a specific endpoint and HTTP method using the scanned and file operations.
     *
     * @param url               the endpoint URL (path key)
     * @param method            the HTTP method for the comparison
     * @param fileOp            the Operation from the file-based specification
     * @param scanOp            the Operation from the runtime-scanned specification
     * @param scanSchemaResults scan-based schema comparison results (임시로 Boolean Map 사용)
     */
    private void resCompare(String url, HttpMethod method, Operation fileOp, Operation scanOp,
            Map<String, Boolean> scanSchemaResults) {
        // 이전 로직에 의해 fileOp과 scanOp은 endpoint랑 http-method가 똑같은 상태가 보장됨.
        // scan은 무조건 null이 아님
        // 파일 스펙과 스캔 스펙 결과를 병합 (스키마 이름이 중복되면 스캔 결과 우선)
        responseComparator.compareResponsesForMethod(url, method, scanOp, fileOp, scanSchemaResults);
    }
}