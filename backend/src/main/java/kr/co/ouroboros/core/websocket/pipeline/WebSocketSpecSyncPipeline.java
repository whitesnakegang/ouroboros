package kr.co.ouroboros.core.websocket.pipeline;

import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.handler.comparator.WebSocketSchemaComparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketSpecSyncPipeline implements SpecSyncPipeline {

    private final WebSocketSchemaComparator schemaComparator;

    @Override
    public OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {

        OuroWebSocketApiSpec wsFileSpec = (OuroWebSocketApiSpec) fileSpec;
        OuroWebSocketApiSpec wsScannedSpec = (OuroWebSocketApiSpec) scannedSpec;

        // TODO 채털명으로 메세지 정답지 만들기 [AUTHOR : 방준엽]
        Map<String, Boolean> schemaMap = schemaComparator.compareSchemas(wsFileSpec, wsScannedSpec);
        
        Map<String, Operation> fileOpMap = wsFileSpec.getOperations();
        Map<String, Operation> scanOpMap = wsScannedSpec.getOperations();

        if (scanOpMap == null || scanOpMap.isEmpty()) {
            // 불일치 비교 안함
        }

        // fileOpMap의 모든 operation 키에서 채널 이름을 추출하여 Map에 저장
        // 키: 채널 이름, 값: 해당 채널 이름을 가진 operation 리스트
        Map<String, List<Operation>> fileChannelNameOperationMap = WebSocketSpecSyncHelper.groupOperationsByChannelName(fileOpMap);

        // TODO 파이프라인 구축
        for(String operationKey :  scanOpMap.keySet()) {

            Operation scanOp = scanOpMap.get(operationKey); // TODO: 나중에 Tags 비교 및 Schema 비교 로직에서 사용 예정
            String channelRef = scanOp.getChannel().getRef();

            // TODO : scanOp에서 progress == null 건너 뜀
            if(scanOp.getXOuroborosProgress() == null) continue;
            
            // 파일에 없는 경우 추가
            if(!fileChannelNameOperationMap.containsKey(channelRef)){
                scanOp.setXOuroborosDiff("channel");
                fileOpMap.put(operationKey, scanOp);
                
                // Operation이 참조하는 Channel 추가
                WebSocketSpecSyncHelper.addReferencedChannel(wsFileSpec, wsScannedSpec, channelRef);
                
                // Operation이 참조하는 Message와 Schema 추가
                WebSocketSpecSyncHelper.addReferencedMessagesAndSchemas(wsFileSpec, wsScannedSpec, scanOp);
                
                continue;
            }
            
            // TODO Tags 비교해서 x-ouroboros-progress 업데이트 [AUTHOR : 임강범]
            if(!scanOp.getXOuroborosProgress().equals("completed")) {
                for (Operation operation : fileChannelNameOperationMap.get(channelRef)) {
                    operation.setXOuroborosProgress(scanOp.getXOuroborosProgress());
                }
                continue;
            }

            // TODO Schema 정답지 비교 COMPLETED인 경우만 여기 로직 실행 [AUTHOR : 임강범]
            Operation fileOp = fileChannelNameOperationMap.get(channelRef)
                    .get(0);
            MessageReference fileMessage = fileOp.getMessages().get(0);
            MessageReference scanMessage = scanOp.getMessages().get(0);

            if(fileMessage.getRef().equals(scanMessage.getRef())){
                // $ref가 같은 경우 schema 정답지 확인
                if(schemaMap.get(WebSocketSpecSyncHelper.extractClassNameFromRef(scanMessage.getRef()))){
                    // 정답인 경우
                    for (Operation operation : fileChannelNameOperationMap.get(channelRef)) {
                        operation.setXOuroborosProgress("completed");
                        operation.setXOuroborosDiff("none");
                    }
                } else {
                    for (Operation operation : fileChannelNameOperationMap.get(channelRef)) {
                        operation.setXOuroborosProgress("none");
                        operation.setXOuroborosDiff("message");
                    }
                }
            } else {
                for (Operation operation : fileChannelNameOperationMap.get(channelRef)) {
                    operation.setXOuroborosProgress("none");
                    operation.setXOuroborosDiff("message");
                }
            }
        }

        return wsFileSpec;
    }
}
