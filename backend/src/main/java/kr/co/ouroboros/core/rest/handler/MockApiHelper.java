package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.handler.RequestDiffHelper.HttpMethod;

public final class MockApiHelper {

    private MockApiHelper() {

    }

    public static boolean isMockApi(String path, Operation fileOp, Operation scanOp) {
        String xOuroborosProgress = scanOp.getXOuroborosProgress();
        if (xOuroborosProgress.equals("MOCK")) {
            fileOp.setXOuroborosProgress("MOCK");
            fileOp.setXOuroborosTag(scanOp.getXOuroborosTag());
            return true;
        }

        return false;
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
}
