package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.RestApiSpecResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateRestApiRequest;

import java.util.List;

/**
 * Service interface for REST API specification management.
 * <p>
 * Manages REST API endpoint specifications in the OpenAPI paths section.
 * Supports full CRUD operations with automatic YAML file synchronization.
 *
 * @since 0.0.1
 */
public interface RestApiSpecService {

    /**
     * Creates a new REST API specification.
     * <p>
     * Validates uniqueness of path+method combination and generates a UUID if not provided.
     * Writes the specification to the OpenAPI YAML file.
     *
     * @param request REST API specification details
     * @return created specification with generated ID
     * @throws Exception if specification creation fails or duplicate path+method exists
     */
    RestApiSpecResponse createRestApiSpec(CreateRestApiRequest request) throws Exception;

    /**
     * Retrieves all REST API specifications.
     * <p>
     * Reads all API specifications from the OpenAPI YAML file.
     * Returns an empty list if no specifications exist.
     *
     * @return list of all specifications
     * @throws Exception if file reading fails
     */
    List<RestApiSpecResponse> getAllRestApiSpecs() throws Exception;

    /**
     * Retrieves a specific REST API specification by ID.
     * <p>
     * Searches through all paths and methods to find the specification with the given ID.
     *
     * @param id specification UUID
     * @return specification details
     * @throws Exception if specification not found or file reading fails
     */
    RestApiSpecResponse getRestApiSpec(String id) throws Exception;

    /**
     * Updates an existing REST API specification.
     * <p>
     * Only provided fields will be updated. Path and method cannot be changed
     * (use delete + create instead to change these).
     *
     * @param id specification UUID
     * @param request updated specification details
     * @return updated specification
     * @throws Exception if specification not found or update fails
     */
    RestApiSpecResponse updateRestApiSpec(String id, UpdateRestApiRequest request) throws Exception;

    /**
     * Deletes a REST API specification.
     * <p>
     * Removes the specification from the OpenAPI YAML file.
     *
     * @param id specification UUID
     * @throws Exception if specification not found or deletion fails
     */
    void deleteRestApiSpec(String id) throws Exception;
}
