package kr.co.ouroboros.core.rest.spec.writer;

import kr.co.ouroboros.core.global.exception.DuplicateApiSpecException;
import kr.co.ouroboros.core.rest.spec.model.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Writes REST API specifications to OpenAPI 3.1.0 YAML format.
 * <p>
 * Handles generation and merging of OpenAPI specification files. When a file exists,
 * it merges new API specifications while checking for duplicate path+method combinations.
 * Generates files at {@code resources/ouroboros/rest/ourorest.yml}.
 *
 * @since 0.0.1
 */
public class OpenApiYamlWriter {

    private static final String OPENAPI_VERSION = "3.1.0";
    private final Yaml yaml;
    private final String serverUrl;
    private final String serverDescription;

    /**
     * Constructs a new OpenApiYamlWriter with the specified server URL and description.
     *
     * @param serverUrl the base server URL to include in the OpenAPI document
     * @param serverDescription the description of the server
     */
    public OpenApiYamlWriter(String serverUrl, String serverDescription) {
        this.serverUrl = serverUrl;
        this.serverDescription = serverDescription;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    /**
     * Writes a REST API specification to OpenAPI YAML file.
     * <p>
     * If the file already exists, merges the new specification into the existing document.
     * Throws {@link kr.co.ouroboros.core.global.exception.DuplicateApiSpecException}
     * if a specification with the same path and method already exists.
     *
     * @param spec the REST API specification to write
     * @param baseResourcePath the base resources directory path
     * @throws IOException if file I/O operation fails
     * @throws kr.co.ouroboros.core.global.exception.DuplicateApiSpecException if duplicate path+method found
     */
    public void writeToFile(RestApiSpec spec, String baseResourcePath) throws IOException {
        // Create directory: resources/ouroboros/rest/
        Path dirPath = Paths.get(baseResourcePath, "ouroboros", "rest");
        Files.createDirectories(dirPath);

        // Single file: ourorest.yml
        String fileName = "ourorest.yml";
        Path filePath = dirPath.resolve(fileName);

        Map<String, Object> openApiDoc;

        // If file exists, read and merge
        if (Files.exists(filePath)) {
            openApiDoc = readExistingFile(filePath);
            mergePathIntoDocument(openApiDoc, spec);
        } else {
            openApiDoc = generateOpenApiDocument(spec);
        }

        // Always update servers section with latest configuration
        updateServersSection(openApiDoc);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            yaml.dump(openApiDoc, writer);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readExistingFile(Path filePath) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath.toFile())) {
            Object loaded = yaml.load(fis);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void mergePathIntoDocument(Map<String, Object> existingDoc, RestApiSpec spec) {
        // Get or create paths section
        Map<String, Object> paths = (Map<String, Object>) existingDoc.get("paths");
        if (paths == null) {
            paths = new LinkedHashMap<>();
            existingDoc.put("paths", paths);
        }

        // Get or create path item
        Map<String, Object> pathItem = (Map<String, Object>) paths.get(spec.getPath());
        if (pathItem == null) {
            pathItem = new LinkedHashMap<>();
            paths.put(spec.getPath(), pathItem);
        }

        // Check for duplicate path+method combination
        String methodLower = spec.getMethod().toLowerCase();
        if (pathItem.containsKey(methodLower)) {
            throw new DuplicateApiSpecException(spec.getPath(), spec.getMethod());
        }

        // Add operation (method)
        Map<String, Object> operation = buildOperation(spec);
        pathItem.put(methodLower, operation);

        // Merge security schemes if needed
        if (spec.getSecurity() != null && !spec.getSecurity().isEmpty()) {
            Map<String, Object> components = (Map<String, Object>) existingDoc.get("components");
            if (components == null) {
                components = new LinkedHashMap<>();
                existingDoc.put("components", components);
            }

            Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");
            if (securitySchemes == null) {
                securitySchemes = new LinkedHashMap<>();
                components.put("securitySchemes", securitySchemes);
            }

            for (SecurityRequirement req : spec.getSecurity()) {
                for (String schemeName : req.getSchemes().keySet()) {
                    if (!securitySchemes.containsKey(schemeName)) {
                        Map<String, Object> scheme = new LinkedHashMap<>();
                        scheme.put("type", "http");
                        scheme.put("scheme", schemeName);
                        securitySchemes.put(schemeName, scheme);
                    }
                }
            }
        }
    }

    private Map<String, Object> generateOpenApiDocument(RestApiSpec spec) {
        Map<String, Object> doc = new LinkedHashMap<>();

        // openapi version
        doc.put("openapi", OPENAPI_VERSION);

        // info
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "API Documentation");
        info.put("version", "1.0.0");
        doc.put("info", info);

        // paths
        Map<String, Object> paths = new LinkedHashMap<>();
        Map<String, Object> pathItem = new LinkedHashMap<>();
        Map<String, Object> operation = buildOperation(spec);
        pathItem.put(spec.getMethod().toLowerCase(), operation);
        paths.put(spec.getPath(), pathItem);
        doc.put("paths", paths);

        // components
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("schemas", new LinkedHashMap<>());

        // security schemes
        if (spec.getSecurity() != null && !spec.getSecurity().isEmpty()) {
            Map<String, Object> securitySchemes = new LinkedHashMap<>();
            for (SecurityRequirement req : spec.getSecurity()) {
                for (String schemeName : req.getSchemes().keySet()) {
                    Map<String, Object> scheme = new LinkedHashMap<>();
                    scheme.put("type", "http");
                    scheme.put("scheme", schemeName);
                    securitySchemes.put(schemeName, scheme);
                }
            }
            components.put("securitySchemes", securitySchemes);
        }
        doc.put("components", components);

        // servers - will be updated by updateServersSection()
        doc.put("servers", new ArrayList<>());

        // security (global)
        doc.put("security", new ArrayList<>());

        return doc;
    }

