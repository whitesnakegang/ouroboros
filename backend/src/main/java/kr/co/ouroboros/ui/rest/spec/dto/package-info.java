/**
 * REST API specification data transfer objects package.
 * <p>
 * Contains DTOs for REST API specification operations, primarily used for
 * communication between the presentation layer (controllers) and service layer.
 * <ul>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.CreateRestApiRequest} - Request DTO for creating API specs</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.CreateRestApiResponse} - Response DTO for API spec creation</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.UpdateRestApiRequest} - Request DTO for updating API specs</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.RestApiSpecResponse} - Response DTO for API spec operations</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.GetRestApiSpecsResponse} - Response DTO for retrieving all API specs</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.CreateSchemaRequest} - Request DTO for creating schemas</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.UpdateSchemaRequest} - Request DTO for updating schemas</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.SchemaResponse} - Response DTO for schema operations</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.ImportYamlResponse} - Response DTO for YAML import operations</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.ImportValidationErrorData} - Data wrapper for YAML import validation errors</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.ValidationError} - Validation error details for YAML import</li>
 *   <li>{@link kr.co.ouroboros.ui.rest.spec.dto.RenamedItem} - Information about items renamed during import</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.ui.rest.spec.dto;
