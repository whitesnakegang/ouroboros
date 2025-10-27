package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;

/**
 * Service interface for REST API specification management.
 * <p>
 * Provides operations for creating, validating, and persisting REST API specifications
 * in OpenAPI 3.1.0 format.
 *
 * @since 0.0.1
 */
public interface RestApiSpecService {

    /**
     * Creates a new REST API specification and writes it to YAML file.
     * <p>
     * Converts the request DTO to domain model, validates uniqueness of path+method combination,
     * and generates an OpenAPI 3.1.0 YAML file in the resources directory.
     *
     * @param request the REST API specification details
     * @return response containing success status and file path
     * @throws Exception if specification creation fails or duplicate path+method exists
     */
    CreateRestApiResponse createRestApiSpec(CreateRestApiRequest request) throws Exception;
}
