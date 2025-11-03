package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.spec.dto.CreateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.dto.SchemaResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateSchemaRequest;

import java.util.List;

/**
 * Service interface for schema management operations.
 * <p>
 * Manages reusable schema definitions in the OpenAPI components/schemas section.
 *
 * @since 0.0.1
 */
public interface SchemaService {

    /**
     * Creates a new schema definition.
     *
     * @param request schema details
     * @return created schema with metadata
     * @throws Exception if schema creation fails or schema already exists
     */
    SchemaResponse createSchema(CreateSchemaRequest request) throws Exception;

    /**
     * Retrieves all schema definitions.
     *
     * @return list of all schemas
     * @throws Exception if file reading fails
     */
    List<SchemaResponse> getAllSchemas() throws Exception;

    /**
     * Retrieves a specific schema by name.
     *
     * @param schemaName the schema identifier
     * @return schema details
     * @throws Exception if schema not found or file reading fails
     */
    SchemaResponse getSchema(String schemaName) throws Exception;

    /**
     * Updates an existing schema definition.
     *
     * @param schemaName the schema identifier
     * @param request updated schema details
     * @return updated schema
     * @throws Exception if schema not found or update fails
     */
    SchemaResponse updateSchema(String schemaName, UpdateSchemaRequest request) throws Exception;

    /**
     * Deletes a schema definition.
     *
     * @param schemaName the schema identifier
     * @throws Exception if schema not found or deletion fails
     */
    void deleteSchema(String schemaName) throws Exception;
}