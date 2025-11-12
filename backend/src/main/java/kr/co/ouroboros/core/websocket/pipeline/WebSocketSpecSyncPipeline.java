package kr.co.ouroboros.core.websocket.pipeline;

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

        // TODO 채털명으로 메세지 정답지 만들기 [AUTHOR : 방준엽]

        Map<String, Operation> scanOpMap = wsScannedSpec.getOperations();

        if (scanOpMap == null || scanOpMap.isEmpty()) {
            // 불일치 비교 안함
        }

        // TODO 파이프라인 구축
        for(String channel :  scanOpMap.keySet()) {

            // TODO 파일에 없는 채널일 경우 처리 [AUTHOR : 임강범]

            // TODO Tags 비교해서 x-ouroboros-progress 업데이트 [AUTHOR : 임강범]

            // TODO Schema 정답지 비교 COMPLETED인 경우만 여기 로직 실행 [AUTHOR : 임강범]
        }

        return null;
    }
}
