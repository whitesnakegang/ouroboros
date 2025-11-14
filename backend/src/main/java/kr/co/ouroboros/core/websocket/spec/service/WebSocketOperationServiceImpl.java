package kr.co.ouroboros.core.websocket.spec.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
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
    private final WebSocketYamlImportService yamlImportService;
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public List<OperationResponse> createOperations(CreateOperationRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            // Read existing document or create new one (from file, not cache)
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

            // Sync missing schemas and messages from cache to file and update $ref references to use class names
            Map<String, Object> cacheDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (cacheDoc != null) {
                syncMissingSchemasAndMessagesFromCache(cacheDoc, asyncApiDoc);
                
                // Update all $ref references in created operations to use class names instead of full package names
                Map<String, Object> cacheSchemas = yamlParser.getSchemas(cacheDoc);
                Map<String, Object> cacheMessages = yamlParser.getMessages(cacheDoc);
                if ((cacheSchemas != null && !cacheSchemas.isEmpty()) || (cacheMessages != null && !cacheMessages.isEmpty())) {
                    Map<String, String> packageToClassNameMap = buildPackageToClassNameMap(cacheSchemas, cacheMessages);
                    
                    // Update all operations in the document
                    Map<String, Object> operations = yamlParser.getOperations(asyncApiDoc);
                    if (operations != null) {
                        for (Map.Entry<String, Object> opEntry : operations.entrySet()) {
                            Object opObj = opEntry.getValue();
                            if (opObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> opMap = (Map<String, Object>) opObj;
                                updateSchemaAndMessageReferences(opMap, packageToClassNameMap);
                            }
                        }
                    }
                }
            }

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

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
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                return new ArrayList<>();
            }

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
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                throw new IllegalArgumentException("No operations found. The specification file does not exist.");
            }

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

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();
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

            // Sync missing schemas and messages from cache to file and update $ref references to use class names
            Map<String, Object> cacheDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (cacheDoc != null) {
                syncMissingSchemasAndMessagesFromCache(cacheDoc, asyncApiDoc);
                
                // Update all $ref references in operation to use class names instead of full package names
                Map<String, Object> cacheSchemas = yamlParser.getSchemas(cacheDoc);
                Map<String, Object> cacheMessages = yamlParser.getMessages(cacheDoc);
                if ((cacheSchemas != null && !cacheSchemas.isEmpty()) || (cacheMessages != null && !cacheMessages.isEmpty())) {
                    Map<String, String> packageToClassNameMap = buildPackageToClassNameMap(cacheSchemas, cacheMessages);
                    updateSchemaAndMessageReferences(existingOperation, packageToClassNameMap);
                }
            }

            // Check and delete channels that are no longer referenced
            channelManager.cleanupUnusedChannels(asyncApiDoc, removedChannels);

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

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

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();

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

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

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
            if (channelObj instanceof Map<?, ?> channelMapObj) {
                Map<String, String> channelMap = (Map<String, String>) channelMapObj;
                ChannelReference channelRef = new ChannelReference();
                // ReferenceConverter converts "$ref" to "ref"
                String ref = channelMap.get("ref");
                channelRef.setRef(ref);
                operation.setChannel(channelRef);
            }

            // Messages (already converted to ref by ReferenceConverter)
            Object messagesObj = processedMap.get("messages");
            if (messagesObj instanceof List<?> messagesListObj) {
                List<Map<String, String>> messagesList = (List<Map<String, String>>) messagesListObj;
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
            if (replyObj instanceof Map<?, ?> replyMapObj) {
                Map<String, Object> replyMap = (Map<String, Object>) replyMapObj;
                Reply reply = new Reply();

                Object replyChannelObj = replyMap.get("channel");
                if (replyChannelObj instanceof Map<?, ?> replyChannelMapObj) {
                    Map<String, String> replyChannelMap = (Map<String, String>) replyChannelMapObj;
                    ChannelReference replyChannelRef = new ChannelReference();
                    // ReferenceConverter converts "$ref" to "ref"
                    String ref = replyChannelMap.get("ref");
                    replyChannelRef.setRef(ref);
                    reply.setChannel(replyChannelRef);
                }

                Object replyMessagesObj = replyMap.get("messages");
                if (replyMessagesObj instanceof List<?> replyMessagesListObj) {
                    List<Map<String, String>> replyMessagesList = (List<Map<String, String>>) replyMessagesListObj;
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
            ImportYamlResponse response = yamlImportService.importYaml(yamlContent);
            
            // After import, read the document from file and update cache
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);
            
            return response;
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

    /**
     * Syncs a cache-only operation to the YAML file.
     * <p>
     * This method is used when an operation exists only in the cache (from code scanning)
     * but not in the YAML file. It adds the operation to the file so it can be edited via
     * the update endpoint.
     * <p>
     * If the operation already exists in the file, this operation is a no-op.
     *
     * @param id operation UUID (x-ouroboros-id)
     * @return synced operation details
     * @throws IllegalArgumentException if operation not found in cache
     * @throws Exception if sync fails
     */
    @Override
    public OperationResponse syncToFile(String id) throws Exception {
        lock.writeLock().lock();
        try {
            // Step 1: Find operation in cache
            Map<String, Object> cacheDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (cacheDoc == null) {
                throw new IllegalArgumentException("No operations found in cache.");
            }

            Map.Entry<String, Map<String, Object>> cacheOperationEntry = yamlParser.findOperationById(cacheDoc, id);
            if (cacheOperationEntry == null) {
                throw new IllegalArgumentException("Operation with id '" + id + "' not found in cache.");
            }

            String operationName = cacheOperationEntry.getKey();
            Map<String, Object> cacheOperation = cacheOperationEntry.getValue();

            // Step 2: Read or create file document
            Map<String, Object> fileDoc = yamlParser.readOrCreateDocument();

            // Step 3: Check if operation already exists in file
            Map.Entry<String, Map<String, Object>> fileOperationEntry = yamlParser.findOperationById(fileDoc, id);
            if (fileOperationEntry != null) {
                // Already exists in file, just return it
                log.info("Operation '{}' already exists in file (ID: {}), no sync needed", operationName, id);
                Operation operation = convertMapToOperation(fileOperationEntry.getValue());
                return OperationResponse.builder()
                        .operationName(fileOperationEntry.getKey())
                        .operation(operation)
                        .build();
            }

            // Step 4: Sync missing schemas and messages from cache to file first
            syncMissingSchemasAndMessagesFromCache(cacheDoc, fileDoc);

            // Step 5: Deep copy operation from cache
            Map<String, Object> operationToAdd = deepCopyOperation(cacheOperation);

            // Step 5.1: Reset Ouroboros custom fields to default values when syncing to file
            // WebSocket operations use: x-ouroboros-progress, x-ouroboros-diff (no x-ouroboros-tag)
            operationToAdd.put("x-ouroboros-progress", "none");
            operationToAdd.put("x-ouroboros-diff", "none");
            
            // Step 5.1.1: Set x-ouroboros-entrypoint from the first server's pathname in file document
            // (Deep copy already contains cache operation's entrypoint, but we override with file's server value)
            String entrypoint = serverManager.extractPathname(fileDoc);
            if (entrypoint != null) {
                operationToAdd.put("x-ouroboros-entrypoint", entrypoint);
            }
            // If file has no server, keep the existing entrypoint from cache operation (already in operationToAdd)

            // Step 5.2: Update all $ref references in operation to use class names instead of full package names
            Map<String, Object> cacheSchemas = yamlParser.getSchemas(cacheDoc);
            Map<String, Object> cacheMessages = yamlParser.getMessages(cacheDoc);
            Map<String, String> packageToClassNameMap = new HashMap<>();
            if ((cacheSchemas != null && !cacheSchemas.isEmpty()) || (cacheMessages != null && !cacheMessages.isEmpty())) {
                packageToClassNameMap = buildPackageToClassNameMap(cacheSchemas, cacheMessages);
                updateSchemaAndMessageReferences(operationToAdd, packageToClassNameMap);
            }

            // Step 5.3: Ensure referenced channels exist in file
            @SuppressWarnings("unchecked")
            Map<String, Object> channelRef = (Map<String, Object>) operationToAdd.get("channel");
            if (channelRef != null) {
                String channelRefStr = (String) channelRef.get("$ref");
                if (channelRefStr != null && channelRefStr.startsWith("#/channels/")) {
                    String channelName = channelRefStr.substring("#/channels/".length());
                    // Extract class name if channel name has package prefix
                    String className = extractClassNameFromFullName(channelName);
                    if (className != null && !className.equals(channelName)) {
                        // Update channel reference to use class name
                        channelRef.put("$ref", "#/channels/" + className);
                        channelName = className;
                    }
                    
                    // Ensure channel exists in file (will be created if missing)
                    Map<String, Object> cacheChannels = yamlParser.getChannels(cacheDoc);
                    if (cacheChannels != null) {
                        // Check both full name and class name in cache channels
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cacheChannel = (Map<String, Object>) cacheChannels.get(channelName);
                        if (cacheChannel == null && !channelName.equals(extractClassNameFromFullName(channelName))) {
                            // Try to find by full package name
                            for (String key : cacheChannels.keySet()) {
                                if (extractClassNameFromFullName(key).equals(channelName)) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> foundChannel = (Map<String, Object>) cacheChannels.get(key);
                                    cacheChannel = foundChannel;
                                    break;
                                }
                            }
                        }
                        
                        if (cacheChannel != null) {
                            Map<String, Object> fileChannels = yamlParser.getOrCreateChannels(fileDoc);
                            if (!fileChannels.containsKey(channelName)) {
                                Map<String, Object> channelToAdd = deepCopyChannel(cacheChannel);
                                
                                // Update messages map keys to use class names instead of full package names
                                @SuppressWarnings("unchecked")
                                Map<String, Object> messagesMap = (Map<String, Object>) channelToAdd.get("messages");
                                if (messagesMap != null && !messagesMap.isEmpty()) {
                                    Map<String, Object> updatedMessagesMap = new LinkedHashMap<>();
                                    for (Map.Entry<String, Object> messageEntry : messagesMap.entrySet()) {
                                        String fullMessageKey = messageEntry.getKey();
                                        String messageClassName = extractClassNameFromFullName(fullMessageKey);
                                        Object messageValue = messageEntry.getValue();
                                        
                                        // Update $ref in message value if it exists
                                        if (messageValue instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> messageMap = (Map<String, Object>) messageValue;
                                            updateSchemaAndMessageReferences(messageMap, packageToClassNameMap);
                                        }
                                        
                                        updatedMessagesMap.put(messageClassName, messageValue);
                                    }
                                    channelToAdd.put("messages", updatedMessagesMap);
                                }
                                
                                // Update other $ref references in channel
                                updateSchemaAndMessageReferences(channelToAdd, packageToClassNameMap);
                                
                                fileChannels.put(channelName, channelToAdd);
                                log.debug("Synced channel '{}' from cache to file", channelName);
                            }
                        }
                    }
                }
            }

            // Step 6: Add operation to file
            yamlParser.putOperation(fileDoc, operationName, operationToAdd);

            // Step 7: Write document to file
            yamlParser.writeDocument(fileDoc);

            // Step 8: Update cache (validates with scanned state + updates cache)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, fileDoc);

            log.info("Synced cache-only operation '{}' to file (ID: {})", operationName, id);

            Operation operation = convertMapToOperation(operationToAdd);
            return OperationResponse.builder()
                    .operationName(operationName)
                    .operation(operation)
                    .build();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Syncs missing schemas and messages from cache to file document.
     * <p>
     * This method checks all schemas and messages in the cache and adds any that don't exist
     * in the file document to ensure consistency during operation sync.
     *
     * @param cacheDoc the cached document containing all schemas and messages
     * @param fileDoc the file document to sync into
     */
    @SuppressWarnings("unchecked")
    private void syncMissingSchemasAndMessagesFromCache(Map<String, Object> cacheDoc, Map<String, Object> fileDoc) {
        Map<String, Object> cacheSchemas = yamlParser.getSchemas(cacheDoc);
        Map<String, Object> cacheMessages = yamlParser.getMessages(cacheDoc);

        // Sync schemas
        if (cacheSchemas != null && !cacheSchemas.isEmpty()) {
            Map<String, Object> fileSchemas = yamlParser.getSchemas(fileDoc);
            if (fileSchemas == null) {
                Map<String, Object> components = yamlParser.getOrCreateComponents(fileDoc);
                fileSchemas = yamlParser.getOrCreateSchemas(components);
            }

            int syncedCount = 0;
            for (Map.Entry<String, Object> cacheEntry : cacheSchemas.entrySet()) {
                String cacheSchemaName = cacheEntry.getKey();
                String className = extractClassNameFromFullName(cacheSchemaName);
                if (className == null || className.isEmpty()) {
                    className = cacheSchemaName;
                }

                if (fileSchemas.containsKey(className)) {
                    continue;
                }

                try {
                    Map<String, Object> cacheSchema = (Map<String, Object>) cacheEntry.getValue();
                    Map<String, Object> schemaToAdd = deepCopySchema(cacheSchema);
                    updateSchemaReferencesToClassName(schemaToAdd, cacheSchemas);
                    fileSchemas.put(className, schemaToAdd);
                    syncedCount++;
                    log.debug("Synced schema '{}' (as '{}') from cache to file", cacheSchemaName, className);
                } catch (Exception e) {
                    log.warn("Failed to sync schema '{}' from cache to file: {}", cacheSchemaName, e.getMessage());
                }
            }

            if (syncedCount > 0) {
                log.info("Synced {} missing schema(s) from cache to file", syncedCount);
            }
        }

        // Sync messages
        if (cacheMessages != null && !cacheMessages.isEmpty()) {
            Map<String, Object> fileMessages = yamlParser.getMessages(fileDoc);
            if (fileMessages == null) {
                Map<String, Object> components = yamlParser.getOrCreateComponents(fileDoc);
                fileMessages = yamlParser.getOrCreateMessages(components);
            }

            int syncedCount = 0;
            for (Map.Entry<String, Object> cacheEntry : cacheMessages.entrySet()) {
                String cacheMessageName = cacheEntry.getKey();
                String className = extractClassNameFromFullName(cacheMessageName);
                if (className == null || className.isEmpty()) {
                    className = cacheMessageName;
                }

                if (fileMessages.containsKey(className)) {
                    continue;
                }

                try {
                    Map<String, Object> cacheMessage = (Map<String, Object>) cacheEntry.getValue();
                    Map<String, Object> messageToAdd = deepCopyMessage(cacheMessage);
                    
                    // Update message's internal 'name' field to use class name if it exists
                    if (messageToAdd.containsKey("name")) {
                        Object nameObj = messageToAdd.get("name");
                        if (nameObj instanceof String) {
                            String fullName = (String) nameObj;
                            String nameClassName = extractClassNameFromFullName(fullName);
                            if (nameClassName != null && !nameClassName.equals(fullName)) {
                                messageToAdd.put("name", nameClassName);
                            }
                        }
                    }
                    
                    updateMessageSchemaReferences(messageToAdd, cacheSchemas);
                    fileMessages.put(className, messageToAdd);
                    syncedCount++;
                    log.debug("Synced message '{}' (as '{}') from cache to file", cacheMessageName, className);
                } catch (Exception e) {
                    log.warn("Failed to sync message '{}' from cache to file: {}", cacheMessageName, e.getMessage());
                }
            }

            if (syncedCount > 0) {
                log.info("Synced {} missing message(s) from cache to file", syncedCount);
            }
        }
    }

    /**
     * Builds a map from package-qualified names to class names for both schemas and messages.
     *
     * @param cacheSchemas map of cache schemas (can be null)
     * @param cacheMessages map of cache messages (can be null)
     * @return map from package-qualified names to class names
     */
    private Map<String, String> buildPackageToClassNameMap(Map<String, Object> cacheSchemas, Map<String, Object> cacheMessages) {
        Map<String, String> map = new HashMap<>();
        
        if (cacheSchemas != null) {
            for (String fullName : cacheSchemas.keySet()) {
                String className = extractClassNameFromFullName(fullName);
                if (className != null && !className.equals(fullName)) {
                    map.put(fullName, className);
                }
            }
        }
        
        if (cacheMessages != null) {
            for (String fullName : cacheMessages.keySet()) {
                String className = extractClassNameFromFullName(fullName);
                if (className != null && !className.equals(fullName)) {
                    map.put(fullName, className);
                }
            }
        }
        
        return map;
    }

    /**
     * Extracts the simple class name from a package-qualified name.
     * <p>
     * Example: "com.c102.ourotest.dto.User" -> "User"
     *
     * @param fullName the package-qualified name
     * @return the simple class name, or the original string if no '.' is present
     */
    private String extractClassNameFromFullName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return fullName;
        }
        
        int lastDotIndex = fullName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fullName;
        }
        
        return fullName.substring(lastDotIndex + 1);
    }

    /**
     * Recursively updates all $ref references in an operation to use class names instead of full package names.
     * Updates both schema references and message references.
     *
     * @param operation the operation map to update
     * @param packageToClassNameMap map of package-qualified names to class names
     */
    @SuppressWarnings("unchecked")
    private void updateSchemaAndMessageReferences(Object operation, Map<String, String> packageToClassNameMap) {
        if (operation == null || !(operation instanceof Map)) {
            return;
        }
        
        Map<String, Object> operationMap = (Map<String, Object>) operation;
        
        // Update $ref if present (can be schema or message reference)
        if (operationMap.containsKey("$ref")) {
            Object refObj = operationMap.get("$ref");
            if (refObj instanceof String) {
                String ref = (String) refObj;
                String updatedRef = updateRefToClassName(ref, packageToClassNameMap);
                if (updatedRef != null && !updatedRef.equals(ref)) {
                    operationMap.put("$ref", updatedRef);
                }
            }
        }
        
        // Recursively process all values
        for (Object value : operationMap.values()) {
            if (value instanceof Map) {
                updateSchemaAndMessageReferences(value, packageToClassNameMap);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    updateSchemaAndMessageReferences(item, packageToClassNameMap);
                }
            }
        }
    }

    /**
     * Updates a $ref string to use class name instead of full package name.
     *
     * @param ref the $ref string to update
     * @param packageToClassNameMap map of package-qualified names to class names (can be empty)
     * @return updated $ref string, or original if no update needed
     */
    private String updateRefToClassName(String ref, Map<String, String> packageToClassNameMap) {
        if (ref == null || ref.isEmpty()) {
            return ref;
        }

        // Handle schema references: "#/components/schemas/package.name.ClassName"
        if (ref.startsWith("#/components/schemas/")) {
            String fullSchemaName = ref.substring("#/components/schemas/".length());
            String className = null;
            if (packageToClassNameMap != null && !packageToClassNameMap.isEmpty()) {
                className = packageToClassNameMap.get(fullSchemaName);
            }
            if (className == null) {
                // Fallback: extract class name directly from ref
                className = extractClassNameFromFullName(fullSchemaName);
            }
            if (className != null && !className.equals(fullSchemaName)) {
                return "#/components/schemas/" + className;
            }
        }

        // Handle message references: "#/components/messages/package.name.MessageName"
        if (ref.startsWith("#/components/messages/")) {
            String fullMessageName = ref.substring("#/components/messages/".length());
            String className = null;
            if (packageToClassNameMap != null && !packageToClassNameMap.isEmpty()) {
                className = packageToClassNameMap.get(fullMessageName);
            }
            if (className == null) {
                // Fallback: extract class name directly from ref
                className = extractClassNameFromFullName(fullMessageName);
            }
            if (className != null && !className.equals(fullMessageName)) {
                return "#/components/messages/" + className;
            }
        }

        // Handle channel-scoped message references: "#/channels/channelName/messages/package.name.MessageName"
        if (ref.contains("/messages/")) {
            int messagesIndex = ref.indexOf("/messages/");
            String prefix = ref.substring(0, messagesIndex + "/messages/".length());
            String fullMessageName = ref.substring(prefix.length());
            String className = null;
            if (packageToClassNameMap != null && !packageToClassNameMap.isEmpty()) {
                className = packageToClassNameMap.get(fullMessageName);
            }
            if (className == null) {
                // Fallback: extract class name directly from ref
                className = extractClassNameFromFullName(fullMessageName);
            }
            if (className != null && !className.equals(fullMessageName)) {
                return prefix + className;
            }
        }

        return ref;
    }

    /**
     * Recursively updates all schema references in a message to use class names instead of full package names.
     *
     * @param message the message map to update
     * @param cacheSchemas map of all cache schemas to resolve full names to class names
     */
    @SuppressWarnings("unchecked")
    private void updateMessageSchemaReferences(Object message, Map<String, Object> cacheSchemas) {
        if (message == null || !(message instanceof Map)) {
            return;
        }
        
        Map<String, Object> messageMap = (Map<String, Object>) message;
        
        // Update payload schema references
        Object payloadObj = messageMap.get("payload");
        if (payloadObj instanceof Map) {
            Map<String, Object> payload = (Map<String, Object>) payloadObj;
            Object schemaObj = payload.get("schema");
            if (schemaObj instanceof Map) {
                updateSchemaReferencesToClassName(schemaObj, cacheSchemas);
            }
        }
        
        // Update headers schema references
        Object headersObj = messageMap.get("headers");
        if (headersObj instanceof Map) {
            Map<String, Object> headers = (Map<String, Object>) headersObj;
            if (headers.containsKey("$ref")) {
                Object refObj = headers.get("$ref");
                if (refObj instanceof String) {
                    String ref = (String) refObj;
                    if (ref.startsWith("#/components/schemas/")) {
                        String fullSchemaName = ref.substring("#/components/schemas/".length());
                        String className = extractClassNameFromFullName(fullSchemaName);
                        if (className != null && !className.equals(fullSchemaName)) {
                            headers.put("$ref", "#/components/schemas/" + className);
                        }
                    }
                }
            }
        }
    }

    /**
     * Recursively updates all $ref references in a schema to use class names instead of full package names.
     *
     * @param schema the schema map to update
     * @param cacheSchemas map of all cache schemas to resolve full names to class names
     */
    @SuppressWarnings("unchecked")
    private void updateSchemaReferencesToClassName(Object schema, Map<String, Object> cacheSchemas) {
        if (schema == null || !(schema instanceof Map)) {
            return;
        }
        
        Map<String, Object> schemaMap = (Map<String, Object>) schema;
        
        // Update $ref if present
        if (schemaMap.containsKey("$ref")) {
            Object refObj = schemaMap.get("$ref");
            if (refObj instanceof String) {
                String ref = (String) refObj;
                if (ref.startsWith("#/components/schemas/")) {
                    String fullSchemaName = ref.substring("#/components/schemas/".length());
                    String className = extractClassNameFromFullName(fullSchemaName);
                    if (className != null && !className.equals(fullSchemaName)) {
                        schemaMap.put("$ref", "#/components/schemas/" + className);
                    }
                }
            }
        }
        
        // Recursively process properties
        Object propertiesObj = schemaMap.get("properties");
        if (propertiesObj instanceof Map) {
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            for (Object property : properties.values()) {
                updateSchemaReferencesToClassName(property, cacheSchemas);
            }
        }
        
        // Recursively process items (for array types)
        Object itemsObj = schemaMap.get("items");
        if (itemsObj instanceof Map) {
            updateSchemaReferencesToClassName(itemsObj, cacheSchemas);
        }
    }

    /**
     * Deep copies an operation map to prevent cache pollution.
     *
     * @param operation the original operation to copy
     * @return a deep copy of the operation
     */
    private Map<String, Object> deepCopyOperation(Map<String, Object> operation) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(operation);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to deep copy operation, returning original (UNSAFE!)", e);
            return new LinkedHashMap<>(operation);
        }
    }

    /**
     * Deep copies a schema map to prevent cache pollution.
     *
     * @param schema the original schema to copy
     * @return a deep copy of the schema
     */
    private Map<String, Object> deepCopySchema(Map<String, Object> schema) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(schema);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to deep copy schema, returning original (UNSAFE!)", e);
            return new LinkedHashMap<>(schema);
        }
    }

    /**
     * Deep copies a message map to prevent cache pollution.
     *
     * @param message the original message to copy
     * @return a deep copy of the message
     */
    private Map<String, Object> deepCopyMessage(Map<String, Object> message) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(message);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to deep copy message, returning original (UNSAFE!)", e);
            return new LinkedHashMap<>(message);
        }
    }

    /**
     * Deep copies a channel map to prevent cache pollution.
     *
     * @param channel the original channel to copy
     * @return a deep copy of the channel
     */
    private Map<String, Object> deepCopyChannel(Map<String, Object> channel) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(channel);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to deep copy channel, returning original (UNSAFE!)", e);
            return new LinkedHashMap<>(channel);
        }
    }
}

