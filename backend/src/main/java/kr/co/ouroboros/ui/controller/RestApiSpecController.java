package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.RestApiSpecResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing REST API specifications.
 * <p>
 * Provides CRUD operations for REST API endpoint specifications.
 * All endpoints are prefixed with {@code /ouro/rest-specs}.
 * All responses follow the standard {@link GlobalApiResponse} format.
 *
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/ouro/rest-specs")
@RequiredArgsConstructor
public class RestApiSpecController {

    private final RestApiSpecService restApiSpecService;

    /**
     * Creates a new REST API specification.
     * <p>
     * Validates uniqueness of path+method combination and automatically generates
     * a UUID if not provided. Supports both schema references and inline schemas.
     *
     * @param request API specification details
     * @return created specification with generated ID
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> createRestApiSpec(
            @RequestBody CreateRestApiRequest request) {
        try {
            RestApiSpecResponse data = restApiSpecService.createRestApiSpec(request);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                    data,
                    "REST API specification created successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid REST API spec request: {}", e.getMessage(), e);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to create REST API specification",
                    "INVALID_REQUEST",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Failed to create REST API specification", e);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to create REST API specification",
                    "INTERNAL_ERROR",
                    "An internal error occurred while creating the specification"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves all REST API specifications.
     * <p>
     * Returns a list of all API specifications from the OpenAPI YAML file.
     * Returns an empty list if no specifications exist.
     *
     * @return list of all specifications
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<RestApiSpecResponse>>> getAllRestApiSpecs() {
        try {
            List<RestApiSpecResponse> data = restApiSpecService.getAllRestApiSpecs();

            if (data.isEmpty()) {
                log.info("No REST API specifications found");
                GlobalApiResponse<List<RestApiSpecResponse>> response = GlobalApiResponse.error(
                        HttpStatus.NOT_FOUND.value(),
                        "No REST API specifications found",
                        "SPEC_NOT_FOUND",
                        "No API specifications exist yet. Create your first specification to get started."
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            GlobalApiResponse<List<RestApiSpecResponse>> response = GlobalApiResponse.success(
                    data,
                    "REST API specifications retrieved successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve REST API specifications", e);
            GlobalApiResponse<List<RestApiSpecResponse>> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve REST API specifications",
                    "INTERNAL_ERROR",
                    "An internal error occurred while retrieving specifications"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves a specific REST API specification by ID.
     * <p>
     * Searches through all paths and methods to find the specification with the given UUID.
     *
     * @param id specification UUID
     * @return specification details
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> getRestApiSpec(
            @PathVariable("id") String id) {
        try {
            RestApiSpecResponse data = restApiSpecService.getRestApiSpec(id);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                    data,
                    "REST API specification retrieved successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("REST API specification not found: {}", id, e);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "REST API specification not found",
                    "SPEC_NOT_FOUND",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Failed to retrieve REST API specification: {}", id, e);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve REST API specification",
                    "INTERNAL_ERROR",
                    "An internal error occurred while retrieving the specification"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Updates an existing REST API specification.
     * <p>
     * Only provided fields will be updated. Path and method cannot be changed
     * (use delete + create instead to change these).
     *
     * @param id specification UUID
     * @param request updated specification details
     * @return updated specification
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> updateRestApiSpec(
            @PathVariable("id") String id,
            @RequestBody UpdateRestApiRequest request) {
        try {
            RestApiSpecResponse data = restApiSpecService.updateRestApiSpec(id, request);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                    data,
                    "REST API specification updated successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("REST API specification not found: {}", id, e);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "REST API specification not found",
                    "SPEC_NOT_FOUND",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Failed to update REST API specification: {}", id, e);
            GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to update REST API specification",
                    "INTERNAL_ERROR",
                    "An internal error occurred while updating the specification"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Deletes a REST API specification.
     * <p>
     * Removes the specification from the OpenAPI YAML file.
     * If the path has no remaining methods, the entire path is removed.
     *
     * @param id specification UUID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteRestApiSpec(
            @PathVariable("id") String id) {
        try {
            restApiSpecService.deleteRestApiSpec(id);
            GlobalApiResponse<Void> response = GlobalApiResponse.success(
                    null,
                    "REST API specification deleted successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("REST API specification not found: {}", id, e);
            GlobalApiResponse<Void> response = GlobalApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "REST API specification not found",
                    "SPEC_NOT_FOUND",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Failed to delete REST API specification: {}", id, e);
            GlobalApiResponse<Void> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to delete REST API specification",
                    "INTERNAL_ERROR",
                    "An internal error occurred while deleting the specification"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
