package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.Operation;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import kr.co.ouroboros.core.rest.handler.RequestDiffHelper.HttpMethod;

public final class MockApiHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private MockApiHelper() {

    }

    /**
     * Determines whether the operation should be treated as a mock and, if so, marks and tags the file operation accordingly.
     *
     * @param fileOp  the operation from the file to update when mock status applies
     * @param scanOp  the scanned operation whose mock metadata is the source of truth
     * @return `true` if the scanned operation's XOuroborosProgress equals "MOCK", `false` otherwise.
     */
    public static boolean isMockApi(Operation fileOp, Operation scanOp) {
        String xOuroborosProgress = scanOp.getXOuroborosProgress();
        if (xOuroborosProgress.equals("mock")) {
            fileOp.setXOuroborosProgress("mock");
            fileOp.setXOuroborosTag(scanOp.getXOuroborosTag());
            return true;
        }

        return false;
    }


    /**
     * Selects the Operation from a PathItem that corresponds to the given HTTP method.
     *
     * @param item the PathItem containing operations for various HTTP methods
     * @param httpMethod the HTTP method whose Operation should be returned
     * @return the Operation for the specified method, or `null` if that method is not defined on the PathItem
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
}