package kr.co.ouroboros.core.websocket.handler.helper;

import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.websocket.common.dto.Channel;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.Reply;
import kr.co.ouroboros.core.websocket.config.WebSocketPrefixProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Normalizes channel addresses in WebSocket API specifications by adding appropriate STOMP destination prefixes.
 * <p>
 * When Springwolf scans code annotations like {@code @MessageMapping("/chat/send")}, it captures only the
 * method path without the application destination prefix configured in Spring WebSocket. This utility adds
 * the correct prefix based on the operation's action type from the server's perspective:
 * <ul>
 *   <li><b>receive</b> operations ({@code @MessageMapping}): Add application destination prefix (e.g., "/app")</li>
 *   <li><b>send</b> operations ({@code @SendTo}): Broker prefixes are typically already present, verify and add if missing</li>
 * </ul>
 * <p>
 * Example transformation:
 * <pre>
 * Before: channel.address = "/chat/send", operation.action = "receive" (@MessageMapping)
 * After:  channel.address = "/app/chat/send"
 *
 * Before: channel.address = "/topic/messages", operation.action = "send" (@SendTo)
 * After:  channel.address = "/topic/messages" (already has broker prefix)
 * </pre>
 *
 * @since 0.1.0
 */
@Slf4j
public class ChannelAddressNormalizer {

    /**
     * Prevents instantiation of this utility class.
     */
    private ChannelAddressNormalizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Normalize all channel addresses in the API specification by adding appropriate prefixes.
     * <p>
     * Creates separate channels for each operation based on their action type to avoid conflicts
     * when the same base path is used with different prefixes:
     * <ul>
     *   <li><b>receive</b> operations: Create channel with "app_" prefix and "/app" destination prefix</li>
     *   <li><b>send</b> operations: Create channel with "topic_" prefix and "/topic" destination prefix</li>
     * </ul>
     * <p>
     * Example:
     * <pre>
     * Original:
     *   channels:
     *     _chat.send: {address: "/chat/send"}
     *   operations:
     *     handleMessage: {action: "receive", channel: {$ref: "#/channels/_chat.send"}}
     *     sendMessage: {action: "send", channel: {$ref: "#/channels/_chat.send"}}
     *
     * After normalization:
     *   channels:
     *     app_chat_send: {address: "/app/chat/send"}
     *     topic_chat_send: {address: "/topic/chat/send"}
     *   operations:
     *     handleMessage: {action: "receive", channel: {$ref: "#/channels/app_chat_send"}}
     *     sendMessage: {action: "send", channel: {$ref: "#/channels/topic_chat_send"}}
     * </pre>
     *
     * @param spec       the WebSocket API specification to normalize
     * @param properties the WebSocket prefix configuration containing application and broker prefixes
     */
    public static void normalizeChannelAddresses(OuroWebSocketApiSpec spec, WebSocketPrefixProperties properties) {
        if (spec == null || properties == null) {
            return;
        }

        Map<String, Operation> operations = spec.getOperations();
        Map<String, Channel> channels = spec.getChannels();

        if (operations == null || channels == null) {
            return;
        }

        // Track new channels to avoid modifying the map while iterating
        Map<String, Channel> newChannels = new java.util.LinkedHashMap<>();

        for (Operation operation : operations.values()) {
            if (operation == null || operation.getChannel() == null) {
                continue;
            }

            String channelRef = operation.getChannel().getRef();
            if (channelRef == null) {
                continue;
            }

            // Extract channel name from reference (e.g., "#/channels/_chat.send" -> "_chat.send")
            String originalChannelName = extractChannelNameFromRef(channelRef);
            if (originalChannelName == null) {
                continue;
            }

            Channel originalChannel = channels.get(originalChannelName);
            if (originalChannel == null || originalChannel.getAddress() == null) {
                continue;
            }

            String originalAddress = originalChannel.getAddress();
            String action = operation.getAction();

            // Check if address already has a known prefix - if so, skip normalization
            if (hasKnownPrefix(originalAddress, properties)) {
                log.debug("Channel {} already has prefix, skipping normalization", originalChannelName);
                continue;
            }

            // Determine prefix and new channel name based on action
            String prefix;
            String prefixLabel;

            if ("receive".equalsIgnoreCase(action)) {
                // @MessageMapping -> application destination prefix
                prefix = properties.getApplicationDestinationPrefix();
                prefixLabel = prefix.replace("/", ""); // Remove all slashes for label
            } else if ("send".equalsIgnoreCase(action)) {
                // @SendTo -> /topic prefix (use first broker prefix)
                List<String> brokerPrefixes = properties.getBrokerPrefixes();
                if (brokerPrefixes == null || brokerPrefixes.isEmpty()) {
                    continue;
                }
                prefix = brokerPrefixes.get(0);
                prefixLabel = prefix.replace("/", ""); // Remove all slashes for label
            } else {
                continue;
            }

            // Create new channel name: prefix_originalName (e.g., "app_chat_send", "topic_chat_send")
            String newChannelName = createPrefixedChannelName(prefixLabel, originalChannelName);

            // Create new channel with prefixed address if not already exists
            if (!newChannels.containsKey(newChannelName) && !channels.containsKey(newChannelName)) {
                Channel newChannel = new Channel();
                newChannel.setAddress(ensurePrefix(originalAddress, prefix));
                newChannel.setMessages(originalChannel.getMessages());
                newChannel.setBindings(originalChannel.getBindings());

                newChannels.put(newChannelName, newChannel);

                log.debug("Created new channel: {} with address: {} (action: {})",
                    newChannelName, newChannel.getAddress(), action);
            }

            // Update operation's channel reference to point to the new channel
            operation.getChannel().setRef("#/channels/" + newChannelName);

            // Update message references to use the new channel
            updateMessageReferences(operation.getMessages(), originalChannelName, newChannelName);

            // Process reply channel if present
            if (operation.getReply() != null && operation.getReply().getChannel() != null) {
                processReplyChannel(operation.getReply(), channels, newChannels, properties);
                // Update reply message references
                updateMessageReferences(operation.getReply().getMessages(), originalChannelName, newChannelName);
            }
        }

        // Add all new channels to the spec
        channels.putAll(newChannels);
    }

