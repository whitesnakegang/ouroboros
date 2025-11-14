package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.core.websocket.spec.util.ReferenceConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manager for updating references in AsyncAPI documents.
 * <p>
 * Handles updating message and schema references when items are renamed during import operations.
 *
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketReferenceUpdater {

    private final WebSocketYamlParser yamlParser;

    /**
     * Updates message references in a channel's messages section.
     * <p>
     * When a message is renamed, updates the $ref in channel.messages.
     *
     * @param channel channel definition
     * @param messageRenameMap map of old message names to new names
     */
    @SuppressWarnings("unchecked")
    public void updateMessageReferencesInChannel(Map<String, Object> channel, Map<String, String> messageRenameMap) {
        if (messageRenameMap.isEmpty()) {
            return;
        }

        Object messagesObj = channel.get("messages");
        if (!(messagesObj instanceof Map)) {
            return;
        }

        Map<String, Object> messages = (Map<String, Object>) messagesObj;
        for (Map.Entry<String, Object> entry : new ArrayList<>(messages.entrySet())) {
            String messageName = entry.getKey();
            if (messageRenameMap.containsKey(messageName)) {
                // Message was renamed, update the key
                String newMessageName = messageRenameMap.get(messageName);
                Object messageRef = messages.remove(messageName);
                messages.put(newMessageName, messageRef);
                log.debug("🔗 Updated channel message reference: {} -> {}", messageName, newMessageName);
            }

            // Update $ref if it's a reference
            if (entry.getValue() instanceof Map) {
                Map<String, Object> messageRef = (Map<String, Object>) entry.getValue();
                Object refObj = messageRef.get("$ref");
                if (refObj instanceof String) {
                    String ref = (String) refObj;
                    if (ref.startsWith("#/components/messages/")) {
                        String refMessageName = ref.substring("#/components/messages/".length());
                        if (messageRenameMap.containsKey(refMessageName)) {
                            String newRefMessageName = messageRenameMap.get(refMessageName);
                            messageRef.put("$ref", "#/components/messages/" + newRefMessageName);
                            log.debug("🔗 Updated channel message $ref: {} -> {}", refMessageName, newRefMessageName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates message references in an operation.
     * <p>
     * When a message is renamed, updates the $ref in operation.messages and operation.reply.messages.
     *
     * @param operation operation definition
     * @param messageRenameMap map of old message names to new names
     */
    @SuppressWarnings("unchecked")
    public void updateMessageReferencesInOperation(Map<String, Object> operation, Map<String, String> messageRenameMap) {
        if (messageRenameMap.isEmpty()) {
            return;
        }

        // Update top-level messages
        Object messagesObj = operation.get("messages");
        if (messagesObj instanceof List) {
            List<Map<String, String>> messages = (List<Map<String, String>>) messagesObj;
            for (Map<String, String> messageRef : messages) {
                String ref = messageRef.get("$ref");
                if (ref != null) {
                    // Handle channel message references: #/channels/{channelName}/messages/{messageName}
                    if (ref.startsWith("#/channels/") && ref.contains("/messages/")) {
                        String[] parts = ref.split("/messages/");
                        if (parts.length == 2) {
                            String messageName = parts[1];
                            if (messageRenameMap.containsKey(messageName)) {
                                String newMessageName = messageRenameMap.get(messageName);
                                messageRef.put("$ref", parts[0] + "/messages/" + newMessageName);
                                log.debug("🔗 Updated operation message $ref: {} -> {}", messageName, newMessageName);
                            }
                        }
                    }
                    // Handle component message references: #/components/messages/{messageName}
                    else if (ref.startsWith("#/components/messages/")) {
                        String messageName = ref.substring("#/components/messages/".length());
                        if (messageRenameMap.containsKey(messageName)) {
                            String newMessageName = messageRenameMap.get(messageName);
                            messageRef.put("$ref", "#/components/messages/" + newMessageName);
                            log.debug("🔗 Updated operation message $ref: {} -> {}", messageName, newMessageName);
                        }
                    }
                }
            }
        }

        // Update reply messages
        Object replyObj = operation.get("reply");
        if (replyObj instanceof Map) {
            Map<String, Object> reply = (Map<String, Object>) replyObj;
            Object replyMessagesObj = reply.get("messages");
            if (replyMessagesObj instanceof List) {
                List<Map<String, String>> replyMessages = (List<Map<String, String>>) replyMessagesObj;
                for (Map<String, String> messageRef : replyMessages) {
                    String ref = messageRef.get("$ref");
                    if (ref != null) {
                        // Handle channel message references: #/channels/{channelName}/messages/{messageName}
                        if (ref.startsWith("#/channels/") && ref.contains("/messages/")) {
                            String[] parts = ref.split("/messages/");
                            if (parts.length == 2) {
                                String messageName = parts[1];
                                if (messageRenameMap.containsKey(messageName)) {
                                    String newMessageName = messageRenameMap.get(messageName);
                                    messageRef.put("$ref", parts[0] + "/messages/" + newMessageName);
                                    log.debug("🔗 Updated reply message $ref: {} -> {}", messageName, newMessageName);
                                }
                            }
                        }
                        // Handle component message references: #/components/messages/{messageName}
                        else if (ref.startsWith("#/components/messages/")) {
                            String messageName = ref.substring("#/components/messages/".length());
                            if (messageRenameMap.containsKey(messageName)) {
                                String newMessageName = messageRenameMap.get(messageName);
                                messageRef.put("$ref", "#/components/messages/" + newMessageName);
                                log.debug("🔗 Updated reply message $ref: {} -> {}", messageName, newMessageName);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Recursively updates all schema $ref references in messages according to schema rename map.
     * <p>
     * This updates message payload schema references.
     *
     * @param existingDoc the existing AsyncAPI document
     * @param schemaRenameMap map of old schema names to new names
     */
    public void updateSchemaReferencesInMessages(Map<String, Object> existingDoc, Map<String, String> schemaRenameMap) {
        if (schemaRenameMap.isEmpty()) {
            return;
        }

        Map<String, Object> components = yamlParser.getOrCreateComponents(existingDoc);
        if (components == null) {
            return;
        }

        Map<String, Object> messages = yamlParser.getMessages(existingDoc);
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : messages.entrySet()) {
            Object messageObj = entry.getValue();
            if (messageObj instanceof Map) {
                ReferenceConverter.updateSchemaReferences(messageObj, schemaRenameMap);
            }
        }
    }

    /**
     * Updates schema references in a single message definition.
     * <p>
     * Recursively scans the message payload for schema references and updates them.
     *
     * @param message message definition
     * @param schemaRenameMap map of old schema names to new names
     */
    public void updateSchemaReferencesInMessage(Map<String, Object> message, Map<String, String> schemaRenameMap) {
        ReferenceConverter.updateSchemaReferences(message, schemaRenameMap);
    }
}

