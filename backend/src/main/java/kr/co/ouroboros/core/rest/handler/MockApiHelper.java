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

    public static boolean checkMockApi(String path, OuroRestApiSpec file, OuroRestApiSpec scan) {

        PathItem filePathItem = file.getPaths().get(path);
        PathItem scanPathItem = scan.getPaths().get(path);

        boolean isMock = false;

        for(HttpMethod httpMethod : HttpMethod.values()) {
            Operation fileOp = getOperationByMethod(filePathItem, httpMethod);
            Operation scanOp = getOperationByMethod(scanPathItem, httpMethod);

            if(fileOp == null || scanOp == null) continue;

            if(scanOp.getXOuroborosProgress().equals("MOCK")) {
                fileOp.setXOuroborosProgress("MOCK");
                fileOp.setXOuroborosTag(scanOp.getXOuroborosTag());
                isMock = true;
            }
        }

        return isMock;
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
