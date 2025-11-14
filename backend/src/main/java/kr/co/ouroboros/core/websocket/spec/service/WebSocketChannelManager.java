package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.ui.websocket.spec.dto.ChannelMessageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager for automatic channel creation and lifecycle management.
 * <p>
 * Handles channel creation, message updates, and automatic cleanup when channels
 * are no longer referenced by any operations.
 *
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelManager {

    private final WebSocketYamlParser yamlParser;

    /**
     * Ensures a channel exists, creating it if necessary.
     * <p>
     * Returns the channel name (either existing or newly created).
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelInfo channel information (address or channelRef + messages)
     * @return channel name
     */
    public String ensureChannelExists(Map<String, Object> asyncApiDoc, ChannelMessageInfo channelInfo) {
        String channelName;

        if (channelInfo.getChannelRef() != null) {
            // Use existing channel
            channelName = channelInfo.getChannelRef();
            Map<String, Object> existingChannel = yamlParser.getChannel(asyncApiDoc, channelName);
            if (existingChannel == null) {
                throw new IllegalArgumentException("Channel '" + channelName + "' not found");
            }
            // Update messages if provided
            if (channelInfo.getMessages() != null && !channelInfo.getMessages().isEmpty()) {
                updateChannelMessages(asyncApiDoc, channelName, channelInfo.getMessages());
            }
        } else if (channelInfo.getAddress() != null) {
            // Create new channel from address
            String address = channelInfo.getAddress();
            channelName = addressToChannelName(address);

            if (!yamlParser.channelExists(asyncApiDoc, channelName)) {
                Map<String, Object> channelDefinition = buildChannelDefinition(address, channelInfo.getMessages());
                yamlParser.putChannel(asyncApiDoc, channelName, channelDefinition);
                log.debug("Auto-created channel: {} (address: {})", channelName, address);
            } else {
                // Update messages if provided
                if (channelInfo.getMessages() != null && !channelInfo.getMessages().isEmpty()) {
                    updateChannelMessages(asyncApiDoc, channelName, channelInfo.getMessages());
                }
            }
        } else {
            throw new IllegalArgumentException("Either address or channelRef must be provided");
        }

        return channelName;
    }

    /**
     * Updates messages in an existing channel.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name
     * @param messageNames list of message names
     */
    @SuppressWarnings("unchecked")
    public void updateChannelMessages(Map<String, Object> asyncApiDoc, String channelName, List<String> messageNames) {
        Map<String, Object> channel = yamlParser.getChannel(asyncApiDoc, channelName);
        if (channel == null) {
            return;
        }

        Map<String, Object> messages = (Map<String, Object>) channel.get("messages");
        if (messages == null) {
            messages = new LinkedHashMap<>();
            channel.put("messages", messages);
        }

        // Add or update message references
        for (String messageName : messageNames) {
            Map<String, String> messageRef = new LinkedHashMap<>();
            messageRef.put("$ref", "#/components/messages/" + messageName);
            messages.put(messageName, messageRef);
        }
    }

    /**
     * Converts an address to a channel name.
     * <p>
     * Pattern: "/chat.send" -> "_chat.send", "/topic/rooms" -> "_topic_rooms"
     *
     * @param address channel address
     * @return channel name
     */
    public String addressToChannelName(String address) {
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        // Remove leading slash and replace remaining slashes with underscores
        String name = address.startsWith("/") ? address.substring(1) : address;
        return "_" + name.replace("/", "_");
    }

    /**
     * Builds a channel definition from address and messages.
     *
     * @param address channel address
     * @param messageNames list of message names
     * @return channel definition map
     */
    private Map<String, Object> buildChannelDefinition(String address, List<String> messageNames) {
        Map<String, Object> channel = new LinkedHashMap<>();
        channel.put("address", address);

        if (messageNames != null && !messageNames.isEmpty()) {
            Map<String, Object> messages = new LinkedHashMap<>();
            for (String messageName : messageNames) {
                Map<String, String> messageRef = new LinkedHashMap<>();
                messageRef.put("$ref", "#/components/messages/" + messageName);
                messages.put(messageName, messageRef);
            }
            channel.put("messages", messages);
        }

        // Add default STOMP bindings
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("stomp", new LinkedHashMap<>());
        channel.put("bindings", bindings);

        return channel;
    }

    /**
     * Extracts channel names referenced by an operation.
     * <p>
     * Includes both the main channel and reply channel.
     *
     * @param operation operation definition map
     * @return set of channel names referenced by the operation
     */
    @SuppressWarnings("unchecked")
    public Set<String> extractChannelReferences(Map<String, Object> operation) {
        Set<String> channelNames = new HashSet<>();

        // Extract main channel reference
        Object channelObj = operation.get("channel");
        if (channelObj instanceof Map) {
            Map<String, String> channelMap = (Map<String, String>) channelObj;
            String ref = channelMap.get("$ref");
            if (ref != null && ref.startsWith("#/channels/")) {
                String channelName = ref.substring("#/channels/".length());
                channelNames.add(channelName);
            }
        }

        // Extract reply channel reference
        Object replyObj = operation.get("reply");
        if (replyObj instanceof Map) {
            Map<String, Object> replyMap = (Map<String, Object>) replyObj;
            Object replyChannelObj = replyMap.get("channel");
            if (replyChannelObj instanceof Map) {
                Map<String, String> replyChannelMap = (Map<String, String>) replyChannelObj;
                String ref = replyChannelMap.get("$ref");
                if (ref != null && ref.startsWith("#/channels/")) {
                    String channelName = ref.substring("#/channels/".length());
                    channelNames.add(channelName);
                }
            }
        }

        return channelNames;
    }

    /**
     * Checks if a channel is used by any operation.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name to check
     * @return true if channel is used by any operation, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean isChannelUsedByOperations(Map<String, Object> asyncApiDoc, String channelName) {
        Map<String, Object> operations = yamlParser.getOperations(asyncApiDoc);
        if (operations == null || operations.isEmpty()) {
            return false;
        }

        String channelRef = "#/channels/" + channelName;

        // Check all operations
        for (Map.Entry<String, Object> entry : operations.entrySet()) {
            Map<String, Object> operation = (Map<String, Object>) entry.getValue();

            // Check main channel
            Object channelObj = operation.get("channel");
            if (channelObj instanceof Map) {
                Map<String, String> channelMap = (Map<String, String>) channelObj;
                String ref = channelMap.get("$ref");
                if (channelRef.equals(ref)) {
                    return true;
                }
            }

            // Check reply channel
            Object replyObj = operation.get("reply");
            if (replyObj instanceof Map) {
                Map<String, Object> replyMap = (Map<String, Object>) replyObj;
                Object replyChannelObj = replyMap.get("channel");
                if (replyChannelObj instanceof Map) {
                    Map<String, String> replyChannelMap = (Map<String, String>) replyChannelObj;
                    String ref = replyChannelMap.get("$ref");
                    if (channelRef.equals(ref)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Removes a channel from the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name to remove
     * @return true if channel was removed, false if it didn't exist
     */
    public boolean removeChannel(Map<String, Object> asyncApiDoc, String channelName) {
        return yamlParser.removeChannel(asyncApiDoc, channelName);
    }

    /**
     * Cleans up unused channels after an operation is deleted or updated.
     * <p>
     * Checks each channel in the provided set and removes it if no longer referenced.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelNames set of channel names to check
     */
    public void cleanupUnusedChannels(Map<String, Object> asyncApiDoc, Set<String> channelNames) {
        for (String channelName : channelNames) {
            if (!isChannelUsedByOperations(asyncApiDoc, channelName)) {
                boolean removed = removeChannel(asyncApiDoc, channelName);
                if (removed) {
                    log.info("Deleted unused channel: {} (no longer referenced by any operation)", channelName);
                }
            }
        }
    }
}
