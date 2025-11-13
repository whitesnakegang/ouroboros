package kr.co.ouroboros.core.websocket.handler.pipeline;

import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.handler.helper.WebSocketSpecSyncHelper;
import kr.co.ouroboros.core.websocket.handler.comparator.WebSocketSchemaComparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketSpecSyncPipeline implements SpecSyncPipeline {

    private final WebSocketSchemaComparator schemaComparator;

    /**
     * Synchronizes and validates WebSocket API information from a scanned specification into a file specification.
     *
     * Compares schemas and operations between the provided specs, merges referenced channels/messages/schemas
     * that are missing from the file spec, and updates per-operation progress and diff flags based on comparison results.
     *
     * @param fileSpec    the existing file-side API specification to update
     * @param scannedSpec the incoming scanned API specification to compare against
     * @return the file-side API specification after applying synchronization and validation updates
     */
    @Override
    public OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {

        OuroWebSocketApiSpec wsFileSpec = (OuroWebSocketApiSpec) fileSpec;
        OuroWebSocketApiSpec wsScannedSpec = (OuroWebSocketApiSpec) scannedSpec;

        Map<String, Boolean> schemaMap = schemaComparator.compareSchemas(wsFileSpec, wsScannedSpec);
        
        Map<String, Operation> fileOpMap = wsFileSpec.getOperations();
        Map<String, Operation> scanOpMap = wsScannedSpec.getOperations();

        if (scanOpMap == null || scanOpMap.isEmpty()) {
            // 불일치 비교 안함
        }

        // fileOpMap의 모든 operation 키에서 채널 이름을 추출하여 Map에 저장
        // 키: 채널 이름, 값: 해당 채널 이름을 가진 operation 리스트
        Map<String, List<Operation>> fileChannelNameOperationMap = WebSocketSpecSyncHelper.groupOperationsByChannelName(fileOpMap);

        for(String operationKey :  scanOpMap.keySet()) {

            Operation scanOp = scanOpMap.get(operationKey);
            String channelRef = scanOp.getChannel().getRef();

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
            
            if(!scanOp.getXOuroborosProgress().equals("completed")) {
                for (Operation operation : fileChannelNameOperationMap.get(channelRef)) {
                    operation.setXOuroborosProgress(scanOp.getXOuroborosProgress());
                }
                continue;
            }

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