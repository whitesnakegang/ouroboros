package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.ui.websocket.spec.dto.CreateOperationRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.OperationResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateOperationRequest;

import java.util.List;

/**
 * Service interface for WebSocket operation management operations.
 * <p>
 * Manages operations in the AsyncAPI operations section.
 * Operations define send/receive actions on channels with reply configurations.
 *
 * @since 0.1.0
 */
public interface WebSocketOperationService {

    /**
     * Creates new operations from receive and reply channel configurations.
     * <p>
     * Generates all combinations of receives × replies as separate operations.
     * Automatically creates channels if they don't exist.
     * Each operation gets a unique x-ouroboros-id (UUID).
     *
     * @param request contains receive and reply channel configurations
     * @return list of created operations
     * @throws Exception if operation creation fails
     */
    List<OperationResponse> createOperations(CreateOperationRequest request) throws Exception;

    /**
     * Retrieves all operations.
     *
     * @return list of all operations
     * @throws Exception if file reading fails
     */
    List<OperationResponse> getAllOperations() throws Exception;

    /**
     * Retrieves a specific operation by id.
     *
     * @param id the x-ouroboros-id (UUID)
     * @return operation details
     * @throws Exception if operation not found or file reading fails
     */
    OperationResponse getOperation(String id) throws Exception;

    /**
     * Updates an existing operation.
     *
     * @param id the x-ouroboros-id (UUID)
     * @param request updated operation details
     * @return updated operation
     * @throws Exception if operation not found or update fails
     */
    OperationResponse updateOperation(String id, UpdateOperationRequest request) throws Exception;

    /**
     * Deletes an operation.
     *
     * @param id the x-ouroboros-id (UUID)
     * @throws Exception if operation not found or deletion fails
     */
    void deleteOperation(String id) throws Exception;
}

