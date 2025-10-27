package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing REST API specifications.
 * <p>
 * Provides endpoints for creating, validating, and managing REST API specifications
 * in OpenAPI 3.1.0 format. All endpoints are prefixed with {@code /ouro/rest-specs}.
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
     * Accepts API specification details and generates an OpenAPI 3.1.0 YAML file.
     * Returns 200 OK on success or 500 Internal Server Error on failure (including duplicate detection).
     *
     * @param request the API specification details
     * @return response entity containing creation result and file path
     */
    @PostMapping
    public ResponseEntity<CreateRestApiResponse> createRestApiSpec(
            @RequestBody CreateRestApiRequest request) {
        try {
            CreateRestApiResponse response = restApiSpecService.createRestApiSpec(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            CreateRestApiResponse errorResponse = CreateRestApiResponse.builder()
                    .success(false)
                    .message("Failed to create REST API specification: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
