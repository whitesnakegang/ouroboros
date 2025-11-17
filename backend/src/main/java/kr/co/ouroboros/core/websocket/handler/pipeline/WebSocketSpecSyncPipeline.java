package kr.co.ouroboros.core.websocket.handler.pipeline;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.global.spec.SpecValidationUtil;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.handler.helper.WebSocketSpecSyncHelper;
import kr.co.ouroboros.core.websocket.handler.comparator.WebSocketSchemaComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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

        // Handle case: file is null but scanned spec has operations (code exists without file)
        if (fileSpec == null && wsScannedSpec.getOperations() != null && !wsScannedSpec.getOperations().isEmpty()) {
            wsFileSpec = wsScannedSpec;
            Map<String, Operation> operations = wsFileSpec.getOperations();
            for (Map.Entry<String, Operation> entry : operations.entrySet()) {
                Operation operation = entry.getValue();
                if (operation != null) {
                    // Generate x-ouroboros-id if not present
                    if (operation.getXOuroborosId() == null) {
                        operation.setXOuroborosId(java.util.UUID.randomUUID().toString());
                    }
                    // Normalize tags to uppercase
                    if (operation.getTags() != null) {
                        operation.setTags(SpecValidationUtil.normalizeWebSocketTags(operation.getTags()));
                    }
                    operation.setXOuroborosDiff("channel");
                    operation.setXOuroborosProgress("none");
                }
            }
            return wsFileSpec;
        }

        Map<String, Boolean> schemaMap = schemaComparator.compareSchemas(wsFileSpec, wsScannedSpec);

        Map<String, Operation> fileOpMap = wsFileSpec.getOperations();
        Map<String, Operation> scanOpMap = wsScannedSpec.getOperations();

        // Early return if no scanned operations to process
        if (scanOpMap == null || scanOpMap.isEmpty()) {
            return wsFileSpec;
        }

        // Initialize fileOpMap if null (empty file case)
        if (fileOpMap == null) {
            fileOpMap = new java.util.LinkedHashMap<>();
            wsFileSpec.setOperations(fileOpMap);
        }

        // fileOpMap의 모든 operation 키에서 채널 이름을 추출하여 Map에 저장
        // 키: 채널 이름, 값: 해당 채널 이름을 가진 operation 리스트
        Map<String, List<Operation>> fileChannelNameOperationMap = WebSocketSpecSyncHelper.groupOperationsByChannelName(fileOpMap);

        for(String operationKey :  scanOpMap.keySet()) {

            Operation scanOp = scanOpMap.get(operationKey);
            if (scanOp == null || scanOp.getChannel() == null) {
                continue;
            }
            
            if(scanOp.getAction().equals("send")) continue;

            String channelRef = scanOp.getChannel().getRef();
            if (channelRef == null) {
                continue;
            }

            if(scanOp.getXOuroborosProgress() == null) continue;
            
            // 파일에 없는 경우 추가
            if(!fileChannelNameOperationMap.containsKey(channelRef)){
                // Normalize tags to uppercase
                if (scanOp.getTags() != null) {
                    scanOp.setTags(SpecValidationUtil.normalizeWebSocketTags(scanOp.getTags()));
                }

                scanOp.setXOuroborosDiff("channel");
                scanOp.setXOuroborosId(UUID.randomUUID().toString());
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

            List<Operation> fileOps = fileChannelNameOperationMap.get(channelRef);
            if (fileOps == null || fileOps.isEmpty()) {
                continue;
            }

            Operation fileOp = fileOps.get(0);
            if (fileOp == null || fileOp.getMessages() == null || fileOp.getMessages().isEmpty()) {
                continue;
            }
            if (scanOp.getMessages() == null || scanOp.getMessages().isEmpty()) {
                continue;
            }

            MessageReference fileMessage = fileOp.getMessages().get(0);
            MessageReference scanMessage = scanOp.getMessages().get(0);

            if (fileMessage == null || scanMessage == null) {
                continue;
            }

            // ref에서 메시지 이름 추출 후, 패키지명을 제외한 클래스명만 추출하여 비교
            String fileMessageName = WebSocketSpecSyncHelper.extractMessageNameFromRef(fileMessage.getRef());
            String scanMessageName = WebSocketSpecSyncHelper.extractMessageNameFromRef(scanMessage.getRef());

            String fileMessageClassName = WebSocketSpecSyncHelper.extractClassNameFromFullName(fileMessageName);
            String scanMessageClassName = WebSocketSpecSyncHelper.extractClassNameFromFullName(scanMessageName);

            if(fileMessageClassName != null && fileMessageClassName.equals(scanMessageClassName)){
                // 클래스명이 같은 경우 schema 정답지 확인
                String schemaName = scanMessageClassName;
                Boolean schemaMatches = schemaMap != null ? schemaMap.get(schemaName) : null;
                if(Boolean.TRUE.equals(schemaMatches)){
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
