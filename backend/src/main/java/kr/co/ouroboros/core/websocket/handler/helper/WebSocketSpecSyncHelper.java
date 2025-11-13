package kr.co.ouroboros.core.websocket.handler.helper;

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

    /**
     * Prevents instantiation of this utility class.
     */
    private WebSocketSpecSyncHelper() {
        // Utility class - 인스턴스 생성 방지
    }

    /**
     * Group operations by their channel reference.
     *
     * @param operationMap map of operation identifiers to Operation objects; entries whose Operation or channel ref is null are ignored
     * @return a map whose keys are channel reference strings and whose values are lists of Operations that reference that channel
     */
    public static Map<String, List<Operation>> groupOperationsByChannelName(Map<String, Operation> operationMap) {
        Map<String, List<Operation>> channelNameOperationMap = new HashMap<>();
        if (operationMap != null) {
            for (Entry<String, Operation> operationEntry : operationMap.entrySet()) {
                String channelRef = operationEntry.getValue().getChannel().getRef();
                channelNameOperationMap
                    .computeIfAbsent(channelRef, k -> new ArrayList<>())
                    .add(operationEntry.getValue());
            }
        }
        return channelNameOperationMap;
    }

    /**
     * Extracts the simple class name appearing after the last dot in a `$ref` string.
     *
     * @param ref the `$ref` containing a dotted fully-qualified name (e.g. "#/channels/.../kr.co.package.ClassName")
     * @return the substring after the last '.', or the original string if no '.' is present; returns an empty string if `ref` is null or empty
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
     * Ensures the channel referenced by `channelRef` (and any messages/schemas it references)
     * from `scannedSpec` is present in `fileSpec`.
     *
     * The method derives the channel name as the substring after the last '/' in `channelRef`
     * (or uses the entire string if no '/' exists), adds that channel to `fileSpec` if absent,
     * and pulls in any referenced messages (and their schemas) from `scannedSpec`.
     *
     * @param fileSpec   the target spec to update; channels, messages, and schemas will be added here as needed
     * @param scannedSpec the source spec to read referenced channel, message, and schema definitions from
     * @param channelRef a reference string identifying a channel (e.g. "#/channels/_chat.send_new_test"); the name
     *                   used is the substring after the last '/' or the full string if '/' is not present
     */
    public static void addReferencedChannel(OuroWebSocketApiSpec fileSpec, OuroWebSocketApiSpec scannedSpec, String channelRef) {
        // channelRef에서 마지막 '/' 뒤의 부분을 파싱하여 channelName에 저장
        // 예: "#/channels/_chat.send_new_test" -> "_chat.send_new_test"
        String channelName;
        if (channelRef != null && !channelRef.isEmpty()) {
            int lastSlashIndex = channelRef.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < channelRef.length() - 1) {
                channelName = channelRef.substring(lastSlashIndex + 1);
            } else {
                channelName = channelRef; // '/'가 없으면 전체 문자열 사용
            }
        } else {
            return; // channelRef가 null이거나 비어있으면 처리하지 않음
        }
        
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
     * Ensure messages and related schemas referenced by an operation exist in the target spec's components.
     *
     * Initializes the target spec's components, messages map, and schemas map if absent, then copies any
     * referenced messages (from the operation and its reply) from the scanned spec into the target spec;
     * referenced payload/header schemas are also pulled in when a message is added. Null or missing
     * components/refs are ignored.
     *
     * @param fileSpec    the target OuroWebSocketApiSpec to receive referenced messages and schemas
     * @param scannedSpec the source OuroWebSocketApiSpec to read referenced messages and schemas from
     * @param operation   the operation whose message and reply references should be synchronized into fileSpec
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
     * Add the Message referenced by `ref` from `scannedSpec` into `fileSpec`, and ensure any schemas
     * referenced by that message's payload or headers are also added.
     *
     * @param fileSpec   target API spec to receive the referenced message and related schemas
     * @param scannedSpec source API spec to resolve the referenced message and schemas from
     * @param ref        message reference string (e.g. "#/components/messages/kr.co.ouroboros...ChatMessage"
     *                   or a channel-style reference containing "/messages/")
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
     * Add a schema referenced by a $ref from the scanned specification into the target file specification.
     *
     * If the reference cannot be resolved, the scanned spec lacks components/schemas, or a schema with the
     * derived class name already exists in the target spec, the method makes no changes.
     *
     * @param fileSpec     the target OuroWebSocketApiSpec to receive the schema
     * @param scannedSpec  the source OuroWebSocketApiSpec to resolve the referenced schema from
     * @param ref          the schema reference (e.g. "#/components/schemas/kr.co.ouroboros.core.websocket.test.ChatMessage")
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
     * Extracts the message name referenced by a $ref string.
     *
     * Supports refs that start with "#/components/messages/" and refs that contain "/messages/" (for channel-scoped message refs).
     *
     * @param ref the $ref string to extract the message name from
     * @return the message name (the substring after the recognized prefix) or `null` if no message name can be extracted
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
     * Extracts the schema name portion from a JSON Reference ($ref) that targets components/schemas.
     *
     * If `ref` starts with "#/components/schemas/", returns the substring after that prefix; otherwise returns `null`.
     *
     * @param ref the $ref string to parse (may be null or empty)
     * @return the schema name extracted from the ref, or `null` if the ref does not contain a components/schemas reference
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
     * Extracts the simple class name from a package-qualified name.
     *
     * @param fullName the package-qualified name (for example, "kr.co.ouroboros.core.websocket.test.ChatMessage")
     * @return the simple class name (for example, "ChatMessage"), or `null` if `fullName` is null or empty
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