    /**
     * Process reply channel by creating a normalized version with broker prefix.
     * <p>
     * Reply channels typically use broker destinations (e.g., /topic or /queue).
     * This method creates a new channel with the appropriate broker prefix if needed.
     *
     * @param reply       the reply object containing the channel reference
     * @param channels    the existing channels map
     * @param newChannels the map to store newly created channels
     * @param properties  the WebSocket prefix configuration
     */
    private static void processReplyChannel(Reply reply, Map<String, Channel> channels,
                                           Map<String, Channel> newChannels,
                                           WebSocketPrefixProperties properties) {
        String replyChannelRef = reply.getChannel().getRef();
        if (replyChannelRef == null) {
            return;
        }

        // Extract channel name from reference
        String replyChannelName = extractChannelNameFromRef(replyChannelRef);
        if (replyChannelName == null) {
            return;
        }

        // Check if channel already exists
        if (channels.containsKey(replyChannelName) || newChannels.containsKey(replyChannelName)) {
            return;
        }

        // Find the original channel (might have different name)
        Channel replyChannel = channels.get(replyChannelName);
        if (replyChannel == null || replyChannel.getAddress() == null) {
            // Try to find the channel without prefix
            String baseChannelName = replyChannelName.replaceAll("^_(topic|queue)_", "_");
            replyChannel = channels.get(baseChannelName);

            if (replyChannel == null) {
                log.debug("Reply channel not found: {}", replyChannelName);
                return;
            }
        }

        String replyAddress = replyChannel.getAddress();

        // Check if address already has broker prefix
        if (hasKnownPrefix(replyAddress, properties)) {
            // Address already has prefix, create channel as-is
            if (!newChannels.containsKey(replyChannelName)) {
                newChannels.put(replyChannelName, replyChannel);
                log.debug("Added reply channel: {} with address: {}", replyChannelName, replyAddress);
            }
            return;
        }

        // Add broker prefix to address
        List<String> brokerPrefixes = properties.getBrokerPrefixes();
        if (brokerPrefixes == null || brokerPrefixes.isEmpty()) {
            return;
        }

        String brokerPrefix = brokerPrefixes.get(0);
        String prefixLabel = brokerPrefix.replace("/", ""); // Remove all slashes for label

        // Create new channel with broker prefix
        String newReplyChannelName = createPrefixedChannelName(prefixLabel, replyChannelName);

        if (!newChannels.containsKey(newReplyChannelName) && !channels.containsKey(newReplyChannelName)) {
            Channel newReplyChannel = new Channel();
            newReplyChannel.setAddress(ensurePrefix(replyAddress, brokerPrefix));
            newReplyChannel.setMessages(replyChannel.getMessages());
            newReplyChannel.setBindings(replyChannel.getBindings());

            newChannels.put(newReplyChannelName, newReplyChannel);

            // Update reply channel reference
            reply.getChannel().setRef("#/channels/" + newReplyChannelName);

            log.debug("Created reply channel: {} with address: {}", newReplyChannelName, newReplyChannel.getAddress());
        }
    }

