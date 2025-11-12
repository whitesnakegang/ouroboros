package kr.co.ouroboros.ui.websocket.spec.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.websocket.spec.service.WebSocketOperationService;
import kr.co.ouroboros.ui.websocket.spec.dto.CreateOperationRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.OperationResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateOperationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing WebSocket operation definitions.
 * <p>
 * Provides CRUD operations for operations in the AsyncAPI operations section.
 * Operations define send/receive actions on channels with reply configurations.
 * All endpoints are prefixed with {@code /ouro/websocket-specs/operations}.
 *
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequestMapping("/ouro/websocket-specs/operations")
@RequiredArgsConstructor
public class WebSocketOperationController {

    private final WebSocketOperationService operationService;

    /**
     * Creates new operations from receive and reply channel configurations.
     * <p>
     * Generates all combinations of receives × replies as separate operations.
     * Automatically creates channels if they don't exist.
     * Each operation gets a unique x-ouroboros-id (UUID).
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param request operation creation details (receives and replies)
     * @return standard API response with list of created operations
     * @throws Exception if creation fails
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<List<OperationResponse>>> createOperations(
            @RequestBody CreateOperationRequest request) throws Exception {
        List<OperationResponse> data = operationService.createOperations(request);
        GlobalApiResponse<List<OperationResponse>> response = GlobalApiResponse.success(
                data,
                "Operations created successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all operation definitions.
     * <p>
     * Returns a list of all operations from the AsyncAPI YAML file.
     * Returns an empty list if no operations exist.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @return list of all operations
     * @throws Exception if retrieval fails
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<OperationResponse>>> getAllOperations() throws Exception {
        List<OperationResponse> data = operationService.getAllOperations();
        GlobalApiResponse<List<OperationResponse>> response = GlobalApiResponse.success(
                data,
                "Operations retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific operation by id.
     * <p>
     * Searches the operations section for the specified x-ouroboros-id.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param id the x-ouroboros-id (UUID)
     * @return operation details
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<OperationResponse>> getOperation(
            @PathVariable("id") String id) throws Exception {
        OperationResponse data = operationService.getOperation(id);
        GlobalApiResponse<OperationResponse> response = GlobalApiResponse.success(
                data,
                "Operation retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing operation definition.
     * <p>
     * Only provided fields will be updated. Existing values are preserved if not specified.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param id the x-ouroboros-id (UUID)
     * @param request updated operation details
     * @return updated operation
     * @throws Exception if update fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<OperationResponse>> updateOperation(
            @PathVariable("id") String id,
            @RequestBody UpdateOperationRequest request) throws Exception {
        OperationResponse data = operationService.updateOperation(id, request);
        GlobalApiResponse<OperationResponse> response = GlobalApiResponse.success(
                data,
                "Operation updated successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an operation definition.
     * <p>
     * Removes the operation from the AsyncAPI operations section.
     * <p>
     * Exceptions are handled by a global exception handler.
     *
     * @param id the x-ouroboros-id (UUID)
     * @return success response
     * @throws Exception if deletion fails
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteOperation(
            @PathVariable("id") String id) throws Exception {
        operationService.deleteOperation(id);
        GlobalApiResponse<Void> response = GlobalApiResponse.success(
                null,
                "Operation deleted successfully"
        );
        return ResponseEntity.ok(response);
    }
}

