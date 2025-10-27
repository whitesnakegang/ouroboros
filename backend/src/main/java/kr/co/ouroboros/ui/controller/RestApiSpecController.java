package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.global.exception.DuplicateApiSpecException;
import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import lombok.RequiredArgsConstructor;
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
            GlobalApiResponse<CreateRestApiResponse> response = GlobalApiResponse.error(
                    HttpStatus.CONFLICT.value(),
                    "Failed to create REST API specification",
                    "DUPLICATE_API",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            GlobalApiResponse<CreateRestApiResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to create REST API specification",
                    "INTERNAL_ERROR",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