    /**
     * Updates the servers section in the OpenAPI document with the latest configuration.
     * <p>
     * This method is called every time the YAML file is written to ensure the server URL
     * and description are always up-to-date with the current configuration.
     *
     * @param doc the OpenAPI document to update
     */
    private void updateServersSection(Map<String, Object> doc) {
        List<Map<String, String>> servers = new ArrayList<>();
        Map<String, String> server = new LinkedHashMap<>();
        server.put("url", serverUrl);
        server.put("description", serverDescription);
        servers.add(server);
        doc.put("servers", servers);
    }

    private Map<String, Object> buildOperation(RestApiSpec spec) {
        Map<String, Object> operation = new LinkedHashMap<>();

        operation.put("summary", spec.getSummary() != null ? spec.getSummary() : "");
        operation.put("deprecated", spec.isDeprecated());
        operation.put("description", spec.getDescription() != null ? spec.getDescription() : "");
        operation.put("tags", spec.getTags() != null ? spec.getTags() : new ArrayList<>());
        operation.put("parameters", spec.getParameters() != null ?
            buildParameters(spec.getParameters()) : new ArrayList<>());

        // responses
        if (spec.getResponses() != null) {
            Map<String, Object> responses = new LinkedHashMap<>();
            for (Map.Entry<String, ApiResponse> entry : spec.getResponses().entrySet()) {
                responses.put(entry.getKey(), buildResponse(entry.getValue()));
            }
            operation.put("responses", responses);
        }

        // security
        if (spec.getSecurity() != null && !spec.getSecurity().isEmpty()) {
            List<Map<String, List<String>>> securityList = new ArrayList<>();
            for (SecurityRequirement req : spec.getSecurity()) {
                securityList.add(req.getSchemes());
            }
            operation.put("security", securityList);
        }

        // custom fields
        if (spec.getId() != null) {
            operation.put("x-ouroboros-id", spec.getId());
        }
        operation.put("x-ouroboros-status", "mock");
        operation.put("x-ouroboros-conflict", "none");

        return operation;
    }

    private List<Map<String, Object>> buildParameters(List<Parameter> parameters) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Parameter param : parameters) {
            Map<String, Object> paramMap = new LinkedHashMap<>();
            paramMap.put("name", param.getName());
            paramMap.put("in", param.getIn());
            paramMap.put("description", param.getDescription());
            paramMap.put("required", param.isRequired());
            if (param.getSchema() != null) {
                paramMap.put("schema", buildSchema(param.getSchema()));
            }
            result.add(paramMap);
        }
        return result;
    }

    private Map<String, Object> buildResponse(ApiResponse response) {
        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("description", response.getDescription() != null ? response.getDescription() : "");

        if (response.getContent() != null) {
            Map<String, Object> content = new LinkedHashMap<>();
            for (Map.Entry<String, MediaType> entry : response.getContent().entrySet()) {
                Map<String, Object> mediaTypeMap = new LinkedHashMap<>();
                if (entry.getValue().getSchema() != null) {
                    mediaTypeMap.put("schema", buildSchema(entry.getValue().getSchema()));
                }
                content.put(entry.getKey(), mediaTypeMap);
            }
            responseMap.put("content", content);
        }

        responseMap.put("headers", response.getHeaders() != null ? response.getHeaders() : new LinkedHashMap<>());

        if (response.getName() != null) {
            responseMap.put("x-ouroboros-name", response.getName());
        }

        return responseMap;
    }

    private Map<String, Object> buildSchema(Schema schema) {
        Map<String, Object> schemaMap = new LinkedHashMap<>();

        if (schema.getTitle() != null) {
            schemaMap.put("title", schema.getTitle());
        }
        schemaMap.put("type", schema.getType() != null ? schema.getType() : "object");

        if (schema.getProperties() != null) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Property> entry : schema.getProperties().entrySet()) {
                properties.put(entry.getKey(), buildProperty(entry.getValue()));
            }
            schemaMap.put("properties", properties);
        }

        if (schema.getOrders() != null && !schema.getOrders().isEmpty()) {
            schemaMap.put("x-ouroboros-orders", schema.getOrders());
        }

        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            schemaMap.put("required", schema.getRequired());
        }

        return schemaMap;
    }

    private Map<String, Object> buildProperty(Property property) {
        Map<String, Object> propertyMap = new LinkedHashMap<>();
        propertyMap.put("type", property.getType() != null ? property.getType() : "string");

        if (property.getMockExpression() != null) {
            propertyMap.put("x-ouroboros-mock", property.getMockExpression());
        }

        if (property.getDescription() != null) {
            propertyMap.put("description", property.getDescription());
        }

        return propertyMap;
    }
}