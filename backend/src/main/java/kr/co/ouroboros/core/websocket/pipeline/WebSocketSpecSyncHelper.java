package kr.co.ouroboros.core.websocket.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import kr.co.ouroboros.core.websocket.common.dto.Channel;
import kr.co.ouroboros.core.websocket.common.dto.Components;
import kr.co.ouroboros.core.websocket.common.dto.Message;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.Payload;
import kr.co.ouroboros.core.websocket.common.dto.PayloadSchema;
import kr.co.ouroboros.core.websocket.common.dto.Reply;
import kr.co.ouroboros.core.websocket.common.dto.Schema;

/**
 * WebSocket API Specification 동기화를 위한 헬퍼 클래스.
 * <p>
 * WebSocketSpecSyncPipeline에서 사용되는 유틸리티 메서드들을 제공합니다.
 *
 * @since 1.0.0
 */
public class WebSocketSpecSyncHelper {

    private WebSocketSpecSyncHelper() {
        // Utility class - 인스턴스 생성 방지
    }

    /**
     * Operation Map을 채널 이름별로 그룹화합니다.
     * 
     * @param operationMap operation 키와 Operation 객체를 담은 Map
     * @return 채널 이름을 키로 하고, 해당 채널 이름을 가진 operation 리스트를 값으로 하는 Map
     */
    public static Map<String, List<Operation>> groupOperationsByChannelName(Map<String, Operation> operationMap) {
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
    public static String extractChannelNameFromOperationKey(String operationKey) {
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
    public static String extractClassNameFromRef(String ref) {
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

    /**
     * Operation이 참조하는 Channel을 fileSpec에 추가합니다.
     * 이미 존재하는 경우에는 추가하지 않습니다.
     * 
     * @param fileSpec 파일에서 로드한 스펙 (대상)
     * @param scannedSpec 스캔한 스펙 (소스)
     * @param operation 추가된 Operation
     * @param channelName 채널 이름
     */
    public static void addReferencedChannel(OuroWebSocketApiSpec fileSpec, OuroWebSocketApiSpec scannedSpec, Operation operation, String channelName) {
        // channels Map 초기화
        Map<String, Channel> fileChannels = fileSpec.getChannels();
        if (fileChannels == null) {
            fileChannels = new HashMap<>();
            fileSpec.setChannels(fileChannels);
        }
        
        // 이미 존재하는지 확인
        if (fileChannels.containsKey(channelName)) {
            return; // 이미 존재하므로 추가하지 않음
        }
        
        // scannedSpec에서 channel 가져오기
        Map<String, Channel> scanChannels = scannedSpec.getChannels();
        if (scanChannels == null || !scanChannels.containsKey(channelName)) {
            return; // 스캔된 스펙에 없으면 추가할 수 없음
        }
        
        Channel channel = scanChannels.get(channelName);
        if (channel == null) {
            return;
        }
        
        // fileSpec에 channel 추가
        fileChannels.put(channelName, channel);
        
        // 채널의 messages에서 참조하는 components.messages도 확인
        if (channel.getMessages() != null) {
            for (MessageReference messageRef : channel.getMessages().values()) {
                if (messageRef.getRef() != null) {
                    addMessageFromRef(fileSpec, scannedSpec, messageRef.getRef());
                }
            }
        }
    }

    /**
     * Operation이 참조하는 Message와 Schema를 fileSpec의 components에 추가합니다.
     * 이미 존재하는 경우에는 추가하지 않습니다.
     * 
     * @param fileSpec 파일에서 로드한 스펙 (대상)
     * @param scannedSpec 스캔한 스펙 (소스)
     * @param operation 추가된 Operation
     */
    public static void addReferencedMessagesAndSchemas(OuroWebSocketApiSpec fileSpec, OuroWebSocketApiSpec scannedSpec, Operation operation) {
        // Components 초기화 확인
        Components fileComponents = fileSpec.getComponents();
        if (fileComponents == null) {
            fileComponents = new Components();
            fileSpec.setComponents(fileComponents);
        }
        
        Components scanComponents = scannedSpec.getComponents();
        if (scanComponents == null) {
            return; // 스캔된 스펙에 components가 없으면 추가할 수 없음
        }
        
        // messages Map 초기화
        if (fileComponents.getMessages() == null) {
            fileComponents.setMessages(new HashMap<>());
        }
        
        // schemas Map 초기화
        if (fileComponents.getSchemas() == null) {
            fileComponents.setSchemas(new HashMap<>());
        }
        
        // Operation의 messages 순회
        if (operation.getMessages() != null) {
            for (MessageReference messageRef : operation.getMessages()) {
                if (messageRef.getRef() != null) {
                    addMessageFromRef(fileSpec, scannedSpec, messageRef.getRef());
                }
            }
        }
        
        // Operation의 reply가 있는 경우 처리
        if (operation.getReply() != null) {
            Reply reply = operation.getReply();
            if (reply.getMessages() != null) {
                for (MessageReference messageRef : reply.getMessages()) {
                    if (messageRef.getRef() != null) {
                        addMessageFromRef(fileSpec, scannedSpec, messageRef.getRef());
                    }
                }
            }
        }
    }

    /**
     * $ref에서 Message를 추출하여 fileSpec에 추가합니다.
     * 
     * @param fileSpec 파일 스펙
     * @param scannedSpec 스캔된 스펙
     * @param ref Message reference (예: "#/components/messages/kr.co.ouroboros.core.websocket.test.ChatMessage" 또는 "#/channels/_chat.send/messages/...")
     */
    public static void addMessageFromRef(OuroWebSocketApiSpec fileSpec, OuroWebSocketApiSpec scannedSpec, String ref) {
        // ref에서 message 이름 추출 (전체 패키지 이름)
        String fullMessageName = extractMessageNameFromRef(ref);
        if (fullMessageName == null || fullMessageName.isEmpty()) {
            return;
        }
        
        // 클래스 이름만 추출 (fileSpec에 추가할 때 사용)
        String className = extractClassNameFromFullName(fullMessageName);
        if (className == null || className.isEmpty()) {
            className = fullMessageName; // 클래스 이름 추출 실패 시 전체 이름 사용
        }
        
        // 이미 존재하는지 확인 (클래스 이름으로)
        Components fileComponents = fileSpec.getComponents();
        if (fileComponents.getMessages() != null && fileComponents.getMessages().containsKey(className)) {
            return; // 이미 존재하므로 추가하지 않음
        }
        
        // scannedSpec에서 message 가져오기 (전체 패키지 이름으로)
        Components scanComponents = scannedSpec.getComponents();
        if (scanComponents == null || scanComponents.getMessages() == null) {
            return;
        }
        
        Message message = scanComponents.getMessages().get(fullMessageName);
        if (message == null) {
            return; // 스캔된 스펙에 없으면 추가할 수 없음
        }
        
        // fileSpec에 message 추가 (클래스 이름으로)
        fileComponents.getMessages().put(className, message);
        
        // message의 payload.schema.$ref에서 schema 추가
        if (message.getPayload() != null) {
            Payload payload = message.getPayload();
            if (payload.getSchema() != null) {
                PayloadSchema payloadSchema = payload.getSchema();
                if (payloadSchema.getRef() != null) {
                    addSchemaFromRef(fileSpec, scannedSpec, payloadSchema.getRef());
                }
            }
        }
        
        // message의 headers.$ref에서 schema 추가 (headers도 schema를 참조할 수 있음)
        if (message.getHeaders() != null && message.getHeaders().getRef() != null) {
            addSchemaFromRef(fileSpec, scannedSpec, message.getHeaders().getRef());
        }
    }

    /**
     * $ref에서 Schema를 추출하여 fileSpec에 추가합니다.
     * 
     * @param fileSpec 파일 스펙
     * @param scannedSpec 스캔된 스펙
     * @param ref Schema reference (예: "#/components/schemas/kr.co.ouroboros.core.websocket.test.ChatMessage")
     */
    public static void addSchemaFromRef(OuroWebSocketApiSpec fileSpec, OuroWebSocketApiSpec scannedSpec, String ref) {
        // ref에서 schema 이름 추출 (전체 패키지 이름)
        String fullSchemaName = extractSchemaNameFromRef(ref);
        if (fullSchemaName == null || fullSchemaName.isEmpty()) {
            return;
        }
        
        // 클래스 이름만 추출 (fileSpec에 추가할 때 사용)
        String className = extractClassNameFromFullName(fullSchemaName);
        if (className == null || className.isEmpty()) {
            className = fullSchemaName; // 클래스 이름 추출 실패 시 전체 이름 사용
        }
        
        // 이미 존재하는지 확인 (클래스 이름으로)
        Components fileComponents = fileSpec.getComponents();
        if (fileComponents.getSchemas() != null && fileComponents.getSchemas().containsKey(className)) {
            return; // 이미 존재하므로 추가하지 않음
        }
        
        // scannedSpec에서 schema 가져오기 (전체 패키지 이름으로)
        Components scanComponents = scannedSpec.getComponents();
        if (scanComponents == null || scanComponents.getSchemas() == null) {
            return;
        }
        
        Schema schema = scanComponents.getSchemas().get(fullSchemaName);
        if (schema == null) {
            return; // 스캔된 스펙에 없으면 추가할 수 없음
        }
        
        // fileSpec에 schema 추가 (클래스 이름으로)
        fileComponents.getSchemas().put(className, schema);
    }

    /**
     * $ref에서 Message 이름을 추출합니다.
     * 예: "#/components/messages/kr.co.ouroboros.core.websocket.test.ChatMessage" -> "kr.co.ouroboros.core.websocket.test.ChatMessage"
     * 예: "#/channels/_chat.send/messages/kr.co.ouroboros.core.websocket.test.ChatMessage" -> "kr.co.ouroboros.core.websocket.test.ChatMessage"
     * 
     * @param ref $ref 문자열
     * @return Message 이름
     */
    public static String extractMessageNameFromRef(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        
        // "#/components/messages/" 또는 "#/channels/.../messages/" 뒤의 부분 추출
        String componentsMessagesPrefix = "#/components/messages/";
        if (ref.startsWith(componentsMessagesPrefix)) {
            return ref.substring(componentsMessagesPrefix.length());
        }
        
        // "#/channels/.../messages/" 패턴 처리
        String channelsMessagesPrefix = "/messages/";
        int messagesIndex = ref.indexOf(channelsMessagesPrefix);
        if (messagesIndex != -1) {
            return ref.substring(messagesIndex + channelsMessagesPrefix.length());
        }
        
        return null;
    }

    /**
     * $ref에서 Schema 이름을 추출합니다.
     * 예: "#/components/schemas/kr.co.ouroboros.core.websocket.test.ChatMessage" -> "kr.co.ouroboros.core.websocket.test.ChatMessage"
     * 
     * @param ref $ref 문자열
     * @return Schema 이름
     */
    public static String extractSchemaNameFromRef(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        
        // "#/components/schemas/" 뒤의 부분 추출
        String componentsSchemasPrefix = "#/components/schemas/";
        if (ref.startsWith(componentsSchemasPrefix)) {
            return ref.substring(componentsSchemasPrefix.length());
        }
        
        return null;
    }

    /**
     * 전체 패키지 이름에서 마지막 클래스 이름을 추출합니다.
     * 예: "kr.co.ouroboros.core.websocket.test.ChatMessage" -> "ChatMessage"
     * 
     * @param fullName 전체 패키지 이름
     * @return 클래스 이름
     */
    public static String extractClassNameFromFullName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return null;
        }
        
        // 마지막 점(.)의 위치를 찾기
        int lastDotIndex = fullName.lastIndexOf('.');
        
        // 점이 없으면 전체 문자열 반환
        if (lastDotIndex == -1) {
            return fullName;
        }
        
        // 마지막 점 뒤의 부분을 반환
        return fullName.substring(lastDotIndex + 1);
    }
}

