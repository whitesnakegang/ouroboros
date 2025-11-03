package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import java.util.UUID;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import lombok.extern.slf4j.Slf4j;

import static kr.co.ouroboros.core.rest.handler.RequestDiffHelper.*;

/**
 * Helper class for comparing and synchronizing endpoint differences between file and scanned API
 * specifications.
 */
@Slf4j
public final class EndpointDiffHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private EndpointDiffHelper() {
    }


    /**
     * Detects whether a URL is missing from the file-based API spec and, if missing, inserts and tags the scanned path item.
     *
     * @param url the endpoint path to check (e.g., "/pets")
     * @param pathsFile map of path strings to PathItem from the file-based specification; may be modified to add the scanned path
     * @param pathsScanned map of path strings to PathItem from the scanned specification used to populate missing entries
     * @return {@code true} if the URL was not present in {@code pathsFile}, was added from {@code pathsScanned}, and its operations were marked with the diff tag; {@code false} if the URL already existed in {@code pathsFile}
     */
    public static boolean isDiffUrl(String url, Map<String, PathItem> pathsFile, Map<String, PathItem> pathsScanned) {
        // url에 해당하는 ENDPOINT가 명세에 있는 경우
        if (pathsFile.get(url) != null) {
            log.info("URL : {} 존재", url);
            return false;
        }

        pathsFile.put(url, pathsScanned.get(url));
        PathItem addedPath = pathsFile.get(url);
        log.info("URL : {} 존재하지 않는 경우", url);
        for (RequestDiffHelper.HttpMethod method : RequestDiffHelper.HttpMethod.values()) {
            Operation op = getOperationByMethod(addedPath, method);
            if (op != null) {
                // Generate x-ouroboros-id if not present
                if (op.getXOuroborosId() == null) {
                    op.setXOuroborosId(UUID.randomUUID().toString());
                    log.debug("Generated x-ouroboros-id for {} {}: {}", method, url, op.getXOuroborosId());
                }
                op.setXOuroborosDiff("endpoint");
            }
        }

        return true;
    }

    /**
     * Determine whether the given file-based operation is marked as an endpoint diff.
     *
     * @param fileOp the operation from the file-based specification to check
     * @return `true` if the operation is marked as an endpoint diff, `false` otherwise
     */
    public static boolean isDiffStatusEndpoint(Operation fileOp) {
        return fileOp.getXOuroborosDiff()
                .equals("endpoint");
    }


    /**
     * Replace the operation for the given HTTP method at the specified URL in the file-based spec and mark it as an endpoint difference.
     *
     * Sets the provided scanned operation into the supplied file spec's PathItem for the given HTTP method and sets its `XOuroborosDiff` to "endpoint".
     *
     * @param url          the URL path whose operation will be replaced
     * @param scanOp       the operation from the scanned spec to apply
     * @param restFileSpec the file-based paths map to modify
     * @param method       the HTTP method whose operation should be replaced and marked
     */
    public static void markDiffEndpoint(String url, Operation scanOp, Map<String, PathItem> restFileSpec, HttpMethod method) {
        log.info("METHOD: [{}], URL: [{}]은 같지만 METHOD는 다름", method, url);
        PathItem pathItem = restFileSpec.get(url);

        // Generate x-ouroboros-id if not present
        if (scanOp.getXOuroborosId() == null) {
            scanOp.setXOuroborosId(UUID.randomUUID().toString());
            log.debug("Generated x-ouroboros-id for {} {}: {}", method, url, scanOp.getXOuroborosId());
        }

        setOperationByMethod(pathItem, method, scanOp);
        Operation operationByMethod = getOperationByMethod(pathItem, method);
        operationByMethod.setXOuroborosDiff("endpoint");
    }

    /**
     * Retrieve the Operation from a PathItem corresponding to the specified HTTP method.
     *
     * @param item       the PathItem containing operations for different HTTP methods
     * @param httpMethod the HTTP method whose Operation should be returned
     * @return the Operation for the specified HTTP method, or `null` if none is defined
     */
    private static Operation getOperationByMethod(PathItem item, HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> item.getGet();
            case POST -> item.getPost();
            case PUT -> item.getPut();
            case PATCH -> item.getPatch();
            case DELETE -> item.getDelete();
        };
    }

    /**
     * Sets the given Operation on the PathItem corresponding to the specified HTTP method.
     *
     * @param item       the PathItem to modify
     * @param httpMethod the HTTP method whose operation will be replaced
     * @param scanOp     the Operation to assign for the specified method
     */
    private static void setOperationByMethod(PathItem item, HttpMethod httpMethod, Operation scanOp) {
        switch (httpMethod) {
            case GET: item.setGet(scanOp); break;
            case POST: item.setPost(scanOp); break;
            case PUT: item.setPut(scanOp); break;
            case PATCH: item.setPatch(scanOp); break;
            case DELETE: item.setDelete(scanOp); break;
        }
    }
}
