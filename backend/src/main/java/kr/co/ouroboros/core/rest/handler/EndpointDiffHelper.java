package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
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
                op.setXOuroborosDiff("endpoint");
            }
        }

        return true;
    }


    public static void markDiffEndpoint(String url, Operation scanOp, Map<String, PathItem> restFileSpec, HttpMethod method) {
        log.info("METHOD: [{}], URL: [{}]은 같지만 METHOD는 다름", method, url);
        PathItem pathItem = restFileSpec.get(url);
        setOperationByMethod(pathItem, method, scanOp);
        Operation operationByMethod = getOperationByMethod(pathItem, method);
        operationByMethod.setXOuroborosDiff("endpoint");
    }

    private static Operation getOperationByMethod(PathItem item, HttpMethod httpMethod) {
        return switch (httpMethod) {
            case GET -> item.getGet();
            case POST -> item.getPost();
            case PUT -> item.getPut();
            case PATCH -> item.getPatch();
            case DELETE -> item.getDelete();
        };
    }

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

