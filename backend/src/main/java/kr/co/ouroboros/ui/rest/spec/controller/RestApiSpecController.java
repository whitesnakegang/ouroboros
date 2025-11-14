package kr.co.ouroboros.ui.rest.spec.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.ui.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.ui.rest.spec.dto.ImportValidationErrorData;
import kr.co.ouroboros.ui.rest.spec.dto.ImportYamlResponse;
import kr.co.ouroboros.ui.rest.spec.dto.RestApiSpecResponse;
import kr.co.ouroboros.ui.rest.spec.dto.UpdateRestApiRequest;
import kr.co.ouroboros.ui.rest.spec.dto.ValidationError;
import kr.co.ouroboros.core.rest.spec.service.RestApiSpecService;
import kr.co.ouroboros.core.rest.spec.validator.ImportRestYamlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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
    private final ImportRestYamlValidator importRestYamlValidator;

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

    /**
     * Syncs a cache-only API specification to the YAML file.
     * <p>
     * This endpoint is used when an API specification exists only in the cache (from code scanning)
     * but not in the YAML file. It adds the specification to the file so it can be edited via
     * the update endpoint.
     * <p>
     * If the specification already exists in the file, this operation is a no-op and returns
     * the existing specification.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param id specification UUID
     * @return synced specification details
     * @throws Exception if specification not found in cache or sync fails
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<GlobalApiResponse<RestApiSpecResponse>> syncToFile(
            @PathVariable("id") String id) throws Exception {
        RestApiSpecResponse data = restApiSpecService.syncToFile(id);
        GlobalApiResponse<RestApiSpecResponse> response = GlobalApiResponse.success(
                data,
                "REST API specification synced to file successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Imports external OpenAPI 3.1.0 YAML file and merges into ourorest.yml.
     * <p>
     * Validates the uploaded YAML file, handles duplicate APIs and schemas by auto-renaming,
     * enriches with Ouroboros custom fields, and updates $ref references accordingly.
     * <p>
     * <b>File Requirements:</b>
     * <ul>
     *   <li>Extension: .yml or .yaml</li>
     *   <li>Format: OpenAPI 3.1.0 specification</li>
     *   <li>Required fields: openapi, info (title, version), paths</li>
     *   <li>Valid HTTP methods: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD, TRACE</li>
     * </ul>
     * <p>
     * <b>Duplicate Handling:</b>
     * <ul>
     *   <li>APIs: Duplicate path+method combinations are renamed with "-import" suffix</li>
     *   <li>Schemas: Duplicate schema names are renamed with "-import" suffix</li>
     *   <li>$ref references are automatically updated to point to renamed schemas</li>
     * </ul>
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param file the OpenAPI YAML file to import
     * @return import result with counts and renamed items
     * @throws Exception if validation fails or import operation fails
     */
    @PostMapping("/import")
    public ResponseEntity<GlobalApiResponse<?>> importYaml(
            @RequestParam("file") MultipartFile file) throws Exception {

        log.info("Received YAML import request: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        // Step 1: Validate file extension
        String filename = file.getOriginalFilename();
        List<ValidationError> fileErrors = importRestYamlValidator.validateFileExtension(filename);
        if (!fileErrors.isEmpty()) {
            ValidationError error = fileErrors.get(0);
            GlobalApiResponse<ImportYamlResponse> response = GlobalApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    error.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Step 2: Read file content
        String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Step 3: Validate YAML content
        List<ValidationError> contentErrors = importRestYamlValidator.validate(yamlContent);
        if (!contentErrors.isEmpty()) {
            // Build error response with validation errors in data field
            ImportValidationErrorData errorData = ImportValidationErrorData.builder()
                    .validationErrors(contentErrors)
                    .build();

            String message = String.format("YAML validation failed with %d error%s",
                    contentErrors.size(),
                    contentErrors.size() > 1 ? "s" : "");

            GlobalApiResponse<ImportValidationErrorData> response = GlobalApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    message,
                    "VALIDATION_FAILED",
                    contentErrors.size() + " validation error" + (contentErrors.size() > 1 ? "s" : "") + " found in the uploaded YAML file"
            );
            response.setData(errorData);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Step 4: Import YAML
        ImportYamlResponse data = restApiSpecService.importYaml(yamlContent);

        GlobalApiResponse<ImportYamlResponse> response = GlobalApiResponse.success(
                data,
                "YAML import completed successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Exports the YAML file content.
     * <p>
     * Returns the current saved content from the ourorest.yml file directly.
     * The response is plain text (YAML format) for easy download.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @return YAML file content as plain text
     * @throws Exception if file reading fails or file does not exist
     */
    @GetMapping("/export/yaml")
    public ResponseEntity<String> exportYaml() throws Exception {
        String yamlContent = restApiSpecService.exportYaml();
        return ResponseEntity.ok()
                .header("Content-Type", "text/yaml; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"ourorest.yml\"")
                .body(yamlContent);
    }
}