    /**
     * Update message references to point to the new channel name.
     * <p>
     * Replaces references like "#/channels/_chat.send/messages/ChatMessage"
     * with "#/channels/app_chat_send/messages/ChatMessage".
     *
     * @param messages           the list of message references to update
     * @param originalChannelName the original channel name to replace
     * @param newChannelName     the new channel name to use
     */
    private static void updateMessageReferences(List<MessageReference> messages, String originalChannelName, String newChannelName) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (MessageReference messageRef : messages) {
            if (messageRef == null || messageRef.getRef() == null) {
                continue;
            }

            String ref = messageRef.getRef();
            String originalChannelPrefix = "#/channels/" + originalChannelName + "/";

            // Check if this reference points to the original channel
            if (ref.startsWith(originalChannelPrefix)) {
                // Replace the channel name in the reference
                String newRef = ref.replace(originalChannelPrefix, "#/channels/" + newChannelName + "/");
                messageRef.setRef(newRef);
                log.debug("Updated message reference: {} -> {}", ref, newRef);
            }
        }
    }

    /**
     * Normalize a single channel address based on the operation action type.
     * <p>
     * AsyncAPI operations are from the server's perspective:
     * <ul>
     *   <li><b>receive</b>: Server receives from client ({@code @MessageMapping}) → add application destination prefix</li>
     *   <li><b>send</b>: Server sends to client ({@code @SendTo}) → verify broker prefix presence</li>
     * </ul>
     *
     * @param address    the original channel address (may or may not have a prefix)
     * @param action     the operation action type ("send" or "receive" from server's perspective)
     * @param properties the WebSocket prefix configuration
     * @return the normalized address with the appropriate prefix
     */
    private static String normalizeAddress(String address, String action, WebSocketPrefixProperties properties) {
        if (address == null || address.isEmpty()) {
            return address;
        }

        // Check if address already has a known prefix
        if (hasKnownPrefix(address, properties)) {
            return address;
        }

        // For "receive" operations (@MessageMapping), add application destination prefix
        // Client sends to: /app/path → Server receives from this destination
        if ("receive".equalsIgnoreCase(action)) {
            String prefix = properties.getApplicationDestinationPrefix();
            return ensurePrefix(address, prefix);
        }

        // For "send" operations (@SendTo), add default broker prefix if missing
        // Server sends to: /topic/path → Client receives from this destination
        if ("send".equalsIgnoreCase(action)) {
            // Use first broker prefix as default (typically "/topic")
            List<String> brokerPrefixes = properties.getBrokerPrefixes();
            if (brokerPrefixes != null && !brokerPrefixes.isEmpty()) {
                String defaultBrokerPrefix = brokerPrefixes.get(0);
                return ensurePrefix(address, defaultBrokerPrefix);
            }
        }

        return address;
    }

    /**
     * Check if the address already starts with a known prefix (application or broker).
     *
     * @param address    the channel address to check
     * @param properties the WebSocket prefix configuration
     * @return true if the address starts with a known prefix, false otherwise
     */
    private static boolean hasKnownPrefix(String address, WebSocketPrefixProperties properties) {
        // Check application destination prefix
        String appPrefix = properties.getApplicationDestinationPrefix();
        if (appPrefix != null && address.startsWith(appPrefix + "/")) {
            return true;
        }

        // Check broker prefixes
        List<String> brokerPrefixes = properties.getBrokerPrefixes();
        if (brokerPrefixes != null) {
            for (String brokerPrefix : brokerPrefixes) {
                if (brokerPrefix != null && address.startsWith(brokerPrefix + "/")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ensure the address starts with the given prefix.
     * <p>
     * If the address already starts with the prefix, returns it unchanged.
     * Otherwise, prepends the prefix to the address.
     *
     * @param address the channel address
     * @param prefix  the prefix to add (e.g., "/app" or "/topic")
     * @return the address with the prefix
     */
    private static String ensurePrefix(String address, String prefix) {
        if (address == null || prefix == null) {
            return address;
        }

        // Remove leading slash from address if present, to avoid double slashes
        String cleanAddress = address.startsWith("/") ? address.substring(1) : address;

        // Ensure prefix ends without trailing slash
        String cleanPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;

        return cleanPrefix + "/" + cleanAddress;
    }

    /**
     * Extract the channel name from a channel reference.
     * <p>
     * Example: "#/channels/_chat.send" -> "_chat.send"
     *
     * @param ref the channel reference string
     * @return the channel name, or null if extraction fails
     */
    private static String extractChannelNameFromRef(String ref) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }

        String prefix = "#/channels/";
        if (ref.startsWith(prefix)) {
            return ref.substring(prefix.length());
        }

        // Fallback: find last slash
        int lastSlashIndex = ref.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < ref.length() - 1) {
            return ref.substring(lastSlashIndex + 1);
        }

        return ref;
    }

    /**
     * Create a prefixed channel name by combining a prefix label with the original channel name.
     * <p>
     * The original channel name is sanitized by converting dots to underscores.
     * The result starts with an underscore followed by the prefix label.
     * <p>
     * Examples:
     * <ul>
     *   <li>prefixLabel="app", originalName="_chat.send" → "_app_chat_send"</li>
     *   <li>prefixLabel="topic", originalName="_chat.send" → "_topic_chat_send"</li>
     *   <li>prefixLabel="queue", originalName="task.process" → "_queue_task_process"</li>
     * </ul>
     *
     * @param prefixLabel  the prefix label (e.g., "app", "topic", "queue")
     * @param originalName the original channel name to be prefixed
     * @return the new channel name with the prefix (starting with underscore)
     */
    private static String createPrefixedChannelName(String prefixLabel, String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "_" + prefixLabel;
        }

        // Replace dots with underscores for consistency
        String cleanName = originalName.replace('.', '_');

        return "_" + prefixLabel + cleanName;
    }
}