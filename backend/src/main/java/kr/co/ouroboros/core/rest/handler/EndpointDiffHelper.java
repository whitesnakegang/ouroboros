package kr.co.ouroboros.core.rest.handler;

import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;

import static kr.co.ouroboros.core.rest.handler.RequestDiffHelper.*;

/**
 * Helper class for comparing and synchronizing endpoint differences between file and scanned API
 * specifications.
 */
public final class EndpointDiffHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private EndpointDiffHelper() {
    }

    /**
     * Compare endpoints between file and scan specs. If the URL path is different or the HTTP
     * methods differ, marks the scanned endpoint as DIFF_ENDPOINT and returns true to skip further
     * comparison.
     *
     * @param path the API path to compare
     * @param file the file-based REST API specification to update/mark
     * @param scan the scanned runtime REST API specification used as the source of truth
     * @return true if endpoint diff exists (path or methods differ), false if endpoints match
     */
    public static boolean compareEndpoint(String path, OuroRestApiSpec file, OuroRestApiSpec scan) {
        PathItem scanPathItem = safe(scan.getPaths()).get(path);
        if (scanPathItem == null) {
            return false;
        }

        PathItem filePathItem = safe(file.getPaths()).get(path);

        // 파일에 path가 없으면: 스캔의 모든 메서드를 endpoint로 마킹
        if (filePathItem == null) {
            markAllMethodsAsEndpoint(scanPathItem, file, scan, path);
            return true;
        }

        // 같은 path에서 메서드별 비교
        boolean hasEndpointDiff = false;

        // 각 메서드 체크 (스캔에 있는 메서드만)
        if (scanPathItem.getGet() != null && filePathItem.getGet() == null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.GET);
            hasEndpointDiff = true;
        }
        if (scanPathItem.getPost() != null && filePathItem.getPost() == null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.POST);
            hasEndpointDiff = true;
        }
        if (scanPathItem.getPut() != null && filePathItem.getPut() == null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.PUT);
            hasEndpointDiff = true;
        }
        if (scanPathItem.getPatch() != null && filePathItem.getPatch() == null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.PATCH);
            hasEndpointDiff = true;
        }
        if (scanPathItem.getDelete() != null && filePathItem.getDelete() == null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.DELETE);
            hasEndpointDiff = true;
        }

        return hasEndpointDiff;
    }

    /**
     * Marks all HTTP methods in the scanned path item as endpoints in the file spec.
     *
     * @param scanPathItem the PathItem from the scanned spec containing all methods to mark
     * @param file         the file-based REST API specification to update
     * @param scan         the scanned REST API specification to copy from
     * @param path         the API path being processed
     */
    private static void markAllMethodsAsEndpoint(PathItem scanPathItem, OuroRestApiSpec file,
            OuroRestApiSpec scan, String path) {
        if (scanPathItem.getGet() != null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.GET);
        }
        if (scanPathItem.getPost() != null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.POST);
        }
        if (scanPathItem.getPut() != null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.PUT);
        }
        if (scanPathItem.getPatch() != null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.PATCH);
        }
        if (scanPathItem.getDelete() != null) {
            markEndpointAndOverwrite(file, scan, path, HttpMethod.DELETE);
        }
    }
}

