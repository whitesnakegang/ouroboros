package kr.co.ouroboros.core.websocket.pipeline;

import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSpecSyncPipeline implements SpecSyncPipeline {

    @Override
    public OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {

        OuroWebSocketApiSpec wsFileSpec = (OuroWebSocketApiSpec) fileSpec;
        OuroWebSocketApiSpec wsScannedSpec = (OuroWebSocketApiSpec) scannedSpec;

        Map<String, Operation> scanOpMap = wsScannedSpec.getOperations();

        if (scanOpMap == null || scanOpMap.isEmpty()) {
            // 불일치 비교 안함
        }

        // TODO 파이프라인 구축
        for(String channel :  scanOpMap.keySet()) {

            // tags 확인 (tags 자체가 없거나 progress가 completed가 아닌 경우)

        }

        return null;
    }
}
