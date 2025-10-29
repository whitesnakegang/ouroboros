package kr.co.ouroboros.ui.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.CreateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.dto.SchemaResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.service.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param request schema details
     * @return standard API response with creation result
     * @throws Exception if creation fails
     */
    @PostMapping
    public ResponseEntity<GlobalApiResponse<SchemaResponse>> createSchema(
            @RequestBody CreateSchemaRequest request) throws Exception {
        SchemaResponse data = schemaService.createSchema(request);
        GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.success(
                data,
                "Schema created successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all schema definitions.
     * <p>
     * Returns a list of all schema definitions from the OpenAPI YAML file.
     * Returns an empty list if no schemas exist.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @return list of all schemas
     * @throws Exception if retrieval fails
     */
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<SchemaResponse>>> getAllSchemas() throws Exception {
        List<SchemaResponse> data = schemaService.getAllSchemas();
        GlobalApiResponse<List<SchemaResponse>> response = GlobalApiResponse.success(
                data,
                "Schemas retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific schema by name.
     * <p>
     * Searches the components/schemas section for the specified schema name.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param schemaName the schema identifier
     * @return schema details
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{schemaName}")
    public ResponseEntity<GlobalApiResponse<SchemaResponse>> getSchema(
            @PathVariable("schemaName") String schemaName) throws Exception {
        SchemaResponse data = schemaService.getSchema(schemaName);
        GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.success(
                data,
                "Schema retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing schema definition.
     * <p>
     * Only provided fields will be updated. Existing values are preserved if not specified.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param schemaName the schema identifier
     * @param request updated schema details
     * @return updated schema
     * @throws Exception if update fails
     */
    @PutMapping("/{schemaName}")
    public ResponseEntity<GlobalApiResponse<SchemaResponse>> updateSchema(
            @PathVariable("schemaName") String schemaName,
            @RequestBody UpdateSchemaRequest request) throws Exception {
        SchemaResponse data = schemaService.updateSchema(schemaName, request);
        GlobalApiResponse<SchemaResponse> response = GlobalApiResponse.success(
                data,
                "Schema updated successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a schema definition.
     * <p>
     * Removes the schema from the OpenAPI components/schemas section.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler}.
     *
     * @param schemaName the schema identifier
     * @return success response
     * @throws Exception if deletion fails
     */
    @DeleteMapping("/{schemaName}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteSchema(
            @PathVariable("schemaName") String schemaName) throws Exception {
        schemaService.deleteSchema(schemaName);
        GlobalApiResponse<Void> response = GlobalApiResponse.success(
                null,
                "Schema deleted successfully"
        );
        return ResponseEntity.ok(response);
    }
}
