package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.ui.websocket.spec.dto.CreateMessageRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.MessageResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateMessageRequest;

import java.util.List;

/**
 * Service interface for WebSocket message management operations.
 * <p>
 * Manages reusable message definitions in the AsyncAPI components/messages section.
 *
 * @since 0.1.0
 */
public interface WebSocketMessageService {

    /**
     * Creates a new message definition.
     *
     * @param request message details
     * @return created message with metadata
     * @throws Exception if message creation fails or message already exists
     */
    MessageResponse createMessage(CreateMessageRequest request) throws Exception;

    /**
     * Retrieves all message definitions.
     *
     * @return list of all messages
     * @throws Exception if file reading fails
     */
    List<MessageResponse> getAllMessages() throws Exception;

    /**
     * Retrieves a specific message by name.
     *
     * @param messageName the message identifier
     * @return message details
     * @throws Exception if message not found or file reading fails
     */
    MessageResponse getMessage(String messageName) throws Exception;

    /**
     * Updates an existing message definition.
     *
     * @param messageName the message identifier
     * @param request updated message details
     * @return updated message
     * @throws Exception if message not found or update fails
     */
    MessageResponse updateMessage(String messageName, UpdateMessageRequest request) throws Exception;

    /**
     * Deletes a message definition.
     *
     * @param messageName the message identifier
     * @throws Exception if message not found or deletion fails
     */
    void deleteMessage(String messageName) throws Exception;
}
