package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.CreateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.dto.SchemaResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.service.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing schema definitions.
 * <p>
 * Provides CRUD operations for reusable schema definitions in the OpenAPI
 * components/schemas section. All endpoints are prefixed with {@code /ouro/rest-specs/schemas}.
 *
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/ouro/rest-specs/schemas")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;

    /**
     * Creates a new schema definition.
     *
     * @param request schema details
     * @return standard API response with creation result
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<SchemaResponse>> createSchema(
            @RequestBody CreateSchemaRequest request) {
        try {
            SchemaResponse data = schemaService.createSchema(request);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.success(
                    data,
                    "Schema created successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid schema request: {}", e.getMessage(), e);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to create schema",
                    "INVALID_REQUEST",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Failed to create schema", e);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to create schema",
                    "INTERNAL_ERROR",
                    "An internal error occurred while creating the schema"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves all schema definitions.
     *
     * @return list of all schemas
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<SchemaResponse>>> getAllSchemas() {
        try {
            List<SchemaResponse> data = schemaService.getAllSchemas();

            if (data.isEmpty()) {
                log.info("No schemas found");
                GlobalApiResponse<List<SchemaResponse>> response = GlobalApiResponse.error(
                        HttpStatus.NOT_FOUND.value(),
                        "No schemas found",
                        "SCHEMA_NOT_FOUND",
                        "No schema definitions exist yet. Create your first schema to get started."
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            GlobalApiResponse<List<SchemaResponse>> response = GlobalApiResponse.success(
                    data,
                    "Schemas retrieved successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve schemas", e);
            GlobalApiResponse<List<SchemaResponse>> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve schemas",
                    "INTERNAL_ERROR",
                    "An internal error occurred while retrieving schemas"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves a specific schema by name.
     *
     * @param schemaName the schema identifier
     * @return schema details
     */
    @GetMapping("/{schemaName}")
    public ResponseEntity<GlobalApiResponse<SchemaResponse>> getSchema(
            @PathVariable("schemaName") String schemaName) {
        try {
            SchemaResponse data = schemaService.getSchema(schemaName);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.success(
                    data,
                    "Schema retrieved successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Schema not found: {}", schemaName, e);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Schema not found",
                    "SCHEMA_NOT_FOUND",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Failed to retrieve schema: {}", schemaName, e);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve schema",
                    "INTERNAL_ERROR",
                    "An internal error occurred while retrieving the schema"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Updates an existing schema definition.
     *
     * @param schemaName the schema identifier
     * @param request updated schema details
     * @return updated schema
     */
    @PutMapping("/{schemaName}")
    public ResponseEntity<GlobalApiResponse<SchemaResponse>> updateSchema(
            @PathVariable("schemaName") String schemaName,
            @RequestBody UpdateSchemaRequest request) {
        try {
            SchemaResponse data = schemaService.updateSchema(schemaName, request);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.success(
                    data,
                    "Schema updated successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Schema not found: {}", schemaName, e);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Schema not found",
                    "SCHEMA_NOT_FOUND",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Failed to update schema: {}", schemaName, e);
            GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to update schema",
                    "INTERNAL_ERROR",
                    "An internal error occurred while updating the schema"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Deletes a schema definition.
     *
     * @param schemaName the schema identifier
     * @return success response
     */
    @DeleteMapping("/{schemaName}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteSchema(
            @PathVariable("schemaName") String schemaName) {
        try {
            schemaService.deleteSchema(schemaName);
            GlobalApiResponse<Void> response = GlobalApiResponse.success(
                    null,
                    "Schema deleted successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Schema not found: {}", schemaName, e);
            GlobalApiResponse<Void> response = GlobalApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Schema not found",
                    "SCHEMA_NOT_FOUND",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Failed to delete schema: {}", schemaName, e);
            GlobalApiResponse<Void> response = GlobalApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to delete schema",
                    "INTERNAL_ERROR",
                    "An internal error occurred while deleting the schema"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
