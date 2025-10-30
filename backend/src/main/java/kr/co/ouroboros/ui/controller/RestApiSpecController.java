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
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param request API specification details
     * @return created specification with generated ID
     * @throws Exception if creation fails
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> createRestApiSpec(
            @RequestBody CreateRestApiRequest request) throws Exception {
        RestApiSpecResponse data = restApiSpecService.createRestApiSpec(request);
        GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                data,
                "REST API specification created successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all REST API specifications.
     * <p>
     * Returns a list of all API specifications from the OpenAPI YAML file.
     * Returns an empty list if no specifications exist.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @return list of all specifications
     * @throws Exception if retrieval fails
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<RestApiSpecResponse>>> getAllRestApiSpecs() throws Exception {
        List<RestApiSpecResponse> data = restApiSpecService.getAllRestApiSpecs();
        GlobalApiResponse<List<RestApiSpecResponse>> response = GlobalApiResponse.success(
                data,
                "REST API specifications retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific REST API specification by ID.
     * <p>
     * Searches through all paths and methods to find the specification with the given UUID.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param id specification UUID
     * @return specification details
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> getRestApiSpec(
            @PathVariable("id") String id) throws Exception {
        RestApiSpecResponse data = restApiSpecService.getRestApiSpec(id);
        GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                data,
                "REST API specification retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing REST API specification.
     * <p>
     * Only provided fields will be updated. Path and method cannot be changed
     * (use delete + create instead to change these).
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param id specification UUID
     * @param request updated specification details
     * @return updated specification
     * @throws Exception if update fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> updateRestApiSpec(
            @PathVariable("id") String id,
            @RequestBody UpdateRestApiRequest request) throws Exception {
        RestApiSpecResponse data = restApiSpecService.updateRestApiSpec(id, request);
        GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                data,
                "REST API specification updated successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a REST API specification.
     * <p>
     * Removes the specification from the OpenAPI YAML file.
     * If the path has no remaining methods, the entire path is removed.
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @throws Exception if retrieval fails
     * @param id specification UUID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteRestApiSpec(
            @PathVariable("id") String id) throws Exception {

            restApiSpecService.deleteRestApiSpec(id);
            GlobalApiResponse<Void> response = GlobalApiResponse.success(
                    null,
                    "REST API specification deleted successfully"
            );
            return ResponseEntity.ok(response);



    }
}
