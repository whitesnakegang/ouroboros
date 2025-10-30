package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import org.springframework.stereotype.Component;

import static kr.co.ouroboros.core.rest.handler.RequestDiffHelper.*;

@Component
public class RestSpecSyncPipeline implements SpecSyncPipeline {

    /**
     * Synchronizes the file-based REST API specification with the scanned runtime specification.
     *
     * Compares the scanned specification's paths against the provided file specification and applies
     * synchronization logic so the returned spec reflects alignment with the scanned runtime state.
     *
     * @param fileSpec    the file-based API specification to validate and update
     * @param scannedSpec the runtime-scanned API specification used as the source of truth
     * @return            the file-based specification after synchronization with the scanned spec
     */
    @Override
    public OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {

        // TODO 파이프라인 구성
        OuroRestApiSpec restFileSpec = (OuroRestApiSpec) fileSpec;
        OuroRestApiSpec resetScannedSpec = (OuroRestApiSpec) scannedSpec;

        Map<String, PathItem> pathsScanned = safe(resetScannedSpec.getPaths());


        Map<String, Operation> s;

        for(String path : pathsScanned.keySet()) {
            // TODO endpoint 검증 로직
            reqCompare(path, restFileSpec, resetScannedSpec);
//            resCompare(key, restFileSpec);
        }

        return restFileSpec;
    }


    // 파일 스펙(file)과 스캔 스펙(scan)의 "요청" 차이만 반영
    private void reqCompare(String path, OuroRestApiSpec file, OuroRestApiSpec scan) {
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

    private void comparePair(Operation fileOp, Operation scanOp,
            OuroRestApiSpec file, OuroRestApiSpec scan, String path, HttpMethod m) {
        if (scanOp == null) return;
        if (fileOp == null) {
            markEndpointAndOverwrite(file, scan, path, m);
            return;
        }
        compareAndMarkRequest(fileOp, scanOp);
    }

    /** 파일에만 있는 메서드를 endpoint로 마킹 */
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
     * Compare and synchronize response definitions for the given API path in the file-backed REST spec.
     *
     * @param key the path key identifying the endpoint (for example "/users/{id}")
     * @param restFileSpec the file-based REST API specification to compare and update
     */
    private void resCompare(String key, OuroRestApiSpec restFileSpec) {

    }
}