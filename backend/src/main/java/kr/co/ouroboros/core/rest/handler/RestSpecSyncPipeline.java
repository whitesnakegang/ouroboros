package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import org.springframework.stereotype.Component;

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
        Map<String, PathItem> pathsScanned = resetScannedSpec.getPaths();


        for(String key : pathsScanned.keySet()) {
            reqCompare(key, restFileSpec);
            resCompare(key, restFileSpec);
        }

        return restFileSpec;
    }

    /**
     * Compare request-side API definitions for the given path key and update the provided file-based spec as needed.
     *
     * @param key the path key to compare (for example "/users/{id}")
     * @param restFileSpec the file-based REST API specification to compare against and modify as required
     */
    private void reqCompare(String key, OuroRestApiSpec restFileSpec) {
        Map<String, PathItem> pathsFile = restFileSpec.getPaths();
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