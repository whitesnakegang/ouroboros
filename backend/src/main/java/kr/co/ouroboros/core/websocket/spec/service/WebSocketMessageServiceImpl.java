package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.core.websocket.spec.util.RefCleanupUtil;
import kr.co.ouroboros.core.websocket.spec.util.ReferenceConverter;
import kr.co.ouroboros.ui.websocket.spec.dto.CreateMessageRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.MessageResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link WebSocketMessageService}.
 * <p>
 * Manages message definitions in the AsyncAPI components/messages section of ourowebsocket.yml.
 * Uses {@link WebSocketYamlParser} for all YAML file operations.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessageServiceImpl implements WebSocketMessageService {

    private final WebSocketYamlParser yamlParser;
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new message in the AsyncAPI document and updates the processed spec cache.
     *
     * @param request contains the message name and definition fields used to build and insert the new message
     * @return the MessageResponse representing the created message
     */
    @Override
    public MessageResponse createMessage(CreateMessageRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            // Read existing document or create new one (from file, not cache)
            Map<String, Object> asyncApiDoc = yamlParser.readOrCreateDocument();

            // Check for duplicate message name
            if (yamlParser.messageExists(asyncApiDoc, request.getMessageName())) {
                throw new IllegalArgumentException("Message '" + request.getMessageName() + "' already exists");
            }

            // Build message definition
            Map<String, Object> messageDefinition = buildMessageDefinition(request);

            // Add message to document
            yamlParser.putMessage(asyncApiDoc, request.getMessageName(), messageDefinition);

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

            log.info("Created WebSocket message: {}", request.getMessageName());

            return convertToResponse(request.getMessageName(), messageDefinition);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<MessageResponse> getAllMessages() throws Exception {
        lock.readLock().lock();
        try {
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                return new ArrayList<>();
            }

            Map<String, Object> messages = yamlParser.getMessages(asyncApiDoc);

            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }

            List<MessageResponse> responses = new ArrayList<>();
            for (Map.Entry<String, Object> entry : messages.entrySet()) {
                String messageName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> messageDefinition = (Map<String, Object>) entry.getValue();
                responses.add(convertToResponse(messageName, messageDefinition));
            }

            return responses;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public MessageResponse getMessage(String messageName) throws Exception {
        lock.readLock().lock();
        try {
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                throw new IllegalArgumentException("No messages found. The specification file does not exist.");
            }

            Map<String, Object> messageDefinition = yamlParser.getMessage(asyncApiDoc, messageName);

            if (messageDefinition == null) {
                throw new IllegalArgumentException("Message '" + messageName + "' not found");
            }

            return convertToResponse(messageName, messageDefinition);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Update an existing AsyncAPI message using only the non-null fields from the request.
     *
     * @param messageName the name of the message to update
     * @param request container of fields to apply; only fields that are non-null on the request are updated
     * @return a MessageResponse representing the updated message
     * @throws IllegalArgumentException if the specification file does not exist or the named message is not found
     * @throws Exception if processing, validation, or caching of the updated specification fails
     */
    @Override
    public MessageResponse updateMessage(String messageName, UpdateMessageRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No messages found. The specification file does not exist.");
            }

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();
            Map<String, Object> existingMessage = yamlParser.getMessage(asyncApiDoc, messageName);

            if (existingMessage == null) {
                throw new IllegalArgumentException("Message '" + messageName + "' not found");
            }

            // Update only provided fields
            if (request.getName() != null) {
                existingMessage.put("name", request.getName());
            }
            if (request.getContentType() != null) {
                existingMessage.put("contentType", request.getContentType());
            }
            if (request.getDescription() != null) {
                existingMessage.put("description", request.getDescription());
            }
            if (request.getHeaders() != null) {
                // Convert ref to $ref for YAML storage
                existingMessage.put("headers", ReferenceConverter.convertRefToDollarRef(request.getHeaders()));
            }
            if (request.getPayload() != null) {
                // Convert ref to $ref for YAML storage
                existingMessage.put("payload", ReferenceConverter.convertRefToDollarRef(request.getPayload()));
            }

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

            log.info("Updated WebSocket message: {}", messageName);

            return convertToResponse(messageName, existingMessage);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a message from the WebSocket AsyncAPI document and updates the processed spec cache.
     *
     * @param messageName the name of the message to remove
     * @throws IllegalArgumentException if the specification file does not exist or the named message is not found
     * @throws Exception if processing or caching fails
     */
    @Override
    public void deleteMessage(String messageName) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No messages found. The specification file does not exist.");
            }

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();

            boolean removed = yamlParser.removeMessage(asyncApiDoc, messageName);

            if (!removed) {
                throw new IllegalArgumentException("Message '" + messageName + "' not found");
            }

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

            log.info("Deleted WebSocket message: {}", messageName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Helper methods for building message structures

    private Map<String, Object> buildMessageDefinition(CreateMessageRequest request) {
        Map<String, Object> message = new LinkedHashMap<>();

        if (request.getName() != null) {
            message.put("name", request.getName());
        }

        message.put("contentType", request.getContentType());

        if (request.getDescription() != null) {
            message.put("description", request.getDescription());
        }

        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            // Convert ref to $ref for YAML storage
            message.put("headers", ReferenceConverter.convertRefToDollarRef(request.getHeaders()));
        }

        if (request.getPayload() != null && !request.getPayload().isEmpty()) {
            // Convert ref to $ref for YAML storage
            message.put("payload", ReferenceConverter.convertRefToDollarRef(request.getPayload()));
        }

        return message;
    }

    private MessageResponse convertToResponse(String messageName, Map<String, Object> messageDefinition) {
        // Clean package names and convert $ref to ref for JSON API
        Map<String, Object> headers = safeGetMap(messageDefinition, "headers");
        Map<String, Object> payload = safeGetMap(messageDefinition, "payload");

        // Clean package names from $ref values
        if (headers != null) {
            headers = RefCleanupUtil.cleanupPackageNamesInRefs(headers);
        }
        if (payload != null) {
            payload = RefCleanupUtil.cleanupPackageNamesInRefs(payload);
        }

        // Clean package names from messageName and name fields
        String cleanedMessageName = RefCleanupUtil.extractClassNameFromFullName(messageName);
        String name = safeGetString(messageDefinition, "name");
        String cleanedName = name != null ? RefCleanupUtil.extractClassNameFromFullName(name) : null;

        return MessageResponse.builder()
                .messageName(cleanedMessageName)
                .name(cleanedName)
                .contentType(safeGetString(messageDefinition, "contentType"))
                .description(safeGetString(messageDefinition, "description"))
                .headers(headers != null ? ReferenceConverter.convertDollarRefToRef(headers) : null)
                .payload(payload != null ? ReferenceConverter.convertDollarRefToRef(payload) : null)
                .build();
    }

    /**
     * Safely extracts a String value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the String value, or null if not found or not a String
     */
    private String safeGetString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            log.warn("Expected String for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Safely extracts a Map value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the Map value, or null if not found or not a Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value != null) {
            log.warn("Expected Map for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

}
