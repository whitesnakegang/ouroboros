package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.global.exception.DuplicateApiSpecException;
import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.GetRestApiSpecsResponse;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing REST API specifications.
 * <p>
 * Provides endpoints for creating, validating, and managing REST API specifications
 * in OpenAPI 3.1.0 format. All endpoints are prefixed with {@code /ouro/rest-specs}.
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
     * Accepts API specification details via JSON and generates an OpenAPI 3.1.0 YAML file.
     * Automatically generates a UUID if not provided in the request.
     *
     * @param request the API specification details (JSON format only)
     * @return standard API response with creation result
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CreateRestApiResponse>> createRestApiSpec(
            @RequestBody CreateRestApiRequest request) {
        try {
            CreateRestApiResponse data = restApiSpecService.createRestApiSpec(request);
            GlobalApiResponse<CreateRestApiResponse> response = GlobalApiResponse.success(
                    data,
                    "REST API specification created successfully"
            );
            return ResponseEntity.ok(response);
        } catch (DuplicateApiSpecException e) {
            log.warn("Duplicate API specification: {} {}", e.getMethod(), e.getPath(), e);
            GlobalApiResponse<CreateRestApiResponse> response = GlobalApiResponse.error(
                    HttpStatus.CONFLICT.value(),
                    "Failed to create REST API specification",
                    "DUPLICATE_API",
                    "The API specification for this path and method already exists"
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("Failed to create REST API specification", e);
            GlobalApiResponse<CreateRestApiResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to create REST API specification",
                    "INTERNAL_ERROR",
                    "An internal error occurred while creating the API specification"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves all REST API specifications.
     * <p>
     * Reads the ourorest.yml file and returns server information and summary data
     * for all API specifications. Returns 404 if the file does not exist.
     *
     * @return server information and all API specification summaries
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<GetRestApiSpecsResponse>> getAllRestApiSpecs() {
        try {
            GetRestApiSpecsResponse data = restApiSpecService.getAllRestApiSpecs();

            // Return 404 if no specifications exist (file not found or empty)
            if (data.getSpecs() == null || data.getSpecs().isEmpty()) {
                log.info("No API specifications found");
                GlobalApiResponse<GetRestApiSpecsResponse> response = GlobalApiResponse.error(
                        HttpStatus.NOT_FOUND.value(),
                        "No API specifications found",
                        "SPEC_NOT_FOUND",
                        "No API specification file exists yet. Create your first API specification to get started."
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            GlobalApiResponse<GetRestApiSpecsResponse> response = GlobalApiResponse.success(
                    data,
                    "REST API specifications retrieved successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve REST API specifications", e);
            GlobalApiResponse<GetRestApiSpecsResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve REST API specifications",
                    "INTERNAL_ERROR",
                    "An internal error occurred while retrieving API specifications"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
