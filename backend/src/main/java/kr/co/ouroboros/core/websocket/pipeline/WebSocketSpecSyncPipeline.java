package kr.co.ouroboros.core.websocket.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import kr.co.ouroboros.core.global.handler.SpecSyncPipeline;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
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
        Map<String, Boolean> resultMap = new HashMap<>();
        Map<String, Operation> fileOpMap = wsFileSpec.getOperations();
        Map<String, Operation> scanOpMap = wsScannedSpec.getOperations();

        if (scanOpMap == null || scanOpMap.isEmpty()) {
            // 불일치 비교 안함
        }

        // fileOpMap의 모든 operation 키에서 채널 이름을 추출하여 Map에 저장
        // 키: 채널 이름, 값: 해당 채널 이름을 가진 operation 리스트
        Map<String, List<Operation>> fileChannelNameOperationMap = groupOperationsByChannelName(fileOpMap);

        // TODO 파이프라인 구축
        for(String operationKey :  scanOpMap.keySet()) {

            Operation scanOp = scanOpMap.get(operationKey); // TODO: 나중에 Tags 비교 및 Schema 비교 로직에서 사용 예정
            String channelName = extractChannelNameFromOperationKey(operationKey);

            // 파일에 없는 경우 추가
            if(!fileChannelNameOperationMap.containsKey(channelName)){
                scanOp.setXOuroborosDiff("channel");
                fileOpMap.put(operationKey, scanOp);
                continue;
            }

            // TODO Tags 비교해서 x-ouroboros-progress 업데이트 [AUTHOR : 임강범]
            if(!scanOp.getXOuroborosProgress().equals("completed")) {
                for (Operation operation : fileChannelNameOperationMap.get(channelName)) {
                    operation.setXOuroborosProgress(scanOp.getXOuroborosProgress());
                }
                continue;
            }

            // TODO Schema 정답지 비교 COMPLETED인 경우만 여기 로직 실행 [AUTHOR : 임강범]
            Operation fileOp = fileChannelNameOperationMap.get(operationKey)
                    .get(0);
            MessageReference fileMessage = fileOp.getMessages().get(0);
            MessageReference scanMessage = scanOp.getMessages().get(0);

            if(fileMessage.getRef().equals(scanMessage.getRef())){
                // $ref가 같은 경우 schema 정답지 확인
                if(resultMap.get(extractClassNameFromRef(scanMessage.getRef()))){
                    // 정답인 경우
                    for (Operation operation : fileChannelNameOperationMap.get(channelName)) {
                        operation.setXOuroborosProgress("completed");
                        operation.setXOuroborosDiff("none");
                    }
                } else {
                    for (Operation operation : fileChannelNameOperationMap.get(channelName)) {
                        operation.setXOuroborosProgress("none");
                        operation.setXOuroborosDiff("message");
                    }
                }
            } else {
                for (Operation operation : fileChannelNameOperationMap.get(channelName)) {
                    operation.setXOuroborosProgress("none");
                    operation.setXOuroborosDiff("message");
                }
            }
        }

        return null;
    }

    /**
     * Operation Map을 채널 이름별로 그룹화합니다.
     * 
     * @param operationMap operation 키와 Operation 객체를 담은 Map
     * @return 채널 이름을 키로 하고, 해당 채널 이름을 가진 operation 리스트를 값으로 하는 Map
     */
    private Map<String, List<Operation>> groupOperationsByChannelName(Map<String, Operation> operationMap) {
        Map<String, List<Operation>> channelNameOperationMap = new HashMap<>();
        if (operationMap != null) {
            for (Entry<String, Operation> operationEntry : operationMap.entrySet()) {
                String channelName = extractChannelNameFromOperationKey(operationEntry.getKey());
                channelNameOperationMap
                    .computeIfAbsent(channelName, k -> new ArrayList<>())
                    .add(operationEntry.getValue());
            }
        }
        return channelNameOperationMap;
    }

    /**
     * Operation 키에서 채널 이름을 추출합니다.
     * 예: "_chat.send_receive_broadcastToDefault" -> "_chat.send"
     * 
     * @param operationKey operation의 키 (예: "_chat.send_receive_broadcastToDefault")
     * @return 채널 이름 (예: "_chat.send")
     */
    private String extractChannelNameFromOperationKey(String operationKey) {
        if (operationKey == null || operationKey.isEmpty()) {
            return "";
        }
        
        // 언더스코어로 분리
        String[] parts = operationKey.split("_");
        
        // 첫 번째와 두 번째 부분을 합쳐서 채널 이름 생성
        if (parts.length >= 2) {
            return parts[0] + "_" + parts[1];
        }
        
        // 언더스코어가 하나만 있거나 없는 경우 원본 반환
        return parts[0];
    }

    /**
     * $ref 문자열에서 마지막 점(.) 뒤의 클래스 이름을 추출합니다.
     * 예: "#/channels/_chat.sendToRoom/messages/kr.co.ouroboros.core.websocket.test.ChatMessage" -> "ChatMessage"
     * 
     * @param ref $ref 문자열 (예: "#/channels/_chat.sendToRoom/messages/kr.co.ouroboros.core.websocket.test.ChatMessage")
     * @return 클래스 이름 (예: "ChatMessage")
     */
    private String extractClassNameFromRef(String ref) {
        if (ref == null || ref.isEmpty()) {
            return "";
        }
        
        // 마지막 점(.)의 위치를 찾기
        int lastDotIndex = ref.lastIndexOf('.');
        
        // 점이 없으면 전체 문자열 반환
        if (lastDotIndex == -1) {
            return ref;
        }
        
        // 마지막 점 뒤의 부분을 반환
        return ref.substring(lastDotIndex + 1);
    }
}
