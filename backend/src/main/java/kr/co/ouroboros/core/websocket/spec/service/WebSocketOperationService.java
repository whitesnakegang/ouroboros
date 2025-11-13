package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.ui.websocket.spec.dto.CreateOperationRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.ImportYamlResponse;
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

    /**
     * Imports external AsyncAPI 3.0.0 YAML file and merges into ourowebsocket.yml.
     * <p>
     * Validates the uploaded YAML file, handles duplicate channels/operations/schemas/messages by auto-renaming,
     * enriches with Ouroboros custom fields, and updates $ref references accordingly.
     * <p>
     * <b>File Requirements:</b>
     * <ul>
     *   <li>Extension: .yml or .yaml</li>
     *   <li>Format: AsyncAPI 3.0.0 specification</li>
     *   <li>Required fields: asyncapi, info (title, version), channels</li>
     *   <li>Valid action types: send, receive</li>
     * </ul>
     * <p>
     * <b>Duplicate Handling:</b>
     * <ul>
     *   <li>Channels: Duplicate channel names are renamed with "-import" suffix</li>
     *   <li>Operations: Duplicate operation names are renamed with "-import" suffix</li>
     *   <li>Schemas: Duplicate schema names are renamed with "-import" suffix</li>
     *   <li>Messages: Duplicate message names are renamed with "-import" suffix</li>
     *   <li>$ref references are automatically updated to point to renamed components</li>
     * </ul>
     *
     * @param yamlContent the AsyncAPI YAML content to import
     * @return import result with counts and renamed items
     * @throws Exception if validation fails or import operation fails
     */
    ImportYamlResponse importYaml(String yamlContent) throws Exception;

    /**
     * Exports the YAML file content as a string.
     * <p>
     * Reads the current saved content from the ourowebsocket.yml file directly.
     *
     * @return YAML file content as a string
     * @throws Exception if file reading fails or file does not exist
     */
    String exportYaml() throws Exception;
}



