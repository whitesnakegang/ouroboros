package kr.co.ouroboros.ui.websocket.spec.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.websocket.spec.service.WebSocketMessageService;
import kr.co.ouroboros.ui.websocket.spec.dto.CreateMessageRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.MessageResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing WebSocket message definitions.
 * <p>
 * Provides CRUD operations for reusable message definitions in the AsyncAPI
 * components/messages section. All endpoints are prefixed with {@code /ouro/websocket-specs/messages}.
 *
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequestMapping("/ouro/websocket-specs/messages")
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final WebSocketMessageService messageService;

    /**
     * Creates a new message definition.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param request message details
     * @return standard API response with creation result
     * @throws Exception if creation fails
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<MessageResponse>> createMessage(
            @RequestBody CreateMessageRequest request) throws Exception {
        MessageResponse data = messageService.createMessage(request);
        GlobalApiResponse<MessageResponse> response = GlobalApiResponse.success(
                data,
                "Message created successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all message definitions.
     * <p>
     * Returns a list of all message definitions from the AsyncAPI YAML file.
     * Returns an empty list if no messages exist.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @return list of all messages
     * @throws Exception if retrieval fails
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<MessageResponse>>> getAllMessages() throws Exception {
        List<MessageResponse> data = messageService.getAllMessages();
        GlobalApiResponse<List<MessageResponse>> response = GlobalApiResponse.success(
                data,
                "Messages retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific message by name.
     * <p>
     * Searches the components/messages section for the specified message name.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param messageName the message identifier
     * @return message details
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{messageName}")
    public ResponseEntity<GlobalApiResponse<MessageResponse>> getMessage(
            @PathVariable("messageName") String messageName) throws Exception {
        MessageResponse data = messageService.getMessage(messageName);
        GlobalApiResponse<MessageResponse> response = GlobalApiResponse.success(
                data,
                "Message retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing message definition.
     * <p>
     * Only provided fields will be updated. Existing values are preserved if not specified.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param messageName the message identifier
     * @param request updated message details
     * @return updated message
     * @throws Exception if update fails
     */
    @PutMapping("/{messageName}")
    public ResponseEntity<GlobalApiResponse<MessageResponse>> updateMessage(
            @PathVariable("messageName") String messageName,
            @RequestBody UpdateMessageRequest request) throws Exception {
        MessageResponse data = messageService.updateMessage(messageName, request);
        GlobalApiResponse<MessageResponse> response = GlobalApiResponse.success(
                data,
                "Message updated successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a message definition.
     * <p>
     * Removes the message from the AsyncAPI components/messages section.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param messageName the message identifier
     * @return success response
     * @throws Exception if deletion fails
     */
    @DeleteMapping("/{messageName}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteMessage(
            @PathVariable("messageName") String messageName) throws Exception {
        messageService.deleteMessage(messageName);
        GlobalApiResponse<Void> response = GlobalApiResponse.success(
                null,
                "Message deleted successfully"
        );
        return ResponseEntity.ok(response);
    }
}
