package kr.co.ouroboros.core.rest.spec.validator;

import kr.co.ouroboros.core.rest.spec.dto.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Validates external OpenAPI YAML files before import.
 * <p>
 * This validator ensures that uploaded YAML files conform to OpenAPI 3.1.0 specification
 * and contain valid structure, required fields, HTTP methods, and data types.
 * Unlike {@link OurorestYamlValidator}, this validator returns validation errors
 * rather than enriching the document.
 * <p>
 * <b>Validation Steps:</b>
 * <ol>
 *   <li>File extension validation (.yml or .yaml)</li>
 *   <li>YAML syntax parsing</li>
 *   <li>OpenAPI 3.1.0 structure validation</li>
 *   <li>Required fields validation (openapi, info, paths)</li>
 *   <li>HTTP method validation</li>
 *   <li>Data type validation</li>
 * </ol>
 *
 * @see OurorestYamlValidator
 * @since 0.0.1
 */
@Slf4j
@Component
public class ImportYamlValidator {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".yml", ".yaml");
    private static final List<String> HTTP_METHODS = Arrays.asList(
            "get", "post", "put", "delete", "patch", "options", "head", "trace"
    );
    private static final List<String> VALID_DATA_TYPES = Arrays.asList(
            "string", "number", "integer", "boolean", "array", "object"
    );

    /**
     * Validates the file extension.
     *
     * @param filename the name of the uploaded file
     * @return list of validation errors (empty if valid)
     */
    public List<ValidationError> validateFileExtension(String filename) {
        List<ValidationError> errors = new ArrayList<>();

        if (filename == null || filename.trim().isEmpty()) {
            errors.add(ValidationError.builder()
                    .location("file")
                    .errorCode("INVALID_FILENAME")
                    .message("Filename is null or empty")
                    .build());
            return errors;
        }

        boolean validExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (filename.toLowerCase().endsWith(ext)) {
                validExtension = true;
                break;
            }
        }

        if (!validExtension) {
            errors.add(ValidationError.builder()
                    .location("file")
                    .errorCode("INVALID_FILE_EXTENSION")
                    .message("File extension must be .yml or .yaml")
                    .build());
        }

        return errors;
    }

    /**
     * Validates the complete YAML content.
     * <p>
     * Performs all validation checks and returns a comprehensive list of errors.
     *
     * @param yamlContent the YAML content as string
     * @return list of validation errors (empty if valid)
     */
    public List<ValidationError> validate(String yamlContent) {
        List<ValidationError> errors = new ArrayList<>();

        // Step 1: Parse YAML
        Map<String, Object> document;
        try {
            Yaml yaml = new Yaml();
            Object parsed = yaml.load(yamlContent);
            if (!(parsed instanceof Map)) {
                errors.add(ValidationError.builder()
                        .location("root")
                        .errorCode("INVALID_YAML_STRUCTURE")
                        .message("YAML root must be an object/map")
                        .build());
                return errors;
            }
            document = (Map<String, Object>) parsed;
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .location("root")
                    .errorCode("YAML_PARSE_ERROR")
                    .message("Failed to parse YAML: " + e.getMessage())
                    .build());
            return errors;
        }

        // Step 2: Validate OpenAPI version
        errors.addAll(validateOpenApiVersion(document));

        // Step 3: Validate required fields
        errors.addAll(validateRequiredFields(document));

        // Step 4: Validate info section
        errors.addAll(validateInfo(document));

        // Step 5: Validate paths and operations
        errors.addAll(validatePaths(document));

        return errors;
    }

    /**
     * Validates the OpenAPI version field.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    private List<ValidationError> validateOpenApiVersion(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        if (!document.containsKey("openapi")) {
            errors.add(ValidationError.builder()
                    .location("openapi")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'openapi'")
                    .build());
            return errors;
        }

        Object versionObj = document.get("openapi");
        if (!(versionObj instanceof String)) {
            errors.add(ValidationError.builder()
                    .location("openapi")
                    .errorCode("INVALID_DATA_TYPE")
                    .message("Field 'openapi' must be a string")
                    .build());
            return errors;
        }

        String version = (String) versionObj;
        if (!version.startsWith("3.")) {
            errors.add(ValidationError.builder()
                    .location("openapi")
                    .errorCode("UNSUPPORTED_VERSION")
                    .message("OpenAPI version must be 3.x.x (found: " + version + ")")
                    .build());
        }

        return errors;
    }

    /**
     * Validates required top-level fields.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    private List<ValidationError> validateRequiredFields(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        if (!document.containsKey("info")) {
            errors.add(ValidationError.builder()
                    .location("info")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'info'")
                    .build());
        }

        if (!document.containsKey("paths")) {
            errors.add(ValidationError.builder()
                    .location("paths")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'paths'")
                    .build());
        }

        return errors;
    }

    /**
     * Validates the info section.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validateInfo(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        Object infoObj = document.get("info");
        if (infoObj == null) {
            return errors; // Already reported in validateRequiredFields
        }

        if (!(infoObj instanceof Map)) {
            errors.add(ValidationError.builder()
                    .location("info")
                    .errorCode("INVALID_DATA_TYPE")
                    .message("Field 'info' must be an object")
                    .build());
            return errors;
        }

        Map<String, Object> info = (Map<String, Object>) infoObj;

        // Check required info fields
        if (!info.containsKey("title")) {
            errors.add(ValidationError.builder()
                    .location("info.title")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'info.title'")
                    .build());
        }

        if (!info.containsKey("version")) {
            errors.add(ValidationError.builder()
                    .location("info.version")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'info.version'")
                    .build());
        }

        return errors;
    }

    /**
     * Validates the paths section and all operations.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validatePaths(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        Object pathsObj = document.get("paths");
        if (pathsObj == null) {
            return errors; // Already reported in validateRequiredFields
        }

        if (!(pathsObj instanceof Map)) {
            errors.add(ValidationError.builder()
                    .location("paths")
                    .errorCode("INVALID_DATA_TYPE")
                    .message("Field 'paths' must be an object")
                    .build());
            return errors;
        }

        Map<String, Object> paths = (Map<String, Object>) pathsObj;

        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            Object pathItemObj = pathEntry.getValue();

            if (!(pathItemObj instanceof Map)) {
                errors.add(ValidationError.builder()
                        .location("paths." + path)
                        .errorCode("INVALID_DATA_TYPE")
                        .message("Path item must be an object")
                        .build());
                continue;
            }

            Map<String, Object> pathItem = (Map<String, Object>) pathItemObj;

            // Validate each HTTP method
            for (Map.Entry<String, Object> entry : pathItem.entrySet()) {
                String key = entry.getKey();

                // Skip non-method fields
                if (key.equals("summary") || key.equals("description") ||
                    key.equals("servers") || key.equals("parameters") ||
                    key.startsWith("$") || key.startsWith("x-")) {
                    continue;
                }

                // Check if it's a valid HTTP method
                if (!HTTP_METHODS.contains(key.toLowerCase())) {
                    errors.add(ValidationError.builder()
                            .location("paths." + path + "." + key)
                            .errorCode("INVALID_HTTP_METHOD")
                            .message("Invalid HTTP method: '" + key + "'. Valid methods: " + HTTP_METHODS)
                            .build());
                    continue;
                }

                // Validate operation object
                Object operationObj = entry.getValue();
                if (!(operationObj instanceof Map)) {
                    errors.add(ValidationError.builder()
                            .location("paths." + path + "." + key)
                            .errorCode("INVALID_DATA_TYPE")
                            .message("Operation must be an object")
                            .build());
                    continue;
                }

                Map<String, Object> operation = (Map<String, Object>) operationObj;
                errors.addAll(validateOperation(operation, path, key));
            }
        }

        return errors;
    }

    /**
     * Validates a single operation.
     *
     * @param operation the operation map
     * @param path the API path
     * @param method the HTTP method
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validateOperation(Map<String, Object> operation, String path, String method) {
        List<ValidationError> errors = new ArrayList<>();
        String location = "paths." + path + "." + method;

        // Validate responses (required field)
        if (!operation.containsKey("responses")) {
            errors.add(ValidationError.builder()
                    .location(location + ".responses")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'responses'")
                    .build());
        } else {
            Object responsesObj = operation.get("responses");
            if (!(responsesObj instanceof Map)) {
                errors.add(ValidationError.builder()
                        .location(location + ".responses")
                        .errorCode("INVALID_DATA_TYPE")
                        .message("Field 'responses' must be an object")
                        .build());
            }
        }

        // Validate schema data types if present
        if (operation.containsKey("requestBody")) {
            errors.addAll(validateRequestBody(operation.get("requestBody"), location + ".requestBody"));
        }

        return errors;
    }

    /**
     * Validates a request body.
     *
     * @param requestBodyObj the request body object
     * @param location the location path for error reporting
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validateRequestBody(Object requestBodyObj, String location) {
        List<ValidationError> errors = new ArrayList<>();

        if (!(requestBodyObj instanceof Map)) {
            errors.add(ValidationError.builder()
                    .location(location)
                    .errorCode("INVALID_DATA_TYPE")
                    .message("RequestBody must be an object")
                    .build());
            return errors;
        }

        Map<String, Object> requestBody = (Map<String, Object>) requestBodyObj;
        if (requestBody.containsKey("content")) {
            Object contentObj = requestBody.get("content");
            if (contentObj instanceof Map) {
                Map<String, Object> content = (Map<String, Object>) contentObj;
                for (Map.Entry<String, Object> entry : content.entrySet()) {
                    String mediaType = entry.getKey();
                    Object mediaTypeObj = entry.getValue();
                    if (mediaTypeObj instanceof Map) {
                        Map<String, Object> mediaTypeMap = (Map<String, Object>) mediaTypeObj;
                        if (mediaTypeMap.containsKey("schema")) {
                            errors.addAll(validateSchema(mediaTypeMap.get("schema"),
                                location + ".content." + mediaType + ".schema"));
                        }
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Validates a schema object and its data types.
     *
     * @param schemaObj the schema object
     * @param location the location path for error reporting
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validateSchema(Object schemaObj, String location) {
        List<ValidationError> errors = new ArrayList<>();

        if (!(schemaObj instanceof Map)) {
            return errors; // Schema can be a reference
        }

        Map<String, Object> schema = (Map<String, Object>) schemaObj;

        // Skip $ref schemas
        if (schema.containsKey("$ref")) {
            return errors;
        }

        // Validate type field if present
        if (schema.containsKey("type")) {
            Object typeObj = schema.get("type");
            if (typeObj instanceof String) {
                String type = (String) typeObj;
                if (!VALID_DATA_TYPES.contains(type)) {
                    errors.add(ValidationError.builder()
                            .location(location + ".type")
                            .errorCode("INVALID_DATA_TYPE")
                            .message("Invalid schema type: '" + type + "'. Valid types: " + VALID_DATA_TYPES)
                            .build());
                }
            }
        }

        // Validate properties recursively
        if (schema.containsKey("properties")) {
            Object propertiesObj = schema.get("properties");
            if (propertiesObj instanceof Map) {
                Map<String, Object> properties = (Map<String, Object>) propertiesObj;
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String propName = entry.getKey();
                    errors.addAll(validateSchema(entry.getValue(),
                        location + ".properties." + propName));
                }
            }
        }

        // Validate items for array type
        if (schema.containsKey("items")) {
            errors.addAll(validateSchema(schema.get("items"), location + ".items"));
        }

        return errors;
    }
}