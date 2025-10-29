package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import org.springframework.stereotype.Component;

@Component
public class RestSpecSyncPipeline implements SpecSyncPipeline {

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

    // TODO implement
    private void reqCompare(String key, OuroRestApiSpec restFileSpec) {
        Map<String, PathItem> pathsFile = restFileSpec.getPaths();
    }

    // TODO implement
    private void resCompare(String key, OuroRestApiSpec restFileSpec) {

    }
}
