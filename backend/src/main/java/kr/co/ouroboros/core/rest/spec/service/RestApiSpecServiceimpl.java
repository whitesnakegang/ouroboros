package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.RestApiSpecResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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

            // Write back to file
            yamlParser.writeDocument(openApiDoc);

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

            // Write back to file
            yamlParser.writeDocument(openApiDoc);

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

            // Write back to file
            yamlParser.writeDocument(openApiDoc);
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
        operation.put("deprecated", request.isDeprecated());

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
            operation.put("security", convertSecurity(request.getSecurity()));
        }

        // Add Ouroboros custom fields
        operation.put("x-ouroboros-id", id);
        operation.put("x-ouroboros-progress", request.getProgress() != null ? request.getProgress() : "mock");
        operation.put("x-ouroboros-tag", request.getTag() != null ? request.getTag() : "none");
        operation.put("x-ouroboros-diff", request.getDiff() != null ? request.getDiff() : "none");

        return operation;
    }

    private void updateOperationFields(Map<String, Object> operation, UpdateRestApiRequest request) {
        if (request.getSummary() != null) {
            operation.put("summary", request.getSummary());
        }
        if (request.getDescription() != null) {
            operation.put("description", request.getDescription());
        }
        if (request.getDeprecated() != null) {
            operation.put("deprecated", request.getDeprecated());
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
        if (request.getSecurity() != null) {
            operation.put("security", convertSecurity(request.getSecurity()));
        }
        if (request.getProgress() != null) {
            operation.put("x-ouroboros-progress", request.getProgress());
        }
        if (request.getTag() != null) {
            operation.put("x-ouroboros-tag", request.getTag());
        }
        if (request.getDiff() != null) {
            operation.put("x-ouroboros-diff", request.getDiff());
        }
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
        if (property.getItems() != null) {
            result.put("items", convertProperty(property.getItems()));
        }
        if (property.getMinItems() != null) {
            result.put("minItems", property.getMinItems());
        }
        if (property.getMaxItems() != null) {
            result.put("maxItems", property.getMaxItems());
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
            result.add(req.getRequirements());
        }
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
                .deprecated((Boolean) operation.get("deprecated"))
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
                .mockExpression((String) propMap.get("x-ouroboros-mock"))
                .minItems((Integer) propMap.get("minItems"))
                .maxItems((Integer) propMap.get("maxItems"));

        Map<String, Object> items = (Map<String, Object>) propMap.get("items");
        if (items != null) {
            builder.items(parseProperty(items));
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
}
