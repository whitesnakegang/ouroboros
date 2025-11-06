package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
import kr.co.ouroboros.core.rest.spec.model.*;
import kr.co.ouroboros.core.rest.spec.validator.SchemaValidator;
import kr.co.ouroboros.ui.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.ui.rest.spec.dto.ImportYamlResponse;
import kr.co.ouroboros.ui.rest.spec.dto.RenamedItem;
import kr.co.ouroboros.ui.rest.spec.dto.RestApiSpecResponse;
import kr.co.ouroboros.ui.rest.spec.dto.UpdateRestApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link RestApiSpecService}.
 * <p>
 * Manages REST API specifications in the OpenAPI paths section of ourorest.yml.
 * Uses {@link RestApiYamlParser} for all YAML file operations.
 * Each specification is identified by a UUID stored in the x-ouroboros-id extension.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestApiSpecServiceimpl implements RestApiSpecService {

    private final RestApiYamlParser yamlParser;
    private final SchemaValidator schemaValidator;
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final List<String> HTTP_METHODS = Arrays.asList(
            "get", "post", "put", "delete", "patch", "options", "head", "trace"
    );

    @Override
    public RestApiSpecResponse createRestApiSpec(CreateRestApiRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            // Generate UUID if not provided
            String id = request.getId() != null ? request.getId() : UUID.randomUUID().toString();

            // Read existing document or create new one
            Map<String, Object> openApiDoc = yamlParser.readOrCreateDocument();

            // Check for duplicate path+method
            if (yamlParser.operationExists(openApiDoc, request.getPath(), request.getMethod())) {
                throw new IllegalArgumentException(
                        "API specification already exists for " + request.getMethod().toUpperCase() + " " + request.getPath()
                );
            }

            // Build operation definition
            Map<String, Object> operation = buildOperation(id, request);

            // Add operation to document
            yamlParser.putOperation(openApiDoc, request.getPath(), request.getMethod(), operation);

            // Auto-create security schemes if security is specified
            if (request.getSecurity() != null && !request.getSecurity().isEmpty()) {
                autoCreateSecuritySchemes(openApiDoc, request.getSecurity());
                
                // Debug: Verify securitySchemes were added
                @SuppressWarnings("unchecked")
                Map<String, Object> components = (Map<String, Object>) openApiDoc.get("components");
                if (components != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");
                    if (securitySchemes != null) {
                        log.info("✓ SecuritySchemes in openApiDoc before save: {}", securitySchemes.keySet());
                    } else {
                        log.warn("⚠️ SecuritySchemes is null after autoCreate!");
                    }
                }
            }

            // Validate and auto-create missing schema references
            int createdSchemas = schemaValidator.validateAndCreateMissingSchemas(openApiDoc);
            if (createdSchemas > 0) {
                log.info("Auto-created {} missing schema(s)", createdSchemas);
            }

            // Process and cache: writes to file + validates with scanned state + updates cache
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            log.info("Created REST API spec: {} {} (ID: {})", request.getMethod().toUpperCase(), request.getPath(), id);

            return convertToResponse(id, request.getPath(), request.getMethod(), operation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<RestApiSpecResponse> getAllRestApiSpecs() throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                return new ArrayList<>();
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
            Map<String, Object> paths = yamlParser.getOrCreatePaths(openApiDoc);

            List<RestApiSpecResponse> responses = new ArrayList<>();

            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();

                for (Map.Entry<String, Object> methodEntry : methods.entrySet()) {
                    String method = methodEntry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();

                    String id = (String) operation.get("x-ouroboros-id");
                    if (id != null) {
                        responses.add(convertToResponse(id, path, method, operation));
                    }
                }
            }

            return responses;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public RestApiSpecResponse getRestApiSpec(String id) throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No API specifications found. The specification file does not exist.");
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
            Map<String, Object> paths = yamlParser.getOrCreatePaths(openApiDoc);

            // Search for operation with matching ID
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();

                for (Map.Entry<String, Object> methodEntry : methods.entrySet()) {
                    String method = methodEntry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();

                    String operationId = (String) operation.get("x-ouroboros-id");
                    if (id.equals(operationId)) {
                        return convertToResponse(id, path, method, operation);
                    }
                }
            }

            throw new IllegalArgumentException("REST API specification with ID '" + id + "' not found");
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public RestApiSpecResponse updateRestApiSpec(String id, UpdateRestApiRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No API specifications found. The specification file does not exist.");
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
            Map<String, Object> paths = yamlParser.getOrCreatePaths(openApiDoc);

            // Find operation with matching ID
            String foundPath = null;
            String foundMethod = null;
            Map<String, Object> operation = null;

            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();

                for (Map.Entry<String, Object> methodEntry : methods.entrySet()) {
                    String method = methodEntry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> op = (Map<String, Object>) methodEntry.getValue();

                    String operationId = (String) op.get("x-ouroboros-id");
                    if (id.equals(operationId)) {
                        foundPath = path;
                        foundMethod = method;
                        operation = op;
                        break;
                    }
                }
                if (operation != null) break;
            }

            if (operation == null) {
                throw new IllegalArgumentException("REST API specification with ID '" + id + "' not found");
            }

            // Determine final path and method
            String finalPath = request.getPath() != null ? request.getPath() : foundPath;
            String finalMethod = request.getMethod() != null ? request.getMethod() : foundMethod;

            // Check if path or method is changing
            boolean isLocationChanging = !foundPath.equals(finalPath) || !foundMethod.equalsIgnoreCase(finalMethod);

            if (isLocationChanging) {
                // Check for duplicate at new location
                if (yamlParser.operationExists(openApiDoc, finalPath, finalMethod)) {
                    throw new IllegalArgumentException(
                            "Cannot move operation: API specification already exists for " +
                            finalMethod.toUpperCase() + " " + finalPath
                    );
                }

                // Remove from old location
                yamlParser.removeOperation(openApiDoc, foundPath, foundMethod);

                // Update operation fields before moving
                updateOperationFields(operation, request);

                // Add to new location
                yamlParser.putOperation(openApiDoc, finalPath, finalMethod, operation);

                log.info("Moved REST API spec from {} {} to {} {} (ID: {})",
                        foundMethod.toUpperCase(), foundPath,
                        finalMethod.toUpperCase(), finalPath, id);
            } else {
                // Update only provided fields (no location change)
                updateOperationFields(operation, request);

                log.info("Updated REST API spec: {} {} (ID: {})", foundMethod.toUpperCase(), foundPath, id);
            }

            // Auto-create security schemes if security is updated
            if (request.getSecurity() != null && !request.getSecurity().isEmpty()) {
                autoCreateSecuritySchemes(openApiDoc, request.getSecurity());
            }

            // Validate and auto-create missing schema references
            int createdSchemas = schemaValidator.validateAndCreateMissingSchemas(openApiDoc);
            if (createdSchemas > 0) {
                log.info("Auto-created {} missing schema(s)", createdSchemas);
            }

            // Process and cache: writes to file + validates with scanned state + updates cache
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            return convertToResponse(id, finalPath, finalMethod, operation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteRestApiSpec(String id) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No API specifications found. The specification file does not exist.");
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
            Map<String, Object> paths = yamlParser.getOrCreatePaths(openApiDoc);

            // Find and remove operation with matching ID
            boolean found = false;
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();

                for (Map.Entry<String, Object> methodEntry : methods.entrySet()) {
                    String method = methodEntry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();

                    String operationId = (String) operation.get("x-ouroboros-id");
                    if (id.equals(operationId)) {
                        yamlParser.removeOperation(openApiDoc, path, method);
                        found = true;
                        log.info("Deleted REST API spec: {} {} (ID: {})", method.toUpperCase(), path, id);
                        break;
                    }
                }
                if (found) break;
            }

            if (!found) {
                throw new IllegalArgumentException("REST API specification with ID '" + id + "' not found");
            }

            // Process and cache: writes to file + validates with scanned state + updates cache
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Helper methods

    private Map<String, Object> buildOperation(String id, CreateRestApiRequest request) {
        Map<String, Object> operation = new LinkedHashMap<>();

        // Add standard OpenAPI fields
        if (request.getSummary() != null) {
            operation.put("summary", request.getSummary());
        }
        if (request.getDescription() != null) {
            operation.put("description", request.getDescription());
        }

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            operation.put("tags", request.getTags());
        }

        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            operation.put("parameters", convertParameters(request.getParameters()));
        }

        if (request.getRequestBody() != null) {
            operation.put("requestBody", convertRequestBody(request.getRequestBody()));
        }

        if (request.getResponses() != null && !request.getResponses().isEmpty()) {
            operation.put("responses", convertResponses(request.getResponses()));
        } else {
            operation.put("responses", new LinkedHashMap<>());
        }

        if (request.getSecurity() != null && !request.getSecurity().isEmpty()) {
            List<Map<String, List<String>>> securityList = convertSecurity(request.getSecurity());
            operation.put("security", securityList);
            log.info("✓ Security added to operation: {}", securityList);
        }

        // Add Ouroboros custom fields (auto-generated)
        operation.put("x-ouroboros-id", id);
        operation.put("x-ouroboros-progress", "mock");  // Always start as "mock"
        operation.put("x-ouroboros-tag", "none");       // Always start as "none"
        operation.put("x-ouroboros-diff", "none");      // Always start as "none"

        return operation;
    }

    private void updateOperationFields(Map<String, Object> operation, UpdateRestApiRequest request) {
        if (request.getSummary() != null) {
            operation.put("summary", request.getSummary());
        }
        if (request.getDescription() != null) {
            operation.put("description", request.getDescription());
        }

        if (request.getTags() != null) {
            operation.put("tags", request.getTags());
        }
        if (request.getParameters() != null) {
            operation.put("parameters", convertParameters(request.getParameters()));
        }
        if (request.getRequestBody() != null) {
            operation.put("requestBody", convertRequestBody(request.getRequestBody()));
        }
        if (request.getResponses() != null) {
            operation.put("responses", convertResponses(request.getResponses()));
        }
        if (request.getSecurity() != null && !request.getSecurity().isEmpty()) {
            List<Map<String, List<String>>> securityList = convertSecurity(request.getSecurity());
            operation.put("security", securityList);
            log.info("✓ Security updated in operation: {}", securityList);
        }

        // Reset x-ouroboros-diff to "none" when user explicitly updates the spec
        // This indicates the user has acknowledged the endpoint and it's no longer a "diff"
        operation.put("x-ouroboros-diff", "none");

        // Note: progress and tag are NOT updated via API
        // They are managed internally or by YAML parser
    }

    private List<Map<String, Object>> convertParameters(List<Parameter> parameters) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Parameter param : parameters) {
            Map<String, Object> paramMap = new LinkedHashMap<>();
            paramMap.put("name", param.getName());
            paramMap.put("in", param.getIn());
            if (param.getDescription() != null) {
                paramMap.put("description", param.getDescription());
            }
            paramMap.put("required", param.isRequired());
            if (param.getSchema() != null) {
                paramMap.put("schema", convertSchema(param.getSchema()));
            }
            result.add(paramMap);
        }
        return result;
    }

    private Map<String, Object> convertRequestBody(RequestBody requestBody) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (requestBody.getDescription() != null) {
            result.put("description", requestBody.getDescription());
        }
        result.put("required", requestBody.isRequired());
        if (requestBody.getContent() != null) {
            result.put("content", convertContent(requestBody.getContent()));
        }
        return result;
    }

    private Map<String, Object> convertResponses(Map<String, ApiResponse> responses) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ApiResponse> entry : responses.entrySet()) {
            Map<String, Object> responseMap = new LinkedHashMap<>();
            ApiResponse response = entry.getValue();

            if (response.getDescription() != null) {
                responseMap.put("description", response.getDescription());
            }
            if (response.getContent() != null) {
                responseMap.put("content", convertContent(response.getContent()));
            }
            if (response.getHeaders() != null) {
                responseMap.put("headers", convertHeaders(response.getHeaders()));
            }
            result.put(entry.getKey(), responseMap);
        }
        return result;
    }

    private Map<String, Object> convertContent(Map<String, MediaType> content) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            Map<String, Object> mediaTypeMap = new LinkedHashMap<>();
            if (entry.getValue().getSchema() != null) {
                mediaTypeMap.put("schema", convertSchema(entry.getValue().getSchema()));
            }
            result.put(entry.getKey(), mediaTypeMap);
        }
        return result;
    }

    private Map<String, Object> convertSchema(Schema schema) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Handle schema reference
        if (schema.getRef() != null && !schema.getRef().isBlank()) {
            // Convert to full $ref format for YAML
            String fullRef = schema.getRef().startsWith("#/components/schemas/")
                    ? schema.getRef()
                    : "#/components/schemas/" + schema.getRef();
            result.put("$ref", fullRef);
            return result; // Reference mode: ignore other fields
        }

        // Validate schema constraints before conversion
        schemaValidator.validateAndCorrect(schema);

        // Inline mode
        if (schema.getType() != null) {
            result.put("type", schema.getType());
        }
        if (schema.getTitle() != null) {
            result.put("title", schema.getTitle());
        }
        if (schema.getDescription() != null) {
            result.put("description", schema.getDescription());
        }
        if (schema.getProperties() != null) {
            result.put("properties", convertProperties(schema.getProperties()));
        }
        if (schema.getRequired() != null) {
            result.put("required", schema.getRequired());
        }
        if (schema.getOrders() != null) {
            result.put("x-ouroboros-orders", schema.getOrders());
        }
        if (schema.getXmlName() != null) {
            Map<String, Object> xml = new LinkedHashMap<>();
            xml.put("name", schema.getXmlName());
            result.put("xml", xml);
        }

        return result;
    }

    private Map<String, Object> convertProperties(Map<String, Property> properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            result.put(entry.getKey(), convertProperty(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> convertProperty(Property property) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Handle schema reference
        if (property.getRef() != null && !property.getRef().isBlank()) {
            // Convert to full $ref format for YAML
            String fullRef = property.getRef().startsWith("#/components/schemas/")
                    ? property.getRef()
                    : "#/components/schemas/" + property.getRef();
            result.put("$ref", fullRef);
            return result; // Reference mode: ignore other fields
        }

        // Inline mode
        if (property.getType() != null) {
            result.put("type", property.getType());
        }
        if (property.getDescription() != null) {
            result.put("description", property.getDescription());
        }
        if (property.getMockExpression() != null) {
            result.put("x-ouroboros-mock", property.getMockExpression());
        }
        
        // Object type - nested properties (재귀!)
        if (property.getProperties() != null && !property.getProperties().isEmpty()) {
            result.put("properties", convertProperties(property.getProperties()));
        }
        if (property.getRequired() != null && !property.getRequired().isEmpty()) {
            result.put("required", property.getRequired());
        }
        
        // Array type - items (재귀!)
        if (property.getItems() != null) {
            result.put("items", convertProperty(property.getItems()));
        }
        if (property.getMinItems() != null) {
            result.put("minItems", property.getMinItems());
        }
        if (property.getMaxItems() != null) {
            result.put("maxItems", property.getMaxItems());
        }
        
        // Additional constraints
        if (property.getFormat() != null) {
            result.put("format", property.getFormat());
        }
        if (property.getEnumValues() != null && !property.getEnumValues().isEmpty()) {
            result.put("enum", property.getEnumValues());
        }
        if (property.getPattern() != null) {
            result.put("pattern", property.getPattern());
        }
        if (property.getMinLength() != null) {
            result.put("minLength", property.getMinLength());
        }
        if (property.getMaxLength() != null) {
            result.put("maxLength", property.getMaxLength());
        }
        if (property.getMinimum() != null) {
            result.put("minimum", property.getMinimum());
        }
        if (property.getMaximum() != null) {
            result.put("maximum", property.getMaximum());
        }
        
        return result;
    }

    private Map<String, Object> convertHeaders(Map<String, Header> headers) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Header> entry : headers.entrySet()) {
            Map<String, Object> headerMap = new LinkedHashMap<>();
            Header header = entry.getValue();
            if (header.getDescription() != null) {
                headerMap.put("description", header.getDescription());
            }
            headerMap.put("required", header.isRequired());
            if (header.getSchema() != null) {
                headerMap.put("schema", convertSchema(header.getSchema()));
            }
            result.put(entry.getKey(), headerMap);
        }
        return result;
    }

    private List<Map<String, List<String>>> convertSecurity(List<SecurityRequirement> security) {
        List<Map<String, List<String>>> result = new ArrayList<>();
        for (SecurityRequirement req : security) {
            if (req.getRequirements() != null && !req.getRequirements().isEmpty()) {
                result.add(req.getRequirements());
                log.debug("Converting security requirement: {}", req.getRequirements());
            }
        }
        log.info("Converted {} security requirement(s)", result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private RestApiSpecResponse convertToResponse(String id, String path, String method, Map<String, Object> operation) {
        RestApiSpecResponse.RestApiSpecResponseBuilder builder = RestApiSpecResponse.builder()
                .id(id)
                .path(path)
                .method(method.toUpperCase())
                .summary((String) operation.get("summary"))
                .description((String) operation.get("description"))
                .tags((List<String>) operation.get("tags"))
                .progress((String) operation.get("x-ouroboros-progress"))
                .tag((String) operation.get("x-ouroboros-tag"))
                .diff((String) operation.get("x-ouroboros-diff"));

        // Convert parameters
        List<Object> params = (List<Object>) operation.get("parameters");
        if (params != null) {
            List<Parameter> parameters = new ArrayList<>();
            for (Object p : params) {
                Map<String, Object> paramMap = (Map<String, Object>) p;
                Parameter param = Parameter.builder()
                        .name((String) paramMap.get("name"))
                        .in((String) paramMap.get("in"))
                        .description((String) paramMap.get("description"))
                        .required((Boolean) paramMap.getOrDefault("required", false))
                        .schema(parseSchema((Map<String, Object>) paramMap.get("schema")))
                        .build();
                parameters.add(param);
            }
            builder.parameters(parameters);
        }

        // Convert request body
        Map<String, Object> reqBody = (Map<String, Object>) operation.get("requestBody");
        if (reqBody != null) {
            builder.requestBody(parseRequestBody(reqBody));
        }

        // Convert responses
        Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
        if (responses != null) {
            builder.responses(parseResponses(responses));
        }

        // Convert security
        List<Object> sec = (List<Object>) operation.get("security");
        if (sec != null) {
            List<SecurityRequirement> security = new ArrayList<>();
            for (Object s : sec) {
                SecurityRequirement req = SecurityRequirement.builder()
                        .requirements((Map<String, List<String>>) s)
                        .build();
                security.add(req);
            }
            builder.security(security);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private RequestBody parseRequestBody(Map<String, Object> reqBody) {
        return RequestBody.builder()
                .description((String) reqBody.get("description"))
                .required((Boolean) reqBody.getOrDefault("required", false))
                .content(parseContent((Map<String, Object>) reqBody.get("content")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, MediaType> parseContent(Map<String, Object> content) {
        if (content == null) return null;

        Map<String, MediaType> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            Map<String, Object> mediaTypeMap = (Map<String, Object>) entry.getValue();
            MediaType mediaType = MediaType.builder()
                    .schema(parseSchema((Map<String, Object>) mediaTypeMap.get("schema")))
                    .build();
            result.put(entry.getKey(), mediaType);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Schema parseSchema(Map<String, Object> schemaMap) {
        if (schemaMap == null) return null;

        Schema.SchemaBuilder builder = Schema.builder();

        // Check for $ref (reference mode) in YAML
        if (schemaMap.containsKey("$ref")) {
            String dollarRef = (String) schemaMap.get("$ref");
            
            // Convert $ref to simplified ref for client
            if (dollarRef != null && dollarRef.startsWith("#/components/schemas/")) {
                String simplifiedRef = dollarRef.substring("#/components/schemas/".length());
                builder.ref(simplifiedRef);
            } else if (dollarRef != null) {
                builder.ref(dollarRef);
            }
            
            return builder.build();
        }

        // Inline mode
        builder.type((String) schemaMap.get("type"))
                .title((String) schemaMap.get("title"))
                .description((String) schemaMap.get("description"))
                .required((List<String>) schemaMap.get("required"))
                .orders((List<String>) schemaMap.get("x-ouroboros-orders"));

        // Parse properties
        Map<String, Object> props = (Map<String, Object>) schemaMap.get("properties");
        if (props != null) {
            Map<String, Property> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                properties.put(entry.getKey(), parseProperty((Map<String, Object>) entry.getValue()));
            }
            builder.properties(properties);
        }

        // Parse XML
        Map<String, Object> xml = (Map<String, Object>) schemaMap.get("xml");
        if (xml != null) {
            builder.xmlName((String) xml.get("name"));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Property parseProperty(Map<String, Object> propMap) {
        if (propMap == null) return null;

        Property.PropertyBuilder builder = Property.builder();

        // Check for $ref (reference mode) in YAML
        if (propMap.containsKey("$ref")) {
            String dollarRef = (String) propMap.get("$ref");
            
            // Convert $ref to simplified ref for client
            if (dollarRef != null && dollarRef.startsWith("#/components/schemas/")) {
                String simplifiedRef = dollarRef.substring("#/components/schemas/".length());
                builder.ref(simplifiedRef);
            } else if (dollarRef != null) {
                builder.ref(dollarRef);
            }
            
            return builder.build();
        }

        // Inline mode
        builder.type((String) propMap.get("type"))
                .description((String) propMap.get("description"))
                .mockExpression((String) propMap.get("x-ouroboros-mock"));
        
        // Object type - nested properties (재귀!)
        Map<String, Object> properties = (Map<String, Object>) propMap.get("properties");
        if (properties != null) {
            Map<String, Property> parsedProperties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                parsedProperties.put(entry.getKey(), parseProperty((Map<String, Object>) entry.getValue()));
            }
            builder.properties(parsedProperties);
        }
        
        List<String> required = (List<String>) propMap.get("required");
        if (required != null) {
            builder.required(required);
        }

        // Array type - items (재귀!)
        Map<String, Object> items = (Map<String, Object>) propMap.get("items");
        if (items != null) {
            builder.items(parseProperty(items));
        }
        builder.minItems((Integer) propMap.get("minItems"))
               .maxItems((Integer) propMap.get("maxItems"));
        
        // Additional constraints
        builder.format((String) propMap.get("format"))
               .pattern((String) propMap.get("pattern"))
               .minLength((Integer) propMap.get("minLength"))
               .maxLength((Integer) propMap.get("maxLength"))
               .minimum((Number) propMap.get("minimum"))
               .maximum((Number) propMap.get("maximum"));
        
        // enum 값 파싱 - SnakeYAML이 숫자를 List<Integer>로 파싱할 수 있음
        Object enumObj = propMap.get("enum");
        if (enumObj instanceof java.util.Collection<?> enumCollection) {
            List<String> enumValues = new java.util.ArrayList<>();
            for (Object item : enumCollection) {
                enumValues.add(item != null ? item.toString() : "");
            }
            builder.enumValues(enumValues);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ApiResponse> parseResponses(Map<String, Object> responses) {
        if (responses == null) return null;

        Map<String, ApiResponse> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : responses.entrySet()) {
            Map<String, Object> responseMap = (Map<String, Object>) entry.getValue();
            ApiResponse response = ApiResponse.builder()
                    .description((String) responseMap.get("description"))
                    .content(parseContent((Map<String, Object>) responseMap.get("content")))
                    .headers(parseHeaders((Map<String, Object>) responseMap.get("headers")))
                    .build();
            result.put(entry.getKey(), response);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Header> parseHeaders(Map<String, Object> headers) {
        if (headers == null) return null;

        Map<String, Header> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            Map<String, Object> headerMap = (Map<String, Object>) entry.getValue();
            Header header = Header.builder()
                    .description((String) headerMap.get("description"))
                    .required((Boolean) headerMap.getOrDefault("required", false))
                    .schema(parseSchema((Map<String, Object>) headerMap.get("schema")))
                    .build();
            result.put(entry.getKey(), header);
        }
        return result;
    }

    @Override
    public ImportYamlResponse importYaml(String yamlContent) throws Exception {
        lock.writeLock().lock();
        try {
            log.info("========================================");
            log.info("📥 Starting YAML import...");

            // Step 1: Parse imported YAML (validation already done in controller)
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> importedDoc = (Map<String, Object>) yaml.load(yamlContent);

            // Step 2: Read existing document or create new one
            Map<String, Object> existingDoc = yamlParser.readOrCreateDocument();

            // Step 3: Prepare renamed tracking
            List<RenamedItem> renamedList = new ArrayList<>();
            Map<String, String> schemaRenameMap = new HashMap<>(); // old name -> new name

            // Step 4: Process schemas first (to build rename map)
            int importedSchemas = importSchemas(importedDoc, existingDoc, renamedList, schemaRenameMap);

            // Step 5: Process APIs with schema reference updates
            int importedApis = importApis(importedDoc, existingDoc, renamedList, schemaRenameMap);

            // Step 6: Validate and auto-create missing schema references
            int createdSchemas = schemaValidator.validateAndCreateMissingSchemas(existingDoc);
            if (createdSchemas > 0) {
                log.info("📦 Auto-created {} missing schema(s)", createdSchemas);
            }

            // Step 7: Write merged document back to file
            yamlParser.writeDocument(existingDoc);

            // Step 8: Build response
            String summary = String.format("Successfully imported %d APIs and %d schemas%s",
                    importedApis, importedSchemas,
                    !renamedList.isEmpty() ? ", renamed " + renamedList.size() + " items due to duplicates" : "");

            log.info("========================================");
            log.info("✅ YAML Import Completed");
            log.info("   📊 APIs imported: {}", importedApis);
            log.info("   📊 Schemas imported: {}", importedSchemas);
            log.info("   📊 Items renamed: {}", renamedList.size());
            log.info("========================================");

            return ImportYamlResponse.builder()
                    .imported(importedApis)
                    .renamed(renamedList.size())
                    .summary(summary)
                    .renamedList(renamedList)
                    .build();

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Imports schemas from imported document into existing document.
     * Handles duplicate schema names by auto-renaming with "-import" suffix.
     *
     * @param importedDoc the imported OpenAPI document
     * @param existingDoc the existing ourorest.yml document
     * @param renamedList list to track renamed items
     * @param schemaRenameMap map to track schema renames (old -> new)
     * @return number of schemas imported
     */
    @SuppressWarnings("unchecked")
    private int importSchemas(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                              List<RenamedItem> renamedList, Map<String, String> schemaRenameMap) {
        int count = 0;

        // Get or create components
        Map<String, Object> existingComponents = (Map<String, Object>) existingDoc.get("components");
        if (existingComponents == null) {
            existingComponents = new LinkedHashMap<>();
            existingDoc.put("components", existingComponents);
        }

        Map<String, Object> existingSchemas = (Map<String, Object>) existingComponents.get("schemas");
        if (existingSchemas == null) {
            existingSchemas = new LinkedHashMap<>();
            existingComponents.put("schemas", existingSchemas);
        }

        // Get imported schemas
        Map<String, Object> importedComponents = (Map<String, Object>) importedDoc.get("components");
        if (importedComponents == null) {
            return 0;
        }

        Map<String, Object> importedSchemas = (Map<String, Object>) importedComponents.get("schemas");
        if (importedSchemas == null || importedSchemas.isEmpty()) {
            return 0;
        }

        // Import each schema
        for (Map.Entry<String, Object> entry : importedSchemas.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            // Check for duplicate and rename if necessary
            if (existingSchemas.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingSchemas.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("schema")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                schemaRenameMap.put(originalName, finalName);
                log.info("🔄 Schema '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            // Add schema to existing document
            Map<String, Object> schema = (Map<String, Object>) entry.getValue();
            enrichSchemaWithOuroborosFields(schema);
            existingSchemas.put(finalName, schema);
            count++;
        }

        return count;
    }

    /**
     * Imports API operations from imported document into existing document.
     * Handles duplicate path+method by auto-renaming paths with "-import" suffix.
     * Updates $ref references according to schema rename map.
     *
     * @param importedDoc the imported OpenAPI document
     * @param existingDoc the existing ourorest.yml document
     * @param renamedList list to track renamed items
     * @param schemaRenameMap map of schema renames to update $ref
     * @return number of APIs imported
     */
    @SuppressWarnings("unchecked")
    private int importApis(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                           List<RenamedItem> renamedList, Map<String, String> schemaRenameMap) {
        int count = 0;

        // Get or create paths
        Map<String, Object> existingPaths = yamlParser.getOrCreatePaths(existingDoc);

        // Get imported paths
        Map<String, Object> importedPaths = (Map<String, Object>) importedDoc.get("paths");
        if (importedPaths == null || importedPaths.isEmpty()) {
            return 0;
        }

        // Import each path
        for (Map.Entry<String, Object> pathEntry : importedPaths.entrySet()) {
            String originalPath = pathEntry.getKey();
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

            // Process each HTTP method
            for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                String method = methodEntry.getKey().toLowerCase();

                // Skip non-method keys
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }

                Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();
                String finalPath = originalPath;

                // Check for duplicate path+method and rename if necessary
                if (yamlParser.operationExists(existingDoc, originalPath, method)) {
                    finalPath = originalPath + "-import";
                    int counter = 1;
                    while (yamlParser.operationExists(existingDoc, finalPath, method)) {
                        finalPath = originalPath + "-import" + counter;
                        counter++;
                    }

                    renamedList.add(RenamedItem.builder()
                            .type("api")
                            .original(originalPath)
                            .renamed(finalPath)
                            .method(method.toUpperCase())
                            .build());

                    log.info("🔄 API '{} {}' renamed to '{} {}' due to duplicate",
                            method.toUpperCase(), originalPath, method.toUpperCase(), finalPath);
                }

                // Update $ref references in the operation
                updateSchemaReferences(operation, schemaRenameMap);

                // Enrich operation with Ouroboros fields
                enrichOperationWithOuroborosFields(operation);

                // Add operation to existing document
                yamlParser.putOperation(existingDoc, finalPath, method, operation);
                count++;
            }
        }

        return count;
    }

    /**
     * Enriches an operation with missing Ouroboros custom fields.
     *
     * @param operation the operation map to enrich
     */
    private void enrichOperationWithOuroborosFields(Map<String, Object> operation) {
        if (!operation.containsKey("x-ouroboros-id")) {
            operation.put("x-ouroboros-id", UUID.randomUUID().toString());
        }
        if (!operation.containsKey("x-ouroboros-progress")) {
            operation.put("x-ouroboros-progress", "mock");
        }
        if (!operation.containsKey("x-ouroboros-tag")) {
            operation.put("x-ouroboros-tag", "none");
        }
        if (!operation.containsKey("x-ouroboros-diff")) {
            operation.put("x-ouroboros-diff", "none");
        }
    }

    /**
     * Enriches a schema with missing Ouroboros custom fields.
     * Recursively processes nested object properties and array items.
     * Also validates and corrects minItems/maxItems constraints.
     *
     * @param schema the schema map to enrich
     */
    @SuppressWarnings("unchecked")
    private void enrichSchemaWithOuroborosFields(Map<String, Object> schema) {
        if (schema == null) {
            return;
        }

        // Skip $ref schemas (they reference another schema)
        if (schema.containsKey("$ref")) {
            return;
        }

        // Validate schema constraints (minItems/maxItems, etc.)
        schemaValidator.validateAndCorrectSchemaMap(schema);

        // Process properties if present
        if (schema.containsKey("properties")) {
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            if (properties != null) {
                // Add x-ouroboros-mock to each property
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> property = (Map<String, Object>) entry.getValue();

                        // Add mock expression if not a reference and doesn't have it
                        if (!property.containsKey("$ref") && !property.containsKey("x-ouroboros-mock")) {
                            property.put("x-ouroboros-mock", "");
                        }

                        // Recursively process nested object properties
                        if ("object".equals(property.get("type")) && property.containsKey("properties")) {
                            enrichSchemaWithOuroborosFields(property);
                        }

                        // Recursively process array items
                        if ("array".equals(property.get("type")) && property.containsKey("items")) {
                            Object items = property.get("items");
                            if (items instanceof Map) {
                                enrichSchemaWithOuroborosFields((Map<String, Object>) items);
                            }
                        }
                    }
                }

                // Add x-ouroboros-orders for property ordering
                if (!schema.containsKey("x-ouroboros-orders")) {
                    schema.put("x-ouroboros-orders", new ArrayList<>(properties.keySet()));
                }
            }
        }

        // Process array items at schema level (for top-level array schemas)
        if ("array".equals(schema.get("type")) && schema.containsKey("items")) {
            Object items = schema.get("items");
            if (items instanceof Map) {
                enrichSchemaWithOuroborosFields((Map<String, Object>) items);
            }
        }
    }

    /**
     * Recursively updates all $ref references in an operation according to schema rename map.
     *
     * @param obj the object to scan for $ref (can be Map, List, or primitive)
     * @param schemaRenameMap map of old schema names to new names
     */
    @SuppressWarnings("unchecked")
    private void updateSchemaReferences(Object obj, Map<String, String> schemaRenameMap) {
        if (schemaRenameMap.isEmpty()) {
            return; // No renames needed
        }

        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;

            // Check if this map has a $ref field
            if (map.containsKey("$ref")) {
                String ref = (String) map.get("$ref");
                if (ref != null && ref.startsWith("#/components/schemas/")) {
                    String schemaName = ref.substring("#/components/schemas/".length());
                    if (schemaRenameMap.containsKey(schemaName)) {
                        String newSchemaName = schemaRenameMap.get(schemaName);
                        map.put("$ref", "#/components/schemas/" + newSchemaName);
                        log.debug("🔗 Updated $ref: {} -> {}", schemaName, newSchemaName);
                    }
                }
            }

            // Recursively scan all values
            for (Object value : map.values()) {
                updateSchemaReferences(value, schemaRenameMap);
            }

        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            for (Object item : list) {
                updateSchemaReferences(item, schemaRenameMap);
            }
        }
    }

    /**
     * Auto-creates security schemes in components/securitySchemes based on security requirements.
     * <p>
     * When an operation has security requirements (e.g., [{ BearerAuth: [] }]),
     * this method automatically creates the corresponding security scheme definition
     * in components/securitySchemes if it doesn't already exist.
     * <p>
     * Supported security schemes:
     * - BearerAuth: HTTP Bearer token (JWT)
     * - BasicAuth: HTTP Basic authentication
     * - ApiKeyAuth: API Key in header
     * - OAuth2: OAuth 2.0
     * - OAuth1: OAuth 1.0
     * - DigestAuth: HTTP Digest authentication
     *
     * @param openApiDoc the OpenAPI document
     * @param securityRequirements list of security requirements from the operation
     */
    @SuppressWarnings("unchecked")
    private void autoCreateSecuritySchemes(Map<String, Object> openApiDoc, List<SecurityRequirement> securityRequirements) {
        // Get or create components section
        Map<String, Object> components = (Map<String, Object>) openApiDoc.computeIfAbsent("components", k -> new LinkedHashMap<>());
        
        // Get or create securitySchemes section
        Map<String, Object> securitySchemes = (Map<String, Object>) components.computeIfAbsent("securitySchemes", k -> new LinkedHashMap<>());

        // Process each security requirement
        for (SecurityRequirement requirement : securityRequirements) {
            if (requirement.getRequirements() == null) continue;
            
            for (String schemeName : requirement.getRequirements().keySet()) {
                // Skip if already exists
                if (securitySchemes.containsKey(schemeName)) {
                    continue;
                }

                // Create appropriate security scheme based on name
                Map<String, Object> scheme = new LinkedHashMap<>();
                
                switch (schemeName) {
                    case "BearerAuth":
                        scheme.put("type", "http");
                        scheme.put("scheme", "bearer");
                        scheme.put("bearerFormat", "JWT");
                        scheme.put("description", "JWT Bearer token authentication");
                        break;
                        
                    case "BasicAuth":
                        scheme.put("type", "http");
                        scheme.put("scheme", "basic");
                        scheme.put("description", "HTTP Basic authentication");
                        break;
                        
                    case "ApiKeyAuth":
                        scheme.put("type", "apiKey");
                        scheme.put("in", "header");
                        scheme.put("name", "X-API-Key");
                        scheme.put("description", "API Key authentication");
                        break;
                        
                    case "OAuth2":
                        scheme.put("type", "oauth2");
                        Map<String, Object> flows = new LinkedHashMap<>();
                        Map<String, Object> authCodeFlow = new LinkedHashMap<>();
                        authCodeFlow.put("authorizationUrl", "https://example.com/oauth/authorize");
                        authCodeFlow.put("tokenUrl", "https://example.com/oauth/token");
                        authCodeFlow.put("scopes", new LinkedHashMap<>());
                        flows.put("authorizationCode", authCodeFlow);
                        scheme.put("flows", flows);
                        scheme.put("description", "OAuth 2.0 authentication");
                        break;
                        
                    case "OAuth1":
                        scheme.put("type", "oauth2");
                        scheme.put("description", "OAuth 1.0 authentication (using OAuth2 type as fallback)");
                        break;
                        
                    case "DigestAuth":
                        scheme.put("type", "http");
                        scheme.put("scheme", "digest");
                        scheme.put("description", "HTTP Digest authentication");
                        break;
                        
                    default:
                        // Unknown security scheme - create a basic Bearer token scheme
                        scheme.put("type", "http");
                        scheme.put("scheme", "bearer");
                        scheme.put("description", "Authentication required");
                        break;
                }
                
                securitySchemes.put(schemeName, scheme);
                log.info("Auto-created security scheme: {}", schemeName);
            }
        }
    }
}
