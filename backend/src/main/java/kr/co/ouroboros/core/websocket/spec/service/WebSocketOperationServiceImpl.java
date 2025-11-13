package kr.co.ouroboros.core.websocket.spec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.websocket.common.dto.ChannelReference;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.Reply;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.ui.websocket.spec.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Implementation of {@link WebSocketOperationService}.
 * <p>
 * Manages operations in the AsyncAPI operations section of ourowebsocket.yml.
 * Automatically creates channels and generates all receive × reply combinations.
 * Uses {@link WebSocketYamlParser} for all YAML file operations.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketOperationServiceImpl implements WebSocketOperationService {

    private final WebSocketYamlParser yamlParser;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public List<OperationResponse> createOperations(CreateOperationRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            Map<String, Object> asyncApiDoc = yamlParser.readOrCreateDocument();

            List<OperationResponse> createdOperations = new ArrayList<>();

            // Validate and ensure server exists
            if (request.getProtocol() == null || request.getProtocol().isEmpty()) {
                throw new IllegalArgumentException("Protocol must be provided (ws or wss)");
            }
            if (!"ws".equals(request.getProtocol()) && !"wss".equals(request.getProtocol())) {
                throw new IllegalArgumentException("Protocol must be either 'ws' or 'wss'");
            }
            if (request.getPathname() == null || request.getPathname().isEmpty()) {
                throw new IllegalArgumentException("Pathname must be provided");
            }

            // Ensure server entry point exists
            ensureServerExists(asyncApiDoc, request.getProtocol(), request.getPathname());

            // At least one of receives or replies must be provided
            boolean hasReceives = request.getReceives() != null && !request.getReceives().isEmpty();
            boolean hasReplies = request.getReplies() != null && !request.getReplies().isEmpty();

            if (!hasReceives && !hasReplies) {
                throw new IllegalArgumentException("At least one receive or reply channel must be provided");
            }

            // Reply-only operations
            if (!hasReceives && hasReplies) {
                for (ChannelMessageInfo reply : request.getReplies()) {
                    String replyChannelName = ensureChannelExists(asyncApiDoc, reply);

                    // Generate unique operation name
                    String operationName = generateOperationName(null, replyChannelName, asyncApiDoc);

                    // Build operation definition (send-only)
                    Map<String, Object> operationDefinition = buildOperationDefinition(
                            null, null,
                            replyChannelName, reply.getMessages(),
                            request.getPathname()
                    );

                    // Add operation to document
                    yamlParser.putOperation(asyncApiDoc, operationName, operationDefinition);

                    // Convert to response
                    Operation operation = convertMapToOperation(operationDefinition);

                    createdOperations.add(OperationResponse.builder()
                            .operationName(operationName)
                            .operation(operation)
                            .build());

                    log.debug("Created send-only operation: {} (reply: {})",
                            operationName, replyChannelName);
                }
            } else {
                // Generate all combinations of receives × replies (or just receives if no replies)
                for (ChannelMessageInfo receive : request.getReceives()) {
                    String receiveChannelName = ensureChannelExists(asyncApiDoc, receive);

                    if (!hasReplies) {
                        // No reply - create receive-only operation
                        String operationName = generateOperationName(receiveChannelName, null, asyncApiDoc);

                        // Build operation definition without reply
                        Map<String, Object> operationDefinition = buildOperationDefinition(
                                receiveChannelName, receive.getMessages(),
                                null, null,
                                request.getPathname()
                        );

                        // Add operation to document
                        yamlParser.putOperation(asyncApiDoc, operationName, operationDefinition);

                        // Convert to response
                        Operation operation = convertMapToOperation(operationDefinition);

                        createdOperations.add(OperationResponse.builder()
                                .operationName(operationName)
                                .operation(operation)
                                .build());

                        log.debug("Created receive-only operation: {} (receive: {})",
                                operationName, receiveChannelName);
                    } else {
                        // With replies - create all combinations
                        for (ChannelMessageInfo reply : request.getReplies()) {
                            String replyChannelName = ensureChannelExists(asyncApiDoc, reply);

                            // Generate unique operation name
                            String operationName = generateOperationName(receiveChannelName, replyChannelName, asyncApiDoc);

                            // Build operation definition
                            Map<String, Object> operationDefinition = buildOperationDefinition(
                                    receiveChannelName, receive.getMessages(),
                                    replyChannelName, reply.getMessages(),
                                    request.getPathname()
                            );

                            // Add operation to document
                            yamlParser.putOperation(asyncApiDoc, operationName, operationDefinition);

                            // Convert to response
                            Operation operation = convertMapToOperation(operationDefinition);

                            createdOperations.add(OperationResponse.builder()
                                    .operationName(operationName)
                                    .operation(operation)
                                    .build());

                            log.debug("Created operation: {} (receive: {}, reply: {})",
                                    operationName, receiveChannelName, replyChannelName);
                        }
                    }
                }
            }

            // Write to file
            yamlParser.writeDocument(asyncApiDoc);

            log.info("Created {} WebSocket operations", createdOperations.size());

            return createdOperations;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<OperationResponse> getAllOperations() throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                return new ArrayList<>();
            }

            Map<String, Object> asyncApiDoc = yamlParser.readDocument();
            Map<String, Object> operations = yamlParser.getOperations(asyncApiDoc);

            if (operations == null || operations.isEmpty()) {
                return new ArrayList<>();
            }

            List<OperationResponse> responses = new ArrayList<>();
            for (Map.Entry<String, Object> entry : operations.entrySet()) {
                String operationName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> operationDefinition = (Map<String, Object>) entry.getValue();

                Operation operation = convertMapToOperation(operationDefinition);

                // Calculate tag based on operation type
                String tag = calculateOperationTag(operation);

                responses.add(OperationResponse.builder()
                        .operationName(operationName)
                        .operation(operation)
                        .tag(tag)
                        .build());
            }

            return responses;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public OperationResponse getOperation(String id) throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No operations found. The specification file does not exist.");
            }

            Map<String, Object> asyncApiDoc = yamlParser.readDocument();
            Map.Entry<String, Map<String, Object>> operationEntry = yamlParser.findOperationById(asyncApiDoc, id);

            if (operationEntry == null) {
                throw new IllegalArgumentException("Operation with id '" + id + "' not found");
            }

            String operationName = operationEntry.getKey();
            Map<String, Object> operationDefinition = operationEntry.getValue();

            Operation operation = convertMapToOperation(operationDefinition);

            return OperationResponse.builder()
                    .operationName(operationName)
                    .operation(operation)
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public OperationResponse updateOperation(String id, UpdateOperationRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No operations found. The specification file does not exist.");
            }

            Map<String, Object> asyncApiDoc = yamlParser.readDocument();
            Map.Entry<String, Map<String, Object>> operationEntry = yamlParser.findOperationById(asyncApiDoc, id);

            if (operationEntry == null) {
                throw new IllegalArgumentException("Operation with id '" + id + "' not found");
            }

            String operationName = operationEntry.getKey();
            Map<String, Object> existingOperation = operationEntry.getValue();

            // Update server entry point if protocol/pathname provided
            String updatedPathname = null;
            if (request.getProtocol() != null || request.getPathname() != null) {
                String protocol = request.getProtocol() != null ? request.getProtocol() :
                        extractProtocolFromExistingServer(asyncApiDoc);
                String pathname = request.getPathname() != null ? request.getPathname() :
                        extractPathnameFromExistingServer(asyncApiDoc);

                if (protocol != null && pathname != null) {
                    ensureServerExists(asyncApiDoc, protocol, pathname);
                    updatedPathname = pathname;
                }
            }

            // Extract old channel references before update
            Set<String> oldChannelRefs = extractChannelReferences(existingOperation);

            // Create a copy of existing operation for comparison
            Map<String, Object> updatedOperation = new LinkedHashMap<>(existingOperation);

            // Update x-ouroboros-entrypoint if pathname was provided
            if (updatedPathname != null) {
                updatedOperation.put("x-ouroboros-entrypoint", updatedPathname);
            }

            // Determine action automatically based on provided fields
            String determinedAction = determineAction(request.getReceive(), request.getReply());
            if (determinedAction != null) {
                updatedOperation.put("action", determinedAction);
            }
            
            // Update receive channel (main channel)
            if (request.getReceive() != null) {
                String channelName = ensureChannelExists(asyncApiDoc, request.getReceive());
                Map<String, String> channelRef = new LinkedHashMap<>();
                channelRef.put("$ref", "#/channels/" + channelName);
                updatedOperation.put("channel", channelRef);
                
                // Update messages if provided - reference channel's messages, not components directly
                if (request.getReceive().getMessages() != null && !request.getReceive().getMessages().isEmpty()) {
                    List<Map<String, String>> messageRefs = request.getReceive().getMessages().stream()
                            .map(msgName -> {
                                Map<String, String> ref = new LinkedHashMap<>();
                                ref.put("$ref", "#/channels/" + channelName + "/messages/" + msgName);
                                return ref;
                            })
                            .collect(Collectors.toList());
                    updatedOperation.put("messages", messageRefs);
                }
            }
            
            // Update reply channel
            if (request.getReply() != null) {
                // Check if this is a send-only operation (action is "send", receive is null, reply is non-null)
                boolean isSendOnly = ("send".equals(determinedAction) || "send".equals(updatedOperation.get("action")))
                        && request.getReceive() == null
                        && request.getReply() != null;
                
                if (isSendOnly) {
                    // For send-only operations, set reply channel as top-level channel
                    String replyChannelName = ensureChannelExists(asyncApiDoc, request.getReply());
                    updatedOperation.put("channel", Map.of("$ref", "#/channels/" + replyChannelName));
                    
                    // Set top-level messages to reply's messages refs - reference channel's messages, not components directly
                    if (request.getReply().getMessages() != null && !request.getReply().getMessages().isEmpty()) {
                        List<Map<String, String>> messageRefs = request.getReply().getMessages().stream()
                                .map(msgName -> {
                                    Map<String, String> ref = new LinkedHashMap<>();
                                    ref.put("$ref", "#/channels/" + replyChannelName + "/messages/" + msgName);
                                    return ref;
                                })
                                .collect(Collectors.toList());
                        updatedOperation.put("messages", messageRefs);
                    }
                    
                    // Remove any "reply" key from updatedOperation (do not create a nested reply block)
                    updatedOperation.remove("reply");
                } else {
                    // Existing behavior for reply-containing updates (receive with reply)
                    Map<String, Object> reply = new LinkedHashMap<>();
                    
                    String replyChannelName = ensureChannelExists(asyncApiDoc, request.getReply());
                    Map<String, String> replyChannelRef = new LinkedHashMap<>();
                    replyChannelRef.put("$ref", "#/channels/" + replyChannelName);
                    reply.put("channel", replyChannelRef);
                    
                    // Update reply messages if provided - reference channel's messages, not components directly
                    if (request.getReply().getMessages() != null && !request.getReply().getMessages().isEmpty()) {
                        List<Map<String, String>> messageRefs = request.getReply().getMessages().stream()
                                .map(msgName -> {
                                    Map<String, String> ref = new LinkedHashMap<>();
                                    ref.put("$ref", "#/channels/" + replyChannelName + "/messages/" + msgName);
                                    return ref;
                                })
                                .collect(Collectors.toList());
                        reply.put("messages", messageRefs);
                    }
                    
                    updatedOperation.put("reply", reply);
                }
            }

            // Extract new channel references after update
            Set<String> newChannelRefs = extractChannelReferences(updatedOperation);

            // Find channels that were removed (in old but not in new)
            Set<String> removedChannels = new HashSet<>(oldChannelRefs);
            removedChannels.removeAll(newChannelRefs);

            // Apply the update to the actual document
            existingOperation.clear();
            existingOperation.putAll(updatedOperation);

            // Check and delete channels that are no longer referenced
            for (String channelName : removedChannels) {
                if (!isChannelUsedByOtherOperations(asyncApiDoc, channelName)) {
                    // Channel is not used by any other operation, delete it
                    boolean channelRemoved = removeChannel(asyncApiDoc, channelName);
                    if (channelRemoved) {
                        log.info("Deleted unused channel: {} (no longer referenced after operation update)", channelName);
                    }
                }
            }

            // Write to file
            yamlParser.writeDocument(asyncApiDoc);

            log.info("Updated WebSocket operation: {}", operationName);

            Operation operation = convertMapToOperation(existingOperation);

            return OperationResponse.builder()
                    .operationName(operationName)
                    .operation(operation)
                    .build();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteOperation(String id) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No operations found. The specification file does not exist.");
            }

            Map<String, Object> asyncApiDoc = yamlParser.readDocument();

            // Find operation by id before deletion
            Map.Entry<String, Map<String, Object>> operationEntry = yamlParser.findOperationById(asyncApiDoc, id);
            if (operationEntry == null) {
                throw new IllegalArgumentException("Operation with id '" + id + "' not found");
            }

            String operationName = operationEntry.getKey();
            Map<String, Object> operationToDelete = operationEntry.getValue();

            // Extract channel names from operation
            Set<String> referencedChannels = extractChannelReferences(operationToDelete);

            // Delete the operation
            boolean removed = yamlParser.removeOperation(asyncApiDoc, operationName);
            if (!removed) {
                throw new IllegalArgumentException("Operation with id '" + id + "' not found");
            }

            // Check if referenced channels are used by other operations
            for (String channelName : referencedChannels) {
                if (!isChannelUsedByOtherOperations(asyncApiDoc, channelName)) {
                    // Channel is not used by any other operation, delete it
                    boolean channelRemoved = removeChannel(asyncApiDoc, channelName);
                    if (channelRemoved) {
                        log.info("Deleted unused channel: {} (no longer referenced by any operation)", channelName);
                    }
                }
            }

            // Write to file
            yamlParser.writeDocument(asyncApiDoc);

            log.info("Deleted WebSocket operation: {}", operationName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Ensures a server entry point exists, creating it if necessary.
     * Server name is generated from protocol and pathname (e.g., "ws-ws", "wss-stomp_v1").
     * Host is automatically set to "localhost:8080".
     * If a server with the same protocol and pathname already exists, reuses it.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param protocol WebSocket protocol (ws or wss)
     * @param pathname WebSocket pathname (entry point)
     */
    private void ensureServerExists(Map<String, Object> asyncApiDoc, String protocol, String pathname) {
        // Generate server name from protocol + pathname
        String serverName = generateServerName(protocol, pathname);

        // Check if server already exists
        Map<String, Object> existingServer = yamlParser.getServer(asyncApiDoc, serverName);
        if (existingServer != null) {
            // Server already exists, no action needed
            log.debug("Reusing existing server: {} (protocol: {}, pathname: {})", serverName, protocol, pathname);
            return;
        }

        // Create new server
        Map<String, Object> serverDefinition = new LinkedHashMap<>();
        serverDefinition.put("host", "localhost:8080");
        serverDefinition.put("pathname", pathname);
        serverDefinition.put("protocol", protocol);
        serverDefinition.put("description", protocol.toUpperCase() + " WebSocket server at " + pathname);

        yamlParser.putServer(asyncApiDoc, serverName, serverDefinition);
        log.debug("Auto-created server: {} (protocol: {}, pathname: {})", serverName, protocol, pathname);
    }

    /**
     * Generates a server name from protocol and pathname.
     * Pattern: protocol-pathname_sanitized (e.g., "ws-ws", "wss-stomp_v1")
     *
     * @param protocol WebSocket protocol (ws or wss)
     * @param pathname WebSocket pathname
     * @return sanitized server name
     */
    private String generateServerName(String protocol, String pathname) {
        // Sanitize pathname: remove leading slash, replace remaining slashes with underscores
        String sanitizedPathname = pathname.startsWith("/") ? pathname.substring(1) : pathname;
        sanitizedPathname = sanitizedPathname.replace("/", "_");

        return protocol + "-" + sanitizedPathname;
    }

    /**
     * Ensures a channel exists, creating it if necessary.
     * Returns the channel name (either existing or newly created).
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelInfo channel information (address or channelRef + messages)
     * @return channel name
     */
    private String ensureChannelExists(Map<String, Object> asyncApiDoc, ChannelMessageInfo channelInfo) {
        String channelName;
        String address;

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
            address = channelInfo.getAddress();
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
    private void updateChannelMessages(Map<String, Object> asyncApiDoc, String channelName, List<String> messageNames) {
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
     * Pattern: "/chat.send" -> "_chat.send", "/topic/rooms" -> "_topic_rooms"
     *
     * @param address channel address
     * @return channel name
     */
    private String addressToChannelName(String address) {
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
     * Builds an operation definition from receive and reply channel information.
     *
     * @param receiveChannelName receive channel name (can be null for send-only)
     * @param receiveMessages receive message names (can be null for send-only)
     * @param replyChannelName reply channel name (can be null for receive-only)
     * @param replyMessages reply message names (can be null for receive-only)
     * @param pathname WebSocket pathname (entry point) for x-ouroboros-entrypoint
     * @return operation definition map
     */
    private Map<String, Object> buildOperationDefinition(
            String receiveChannelName, List<String> receiveMessages,
            String replyChannelName, List<String> replyMessages,
            String pathname) {

        Map<String, Object> operation = new LinkedHashMap<>();

        // Determine action based on provided channels
        if (receiveChannelName != null) {
            operation.put("action", "receive");
        } else if (replyChannelName != null) {
            operation.put("action", "send");
        }

        // Channel reference (main channel for receive, reply channel for send-only)
        if (receiveChannelName != null) {
            Map<String, String> channelRef = new LinkedHashMap<>();
            channelRef.put("$ref", "#/channels/" + receiveChannelName);
            operation.put("channel", channelRef);

            // Messages - reference channel's messages, not components directly
            if (receiveMessages != null && !receiveMessages.isEmpty()) {
                List<Map<String, String>> messageRefs = receiveMessages.stream()
                        .map(msgName -> {
                            Map<String, String> ref = new LinkedHashMap<>();
                            ref.put("$ref", "#/channels/" + receiveChannelName + "/messages/" + msgName);
                            return ref;
                        })
                        .collect(Collectors.toList());
                operation.put("messages", messageRefs);
            }
        } else if (replyChannelName != null) {
            // For send-only, the main channel is the reply channel
            Map<String, String> channelRef = new LinkedHashMap<>();
            channelRef.put("$ref", "#/channels/" + replyChannelName);
            operation.put("channel", channelRef);

            // Messages - reference channel's messages, not components directly
            if (replyMessages != null && !replyMessages.isEmpty()) {
                List<Map<String, String>> messageRefs = replyMessages.stream()
                        .map(msgName -> {
                            Map<String, String> ref = new LinkedHashMap<>();
                            ref.put("$ref", "#/channels/" + replyChannelName + "/messages/" + msgName);
                            return ref;
                        })
                        .collect(Collectors.toList());
                operation.put("messages", messageRefs);
            }
        }

        // Reply configuration (only for receive operations with reply)
        if (receiveChannelName != null && replyChannelName != null) {
            Map<String, Object> reply = new LinkedHashMap<>();
            Map<String, String> replyChannelRef = new LinkedHashMap<>();
            replyChannelRef.put("$ref", "#/channels/" + replyChannelName);
            reply.put("channel", replyChannelRef);

            // Reply messages - reference channel's messages, not components directly
            if (replyMessages != null && !replyMessages.isEmpty()) {
                List<Map<String, String>> replyMessageRefs = replyMessages.stream()
                        .map(msgName -> {
                            Map<String, String> ref = new LinkedHashMap<>();
                            ref.put("$ref", "#/channels/" + replyChannelName + "/messages/" + msgName);
                            return ref;
                        })
                        .collect(Collectors.toList());
                reply.put("messages", replyMessageRefs);
            }
            operation.put("reply", reply);
        }

        // Generate UUID for x-ouroboros-id
        operation.put("x-ouroboros-id", UUID.randomUUID().toString());

        // Store entry point (pathname) for this operation
        operation.put("x-ouroboros-entrypoint", pathname);

        // Add STOMP bindings
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("stomp", new LinkedHashMap<>());
        operation.put("bindings", bindings);

        // Add Ouroboros custom fields at top level (not in tags)
        operation.put("x-ouroboros-diff", "none");
        operation.put("x-ouroboros-progress", "none");

        return operation;
    }

    /**
     * Generates a unique operation name from receive and reply channel names.
     *
     * @param receiveChannelName receive channel name (can be null for send-only)
     * @param replyChannelName reply channel name (can be null for receive-only)
     * @param asyncApiDoc AsyncAPI document
     * @return unique operation name
     */
    private String generateOperationName(String receiveChannelName, String replyChannelName, Map<String, Object> asyncApiDoc) {
        String baseName;
        if (receiveChannelName == null && replyChannelName != null) {
            // Send-only operation
            baseName = replyChannelName + "_send";
        } else if (receiveChannelName != null && replyChannelName == null) {
            // Receive-only operation
            baseName = receiveChannelName + "_receive";
        } else if (receiveChannelName != null && replyChannelName != null) {
            // Receive-reply operation
            baseName = receiveChannelName + "_to_" + replyChannelName;
        } else {
            // Should never happen
            baseName = "unnamed_operation";
        }

        String operationName = baseName;
        int counter = 1;

        // Ensure uniqueness
        while (yamlParser.operationExists(asyncApiDoc, operationName)) {
            operationName = baseName + "_" + counter;
            counter++;
        }

        return operationName;
    }

    /**
     * Converts a Map to an Operation DTO.
     *
     * @param operationMap operation definition map
     * @return Operation DTO
     */
    @SuppressWarnings("unchecked")
    private Operation convertMapToOperation(Map<String, Object> operationMap) {
        // Preprocess: convert $ref to ref for Jackson deserialization
        Map<String, Object> processedMap = convertDollarRefToRef(operationMap);
        
        try {
            return objectMapper.convertValue(processedMap, Operation.class);
        } catch (Exception e) {
            log.error("Failed to convert operation map to Operation DTO", e);
            // Fallback: manual conversion (use processedMap which has ref instead of $ref)
            Operation operation = new Operation();
            operation.setAction((String) processedMap.get("action"));
            operation.setXOuroborosId((String) processedMap.get("x-ouroboros-id"));
            operation.setXOuroborosEntrypoint((String) processedMap.get("x-ouroboros-entrypoint"));
            operation.setXOuroborosDiff((String) processedMap.get("x-ouroboros-diff"));
            operation.setXOuroborosProgress((String) processedMap.get("x-ouroboros-progress"));

            // Channel reference (already converted to ref by convertDollarRefToRef)
            Object channelObj = processedMap.get("channel");
            if (channelObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> channelMap = (Map<String, String>) channelObj;
                ChannelReference channelRef = new ChannelReference();
                String ref = channelMap.get("ref");
                channelRef.setRef(ref);
                operation.setChannel(channelRef);
            }

            // Messages (already converted to ref by convertDollarRefToRef)
            Object messagesObj = processedMap.get("messages");
            if (messagesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messagesList = (List<Map<String, String>>) messagesObj;
                List<MessageReference> messageRefs = messagesList.stream()
                        .map(msgMap -> {
                            MessageReference msgRef = new MessageReference();
                            String ref = msgMap.get("ref");
                            msgRef.setRef(ref);
                            return msgRef;
                        })
                        .collect(Collectors.toList());
                operation.setMessages(messageRefs);
            }

            // Reply (already converted to ref by convertDollarRefToRef)
            Object replyObj = processedMap.get("reply");
            if (replyObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> replyMap = (Map<String, Object>) replyObj;
                Reply reply = new Reply();

                Object replyChannelObj = replyMap.get("channel");
                if (replyChannelObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> replyChannelMap = (Map<String, String>) replyChannelObj;
                    ChannelReference replyChannelRef = new ChannelReference();
                    String ref = replyChannelMap.get("ref");
                    replyChannelRef.setRef(ref);
                    reply.setChannel(replyChannelRef);
                }

                Object replyMessagesObj = replyMap.get("messages");
                if (replyMessagesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> replyMessagesList = (List<Map<String, String>>) replyMessagesObj;
                    List<MessageReference> replyMessageRefs = replyMessagesList.stream()
                            .map(msgMap -> {
                                MessageReference msgRef = new MessageReference();
                                String ref = msgMap.get("ref");
                                msgRef.setRef(ref);
                                return msgRef;
                            })
                            .collect(Collectors.toList());
                    reply.setMessages(replyMessageRefs);
                }

                operation.setReply(reply);
            }

            return operation;
        }
    }

    /**
     * Recursively converts $ref fields to ref fields in a Map for Jackson deserialization.
     * <p>
     * YAML uses $ref (AsyncAPI standard), but JSON API uses ref.
     * This method converts $ref to ref so that Jackson can properly deserialize to DTOs.
     *
     * @param map the map to process
     * @return new map with $ref converted to ref (deep copy)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertDollarRefToRef(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Convert $ref key to ref
            if ("$ref".equals(key) && value instanceof String) {
                result.put("ref", value);
            } else if (value instanceof Map) {
                // Recursively process nested maps
                result.put(key, convertDollarRefToRef((Map<String, Object>) value));
            } else if (value instanceof List) {
                // Recursively process lists
                result.put(key, convertDollarRefToRefInList((List<Object>) value));
            } else {
                // Keep other fields as-is
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Recursively converts $ref fields to ref fields in a List.
     *
     * @param list the list to process
     * @return new list with $ref converted to ref (deep copy)
     */
    @SuppressWarnings("unchecked")
    private List<Object> convertDollarRefToRefInList(List<Object> list) {
        if (list == null) {
            return null;
        }
        
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(convertDollarRefToRef((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(convertDollarRefToRefInList((List<Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Extracts channel names referenced by an operation.
     * Includes both the main channel and reply channel.
     *
     * @param operation operation definition map
     * @return set of channel names referenced by the operation
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractChannelReferences(Map<String, Object> operation) {
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
     * Checks if a channel is used by any operation (excluding the operation being deleted).
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name to check
     * @return true if channel is used by any operation, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean isChannelUsedByOtherOperations(Map<String, Object> asyncApiDoc, String channelName) {
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
    private boolean removeChannel(Map<String, Object> asyncApiDoc, String channelName) {
        return yamlParser.removeChannel(asyncApiDoc, channelName);
    }

    /**
     * Determines the action based on receive and reply configuration.
     * <p>
     * Rules:
     * <ul>
     *   <li>If only reply is provided: "send"</li>
     *   <li>If receive is provided (with or without reply): "receive"</li>
     *   <li>If neither is provided: null (no change)</li>
     * </ul>
     *
     * @param receive receive channel configuration
     * @param reply reply channel configuration
     * @return determined action, or null if cannot be determined
     */
    private String determineAction(ChannelMessageInfo receive, ChannelMessageInfo reply) {
        if (receive != null) {
            // If receive is provided, action is always "receive"
            return "receive";
        } else if (reply != null) {
            // If only reply is provided, action is "send"
            return "send";
        }
        // Neither provided, no change
        return null;
    }

    /**
     * Extracts protocol from existing server in the document.
     * <p>
     * Looks for the first server and returns its protocol.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return protocol (ws or wss), or null if not found
     */
    @SuppressWarnings("unchecked")
    private String extractProtocolFromExistingServer(Map<String, Object> asyncApiDoc) {
        Map<String, Object> servers = yamlParser.getServers(asyncApiDoc);
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        
        // Get first server
        Map<String, Object> firstServer = servers.values().stream()
                .filter(server -> server instanceof Map)
                .map(server -> (Map<String, Object>) server)
                .findFirst()
                .orElse(null);
        
        if (firstServer != null) {
            return (String) firstServer.get("protocol");
        }
        
        return null;
    }

    /**
     * Extracts pathname from existing server in the document.
     * <p>
     * Looks for the first server and returns its pathname.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return pathname, or null if not found
     */
    @SuppressWarnings("unchecked")
    private String extractPathnameFromExistingServer(Map<String, Object> asyncApiDoc) {
        Map<String, Object> servers = yamlParser.getServers(asyncApiDoc);
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        
        // Get first server
        Map<String, Object> firstServer = servers.values().stream()
                .filter(server -> server instanceof Map)
                .map(server -> (Map<String, Object>) server)
                .findFirst()
                .orElse(null);
        
        if (firstServer != null) {
            return (String) firstServer.get("pathname");
        }
        
        return null;
    }

    /**
     * Calculates the operation tag based on operation type.
     * <p>
     * Tag values:
     * <ul>
     *   <li>"sendto": action is "send" (send-only operation)</li>
     *   <li>"receive": action is "receive" and no reply exists (receive-only operation)</li>
     *   <li>"duplicate": action is "receive" and reply exists (receive with reply operation)</li>
     * </ul>
     *
     * @param operation the operation to calculate tag for
     * @return operation tag
     */
    private String calculateOperationTag(Operation operation) {
        if (operation == null || operation.getAction() == null) {
            return null;
        }

        String action = operation.getAction();

        // Send-only operation
        if ("send".equals(action)) {
            return "sendto";
        }

        // Receive operation
        if ("receive".equals(action)) {
            // Check if reply exists
            if (operation.getReply() != null) {
                return "duplicate";
            } else {
                return "receive";
            }
        }

        return null;
    }

    @Override
    public ImportYamlResponse importYaml(String yamlContent) throws Exception {
        lock.writeLock().lock();
        try {
            log.info("========================================");
            log.info("📥 Starting AsyncAPI YAML import...");

            // Step 1: Parse imported YAML (validation already done in controller)
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> importedDoc = (Map<String, Object>) yaml.load(yamlContent);

            // Step 2: Read existing document or create new one
            Map<String, Object> existingDoc = yamlParser.readOrCreateDocument();

            // Step 3: Prepare renamed tracking
            List<RenamedItem> renamedList = new ArrayList<>();
            Map<String, String> schemaRenameMap = new HashMap<>();  // old name -> new name
            Map<String, String> messageRenameMap = new HashMap<>(); // old name -> new name

            // Step 4: Process schemas first
            int importedSchemas = importSchemas(importedDoc, existingDoc, renamedList, schemaRenameMap);

            // Step 5: Process messages (with schema reference updates)
            int importedMessages = importMessages(importedDoc, existingDoc, renamedList, messageRenameMap, schemaRenameMap);

            // Step 5.5: Process servers
            int importedServers = importServers(importedDoc, existingDoc, renamedList);

            // Step 5.6: Extract entrypoint from imported servers for operations
            String entrypoint = extractFirstServerPathname(importedDoc);

            // Step 6: Process channels (with message reference updates)
            int importedChannels = importChannels(importedDoc, existingDoc, renamedList, messageRenameMap);

            // Step 7: Process operations (with message reference updates)
            int importedOperations = importOperations(importedDoc, existingDoc, renamedList, messageRenameMap, entrypoint);

            // Step 7.5: Update schema references in messages (after all messages are imported)
            updateSchemaReferencesInMessages(existingDoc, schemaRenameMap);

            // Step 8: Save to file
            yamlParser.writeDocument(existingDoc);

            // Step 9: Build response
            String summary = String.format("Successfully imported %d channels, %d operations, %d schemas, %d messages%s",
                    importedChannels, importedOperations, importedSchemas, importedMessages,
                    !renamedList.isEmpty() ? ", renamed " + renamedList.size() + " items due to duplicates" : "");

            log.info("========================================");
            log.info("✅ AsyncAPI YAML Import Completed");
            log.info("   📊 Servers imported: {}", importedServers);
            log.info("   📊 Channels imported: {}", importedChannels);
            log.info("   📊 Operations imported: {}", importedOperations);
            log.info("   📊 Schemas imported: {}", importedSchemas);
            log.info("   📊 Messages imported: {}", importedMessages);
            log.info("   📊 Items renamed: {}", renamedList.size());
            log.info("========================================");

            return ImportYamlResponse.builder()
                    .importedChannels(importedChannels)
                    .importedOperations(importedOperations)
                    .renamed(renamedList.size())
                    .summary(summary)
                    .renamedList(renamedList)
                    .build();

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Imports schemas from imported document into existing document.
     * Handles duplicate schema names by auto-renaming with "-import" suffix.
     */
    @SuppressWarnings("unchecked")
    private int importSchemas(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                              List<RenamedItem> renamedList, Map<String, String> schemaRenameMap) {
        int count = 0;

        Map<String, Object> existingComponents = yamlParser.getOrCreateComponents(existingDoc);
        Map<String, Object> existingSchemas = yamlParser.getOrCreateSchemas(existingComponents);

        Map<String, Object> importedComponents = (Map<String, Object>) importedDoc.get("components");
        if (importedComponents == null) {
            return 0;
        }

        Map<String, Object> importedSchemas = (Map<String, Object>) importedComponents.get("schemas");
        if (importedSchemas == null || importedSchemas.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedSchemas.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            // Check for duplicate and rename if necessary
            if (existingSchemas.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingSchemas.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("schema")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                schemaRenameMap.put(originalName, finalName);
                log.info("🔄 Schema '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            existingSchemas.put(finalName, entry.getValue());
            count++;
        }

        return count;
    }

    /**
     * Imports messages from imported document into existing document.
     * Handles duplicate message names by auto-renaming with "-import" suffix.
     * Updates schema references in message payloads according to schema rename map.
     */
    @SuppressWarnings("unchecked")
    private int importMessages(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                               List<RenamedItem> renamedList, Map<String, String> messageRenameMap,
                               Map<String, String> schemaRenameMap) {
        int count = 0;

        Map<String, Object> existingComponents = yamlParser.getOrCreateComponents(existingDoc);
        Map<String, Object> existingMessages = yamlParser.getOrCreateMessages(existingComponents);

        Map<String, Object> importedComponents = (Map<String, Object>) importedDoc.get("components");
        if (importedComponents == null) {
            return 0;
        }

        Map<String, Object> importedMessages = (Map<String, Object>) importedComponents.get("messages");
        if (importedMessages == null || importedMessages.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedMessages.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingMessages.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingMessages.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("message")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                messageRenameMap.put(originalName, finalName);
                log.info("🔄 Message '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            // Update message references in the message itself (for payload schema refs)
            Map<String, Object> message = (Map<String, Object>) entry.getValue();
            updateSchemaReferences(message, schemaRenameMap);

            existingMessages.put(finalName, message);
            count++;
        }

        return count;
    }

    /**
     * Imports channels from imported document into existing document.
     * Handles duplicate channel names by auto-renaming with "-import" suffix.
     * Updates message references according to message rename map.
     */
    @SuppressWarnings("unchecked")
    private int importChannels(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                               List<RenamedItem> renamedList, Map<String, String> messageRenameMap) {
        int count = 0;

        Map<String, Object> existingChannels = yamlParser.getOrCreateChannels(existingDoc);

        Map<String, Object> importedChannels = (Map<String, Object>) importedDoc.get("channels");
        if (importedChannels == null || importedChannels.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedChannels.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingChannels.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingChannels.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("channel")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                log.info("🔄 Channel '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            // Enrich channel with Ouroboros fields
            Map<String, Object> channel = (Map<String, Object>) entry.getValue();
            enrichChannelWithOuroborosFields(channel);

            // Update message references in channel messages
            updateMessageReferencesInChannel(channel, messageRenameMap);

            existingChannels.put(finalName, channel);
            count++;
        }

        return count;
    }

    /**
     * Imports servers from imported document into existing document.
     * Handles duplicate server names by auto-renaming with "-import" suffix.
     *
     * @param importedDoc the imported AsyncAPI document
     * @param existingDoc the existing document to merge into
     * @param renamedList list to track renamed items
     * @return number of servers imported
     */
    @SuppressWarnings("unchecked")
    private int importServers(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                              List<RenamedItem> renamedList) {
        int count = 0;

        // Get or create servers section
        Map<String, Object> existingServers = (Map<String, Object>) existingDoc.get("servers");
        if (existingServers == null) {
            existingServers = new LinkedHashMap<>();
            existingDoc.put("servers", existingServers);
        }

        Map<String, Object> importedServers = (Map<String, Object>) importedDoc.get("servers");
        if (importedServers == null || importedServers.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedServers.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingServers.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingServers.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("server")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                log.info("🔄 Server '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            existingServers.put(finalName, entry.getValue());
            count++;
        }

        return count;
    }

    /**
     * Imports operations from imported document into existing document.
     * Handles duplicate operation names by auto-renaming with "-import" suffix.
     * Updates message references according to message rename map.
     */
    @SuppressWarnings("unchecked")
    private int importOperations(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                                 List<RenamedItem> renamedList, Map<String, String> messageRenameMap, String entrypoint) {
        int count = 0;

        Map<String, Object> existingOperations = yamlParser.getOrCreateOperations(existingDoc);

        Map<String, Object> importedOperations = (Map<String, Object>) importedDoc.get("operations");
        if (importedOperations == null || importedOperations.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedOperations.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingOperations.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingOperations.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                Map<String, Object> operation = (Map<String, Object>) entry.getValue();
                String action = (String) operation.get("action");

                renamedList.add(RenamedItem.builder()
                        .type("operation")
                        .original(originalName)
                        .renamed(finalName)
                        .action(action)
                        .build());

                log.info("🔄 Operation '{}' ({}) renamed to '{}' due to duplicate",
                        originalName, action, finalName);
            }

            // Enrich operation with Ouroboros fields
            Map<String, Object> operation = (Map<String, Object>) entry.getValue();
            enrichOperationWithOuroborosFields(operation, entrypoint);

            // Update message references in operation
            updateMessageReferencesInOperation(operation, messageRenameMap);

            existingOperations.put(finalName, operation);
            count++;
        }

        return count;
    }

    /**
     * Updates message references in a channel's messages section.
     * When a message is renamed, updates the $ref in channel.messages.
     *
     * @param channel channel definition
     * @param messageRenameMap map of old message names to new names
     */
    @SuppressWarnings("unchecked")
    private void updateMessageReferencesInChannel(Map<String, Object> channel, Map<String, String> messageRenameMap) {
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
     * When a message is renamed, updates the $ref in operation.messages and operation.reply.messages.
     *
     * @param operation operation definition
     * @param messageRenameMap map of old message names to new names
     */
    @SuppressWarnings("unchecked")
    private void updateMessageReferencesInOperation(Map<String, Object> operation, Map<String, String> messageRenameMap) {
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
     * This updates message payload schema references.
     *
     * @param existingDoc the existing AsyncAPI document
     * @param schemaRenameMap map of old schema names to new names
     */
    @SuppressWarnings("unchecked")
    private void updateSchemaReferencesInMessages(Map<String, Object> existingDoc, Map<String, String> schemaRenameMap) {
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
                updateSchemaReferences(messageObj, schemaRenameMap);
            }
        }
    }

    /**
     * Recursively updates all $ref references according to schema rename map.
     * Similar to REST API import logic.
     *
     * @param obj the object to scan for $ref (can be Map, List, or primitive)
     * @param schemaRenameMap map of old schema names to new names
     */
    @SuppressWarnings("unchecked")
    private void updateSchemaReferences(Object obj, Map<String, String> schemaRenameMap) {
        if (schemaRenameMap.isEmpty()) {
            return;
        }

        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;

            // Check if this map has a $ref field
            if (map.containsKey("$ref")) {
                String ref = (String) map.get("$ref");
                if (ref != null && ref.startsWith("#/components/schemas/")) {
                    String schemaName = ref.substring("#/components/schemas/".length());
                    if (schemaRenameMap.containsKey(schemaName)) {
                        String newSchemaName = schemaRenameMap.get(schemaName);
                        map.put("$ref", "#/components/schemas/" + newSchemaName);
                        log.debug("🔗 Updated schema $ref: {} -> {}", schemaName, newSchemaName);
                    }
                }
            }

            // Recursively scan all values
            for (Object value : map.values()) {
                updateSchemaReferences(value, schemaRenameMap);
            }

        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            for (Object item : list) {
                updateSchemaReferences(item, schemaRenameMap);
            }
        }
    }

    /**
     * Extracts the pathname from the first server in the imported document.
     *
     * @param importedDoc the imported AsyncAPI document
     * @return the pathname of the first server, or null if not found
     */
    @SuppressWarnings("unchecked")
    private String extractFirstServerPathname(Map<String, Object> importedDoc) {
        Object serversObj = importedDoc.get("servers");
        if (!(serversObj instanceof Map)) {
            return null;
        }

        Map<String, Object> servers = (Map<String, Object>) serversObj;
        if (servers.isEmpty()) {
            return null;
        }

        // Get first server
        Map.Entry<String, Object> firstServerEntry = servers.entrySet().iterator().next();
        Object serverObj = firstServerEntry.getValue();
        if (!(serverObj instanceof Map)) {
            return null;
        }

        Map<String, Object> server = (Map<String, Object>) serverObj;
        Object pathnameObj = server.get("pathname");
        return pathnameObj instanceof String ? (String) pathnameObj : null;
    }

    /**
     * Enriches a channel with missing x-ouroboros-* fields.
     * For import operations, channels are not enriched with Ouroboros fields.
     */
    private void enrichChannelWithOuroborosFields(Map<String, Object> channel) {
        // No enrichment for imported channels
    }

    /**
     * Enriches an operation with missing x-ouroboros-* fields.
     * Adds x-ouroboros-id (UUID), x-ouroboros-progress ("none"),
     * x-ouroboros-diff ("none"), and x-ouroboros-entrypoint (server pathname).
     *
     * @param operation the operation to enrich
     * @param entrypoint the server pathname to use as entrypoint
     */
    private void enrichOperationWithOuroborosFields(Map<String, Object> operation, String entrypoint) {
        if (!operation.containsKey("x-ouroboros-id")) {
            operation.put("x-ouroboros-id", UUID.randomUUID().toString());
        }
        if (!operation.containsKey("x-ouroboros-progress")) {
            operation.put("x-ouroboros-progress", "none");
        }
        if (!operation.containsKey("x-ouroboros-diff")) {
            operation.put("x-ouroboros-diff", "none");
        }
        if (!operation.containsKey("x-ouroboros-entrypoint") && entrypoint != null) {
            operation.put("x-ouroboros-entrypoint", entrypoint);
        }
    }

    @Override
    public String exportYaml() throws Exception {
        lock.readLock().lock();
        try {
            return yamlParser.readYamlContent();
        } finally {
            lock.readLock().unlock();
        }
    }
}

