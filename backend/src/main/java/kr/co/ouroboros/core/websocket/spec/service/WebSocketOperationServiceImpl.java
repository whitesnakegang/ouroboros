package kr.co.ouroboros.core.websocket.spec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.websocket.common.dto.ChannelReference;
import kr.co.ouroboros.core.websocket.common.dto.MessageReference;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import kr.co.ouroboros.core.websocket.common.dto.Reply;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.core.websocket.spec.util.ReferenceConverter;
import kr.co.ouroboros.ui.websocket.spec.dto.*;
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
    private final WebSocketChannelManager channelManager;
    private final WebSocketServerManager serverManager;
    private final WebSocketReferenceUpdater referenceUpdater;
    private final WebSocketYamlImportService yamlImportService;
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
            serverManager.ensureServerExists(asyncApiDoc, request.getProtocol(), request.getPathname());

            // At least one of receives or replies must be provided
            boolean hasReceives = request.getReceives() != null && !request.getReceives().isEmpty();
            boolean hasReplies = request.getReplies() != null && !request.getReplies().isEmpty();

            if (!hasReceives && !hasReplies) {
                throw new IllegalArgumentException("At least one receive or reply channel must be provided");
            }

            // Reply-only operations
            if (!hasReceives && hasReplies) {
                for (ChannelMessageInfo reply : request.getReplies()) {
                    String replyChannelName = channelManager.ensureChannelExists(asyncApiDoc, reply);

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
                    String receiveChannelName = channelManager.ensureChannelExists(asyncApiDoc, receive);

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
                            String replyChannelName = channelManager.ensureChannelExists(asyncApiDoc, reply);

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
                        serverManager.extractProtocol(asyncApiDoc);
                String pathname = request.getPathname() != null ? request.getPathname() :
                        serverManager.extractPathname(asyncApiDoc);

                if (protocol != null && pathname != null) {
                    serverManager.ensureServerExists(asyncApiDoc, protocol, pathname);
                    updatedPathname = pathname;
                }
            }

            // Extract old channel references before update
            Set<String> oldChannelRefs = channelManager.extractChannelReferences(existingOperation);

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
                String channelName = channelManager.ensureChannelExists(asyncApiDoc, request.getReceive());
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
                    String replyChannelName = channelManager.ensureChannelExists(asyncApiDoc, request.getReply());
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
                    
                    String replyChannelName = channelManager.ensureChannelExists(asyncApiDoc, request.getReply());
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
            Set<String> newChannelRefs = channelManager.extractChannelReferences(updatedOperation);

            // Find channels that were removed (in old but not in new)
            Set<String> removedChannels = new HashSet<>(oldChannelRefs);
            removedChannels.removeAll(newChannelRefs);

            // Apply the update to the actual document
            existingOperation.clear();
            existingOperation.putAll(updatedOperation);

            // Check and delete channels that are no longer referenced
            channelManager.cleanupUnusedChannels(asyncApiDoc, removedChannels);

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
            Set<String> referencedChannels = channelManager.extractChannelReferences(operationToDelete);

            // Delete the operation
            boolean removed = yamlParser.removeOperation(asyncApiDoc, operationName);
            if (!removed) {
                throw new IllegalArgumentException("Operation with id '" + id + "' not found");
            }

            // Check and delete channels that are no longer referenced
            channelManager.cleanupUnusedChannels(asyncApiDoc, referencedChannels);

            // Write to file
            yamlParser.writeDocument(asyncApiDoc);

            log.info("Deleted WebSocket operation: {}", operationName);
        } finally {
            lock.writeLock().unlock();
        }
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
        Map<String, Object> processedMap = ReferenceConverter.convertDollarRefToRef(operationMap);
        
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

            // Channel reference (already converted to ref by ReferenceConverter)
            Object channelObj = processedMap.get("channel");
            if (channelObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> channelMap = (Map<String, String>) channelObj;
                ChannelReference channelRef = new ChannelReference();
                // ReferenceConverter converts "$ref" to "ref"
                String ref = channelMap.get("ref");
                channelRef.setRef(ref);
                operation.setChannel(channelRef);
            }

            // Messages (already converted to ref by ReferenceConverter)
            Object messagesObj = processedMap.get("messages");
            if (messagesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messagesList = (List<Map<String, String>>) messagesObj;
                List<MessageReference> messageRefs = messagesList.stream()
                        .map(msgMap -> {
                            MessageReference msgRef = new MessageReference();
                            // ReferenceConverter converts "$ref" to "ref"
                            String ref = msgMap.get("ref");
                            msgRef.setRef(ref);
                            return msgRef;
                        })
                        .collect(Collectors.toList());
                operation.setMessages(messageRefs);
            }

            // Reply (already converted to ref by ReferenceConverter)
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
                    // ReferenceConverter converts "$ref" to "ref"
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
                                // ReferenceConverter converts "$ref" to "ref"
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
            return yamlImportService.importYaml(yamlContent);
        } finally {
            lock.writeLock().unlock();
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

