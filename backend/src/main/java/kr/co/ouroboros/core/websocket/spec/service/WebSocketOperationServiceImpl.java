package kr.co.ouroboros.core.websocket.spec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.websocket.common.dto.ChannelReference;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.Reply;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.ui.websocket.spec.dto.ChannelMessageInfo;
import kr.co.ouroboros.ui.websocket.spec.dto.CreateOperationRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.OperationResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateOperationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

                responses.add(OperationResponse.builder()
                        .operationName(operationName)
                        .operation(operation)
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
                
                // Update messages if provided
                if (request.getReceive().getMessages() != null && !request.getReceive().getMessages().isEmpty()) {
                    List<Map<String, String>> messageRefs = request.getReceive().getMessages().stream()
                            .map(msgName -> {
                                Map<String, String> ref = new LinkedHashMap<>();
                                ref.put("$ref", "#/components/messages/" + msgName);
                                return ref;
                            })
                            .collect(Collectors.toList());
                    updatedOperation.put("messages", messageRefs);
                }
            }
            
            // Update reply channel
            if (request.getReply() != null) {
                Map<String, Object> reply = new LinkedHashMap<>();
                
                String replyChannelName = ensureChannelExists(asyncApiDoc, request.getReply());
                Map<String, String> replyChannelRef = new LinkedHashMap<>();
                replyChannelRef.put("$ref", "#/channels/" + replyChannelName);
                reply.put("channel", replyChannelRef);
                
                // Update reply messages if provided
                if (request.getReply().getMessages() != null && !request.getReply().getMessages().isEmpty()) {
                    List<Map<String, String>> messageRefs = request.getReply().getMessages().stream()
                            .map(msgName -> {
                                Map<String, String> ref = new LinkedHashMap<>();
                                ref.put("$ref", "#/components/messages/" + msgName);
                                return ref;
                            })
                            .collect(Collectors.toList());
                    reply.put("messages", messageRefs);
                }
                
                updatedOperation.put("reply", reply);
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

            // Messages
            if (receiveMessages != null && !receiveMessages.isEmpty()) {
                List<Map<String, String>> messageRefs = receiveMessages.stream()
                        .map(msgName -> {
                            Map<String, String> ref = new LinkedHashMap<>();
                            ref.put("$ref", "#/components/messages/" + msgName);
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

            // Messages
            if (replyMessages != null && !replyMessages.isEmpty()) {
                List<Map<String, String>> messageRefs = replyMessages.stream()
                        .map(msgName -> {
                            Map<String, String> ref = new LinkedHashMap<>();
                            ref.put("$ref", "#/components/messages/" + msgName);
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

            if (replyMessages != null && !replyMessages.isEmpty()) {
                List<Map<String, String>> replyMessageRefs = replyMessages.stream()
                        .map(msgName -> {
                            Map<String, String> ref = new LinkedHashMap<>();
                            ref.put("$ref", "#/components/messages/" + msgName);
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
        try {
            return objectMapper.convertValue(operationMap, Operation.class);
        } catch (Exception e) {
            log.error("Failed to convert operation map to Operation DTO", e);
            // Fallback: manual conversion
            Operation operation = new Operation();
            operation.setAction((String) operationMap.get("action"));
            operation.setXOuroborosId((String) operationMap.get("x-ouroboros-id"));
            operation.setXOuroborosEntrypoint((String) operationMap.get("x-ouroboros-entrypoint"));
            operation.setXOuroborosDiff((String) operationMap.get("x-ouroboros-diff"));
            operation.setXOuroborosProgress((String) operationMap.get("x-ouroboros-progress"));

            // Channel reference
            Object channelObj = operationMap.get("channel");
            if (channelObj instanceof Map) {
                Map<String, String> channelMap = (Map<String, String>) channelObj;
                ChannelReference channelRef = new ChannelReference();
                channelRef.setRef(channelMap.get("$ref"));
                operation.setChannel(channelRef);
            }

            // Messages
            Object messagesObj = operationMap.get("messages");
            if (messagesObj instanceof List) {
                List<Map<String, String>> messagesList = (List<Map<String, String>>) messagesObj;
                List<MessageReference> messageRefs = messagesList.stream()
                        .map(msgMap -> {
                            MessageReference msgRef = new MessageReference();
                            msgRef.setRef(msgMap.get("$ref"));
                            return msgRef;
                        })
                        .collect(Collectors.toList());
                operation.setMessages(messageRefs);
            }

            // Reply
            Object replyObj = operationMap.get("reply");
            if (replyObj instanceof Map) {
                Map<String, Object> replyMap = (Map<String, Object>) replyObj;
                Reply reply = new Reply();

                Object replyChannelObj = replyMap.get("channel");
                if (replyChannelObj instanceof Map) {
                    Map<String, String> replyChannelMap = (Map<String, String>) replyChannelObj;
                    ChannelReference replyChannelRef = new ChannelReference();
                    replyChannelRef.setRef(replyChannelMap.get("$ref"));
                    reply.setChannel(replyChannelRef);
                }

                Object replyMessagesObj = replyMap.get("messages");
                if (replyMessagesObj instanceof List) {
                    List<Map<String, String>> replyMessagesList = (List<Map<String, String>>) replyMessagesObj;
                    List<MessageReference> replyMessageRefs = replyMessagesList.stream()
                            .map(msgMap -> {
                                MessageReference msgRef = new MessageReference();
                                msgRef.setRef(msgMap.get("$ref"));
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
}

