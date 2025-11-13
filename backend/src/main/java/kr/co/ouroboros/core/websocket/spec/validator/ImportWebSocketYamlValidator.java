package kr.co.ouroboros.core.websocket.spec.validator;

import kr.co.ouroboros.ui.websocket.spec.dto.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.*;

/**
 * Validates external AsyncAPI YAML files before import.
 * <p>
 * This validator ensures that uploaded YAML files conform to AsyncAPI 3.0.0 specification
 * and contain valid structure, required fields, action types, and data types.
 * Unlike {@link OurowebsocketYamlValidator}, this validator returns validation errors
 * rather than enriching the document.
 * <p>
 * <b>Validation Steps:</b>
 * <ol>
 *   <li>File extension validation (.yml or .yaml)</li>
 *   <li>YAML syntax parsing</li>
 *   <li>AsyncAPI 3.0.0 structure validation</li>
 *   <li>Required fields validation (asyncapi, info, channels, operations)</li>
 *   <li>Action type validation (send/receive)</li>
 *   <li>Data type validation</li>
 * </ol>
 *
 * @see OurowebsocketYamlValidator
 * @since 0.1.0
 */
@Slf4j
@Component
public class ImportWebSocketYamlValidator {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".yml", ".yaml");
    private static final List<String> VALID_ACTIONS = Arrays.asList("send", "receive");
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

        // Step 1: Parse YAML (using SafeConstructor to prevent arbitrary object deserialization)
        Map<String, Object> document;
        try {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
            Object parsed = yaml.load(yamlContent);
            if (!(parsed instanceof Map)) {
                errors.add(ValidationError.builder()
                        .location("root")
                        .errorCode("INVALID_YAML_STRUCTURE")
                        .message("YAML root must be an object/map")
                        .build());
                return errors;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedDocument = (Map<String, Object>) parsed;
            document = parsedDocument;
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .location("root")
                    .errorCode("YAML_PARSE_ERROR")
                    .message("Failed to parse YAML: " + e.getMessage())
                    .build());
            return errors;
        }

        // Step 2: Validate AsyncAPI version
        errors.addAll(validateAsyncApiVersion(document));

        // Step 3: Validate required fields
        errors.addAll(validateRequiredFields(document));

        // Step 4: Validate info section
        errors.addAll(validateInfo(document));

        // Step 5: Validate channels
        errors.addAll(validateChannels(document));

        // Step 6: Validate operations
        errors.addAll(validateOperations(document));

        return errors;
    }

    /**
     * Validates the AsyncAPI version field.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    private List<ValidationError> validateAsyncApiVersion(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        if (!document.containsKey("asyncapi")) {
            errors.add(ValidationError.builder()
                    .location("asyncapi")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'asyncapi'")
                    .build());
            return errors;
        }

        Object versionObj = document.get("asyncapi");
        if (!(versionObj instanceof String)) {
            errors.add(ValidationError.builder()
                    .location("asyncapi")
                    .errorCode("INVALID_DATA_TYPE")
                    .message("Field 'asyncapi' must be a string")
                    .build());
            return errors;
        }

        String version = (String) versionObj;
        if (!version.startsWith("3.")) {
            errors.add(ValidationError.builder()
                    .location("asyncapi")
                    .errorCode("UNSUPPORTED_VERSION")
                    .message("AsyncAPI version must be 3.x.x (found: " + version + ")")
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

        if (!document.containsKey("channels")) {
            errors.add(ValidationError.builder()
                    .location("channels")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'channels'")
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
     * Validates the channels section.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validateChannels(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        Object channelsObj = document.get("channels");
        if (channelsObj == null) {
            return errors; // Already reported in validateRequiredFields
        }

        if (!(channelsObj instanceof Map)) {
            errors.add(ValidationError.builder()
                    .location("channels")
                    .errorCode("INVALID_DATA_TYPE")
                    .message("Field 'channels' must be an object")
                    .build());
            return errors;
        }

        Map<String, Object> channels = (Map<String, Object>) channelsObj;

        for (Map.Entry<String, Object> channelEntry : channels.entrySet()) {
            String channelName = channelEntry.getKey();
            Object channelObj = channelEntry.getValue();

            if (!(channelObj instanceof Map)) {
                errors.add(ValidationError.builder()
                        .location("channels." + channelName)
                        .errorCode("INVALID_DATA_TYPE")
                        .message("Channel must be an object")
                        .build());
                continue;
            }

            Map<String, Object> channel = (Map<String, Object>) channelObj;

            // Validate address field (required in AsyncAPI 3.0.0)
            if (!channel.containsKey("address")) {
                errors.add(ValidationError.builder()
                        .location("channels." + channelName + ".address")
                        .errorCode("MISSING_REQUIRED_FIELD")
                        .message("Missing required field 'address'")
                        .build());
            }
        }

        return errors;
    }

    /**
     * Validates the operations section.
     *
     * @param document the parsed YAML document
     * @return list of validation errors
     */
    @SuppressWarnings("unchecked")
    private List<ValidationError> validateOperations(Map<String, Object> document) {
        List<ValidationError> errors = new ArrayList<>();

        Object operationsObj = document.get("operations");
        if (operationsObj == null) {
            // Operations are optional in AsyncAPI 3.0.0
            return errors;
        }

        if (!(operationsObj instanceof Map)) {
            errors.add(ValidationError.builder()
                    .location("operations")
                    .errorCode("INVALID_DATA_TYPE")
                    .message("Field 'operations' must be an object")
                    .build());
            return errors;
        }

        Map<String, Object> operations = (Map<String, Object>) operationsObj;

        for (Map.Entry<String, Object> operationEntry : operations.entrySet()) {
            String operationName = operationEntry.getKey();
            Object operationObj = operationEntry.getValue();

            if (!(operationObj instanceof Map)) {
                errors.add(ValidationError.builder()
                        .location("operations." + operationName)
                        .errorCode("INVALID_DATA_TYPE")
                        .message("Operation must be an object")
                        .build());
                continue;
            }

            Map<String, Object> operation = (Map<String, Object>) operationObj;
            errors.addAll(validateOperation(operation, operationName));
        }

        return errors;
    }

    /**
     * Validates a single operation.
     *
     * @param operation the operation map
     * @param operationName the operation name
     * @return list of validation errors
     */
    private List<ValidationError> validateOperation(Map<String, Object> operation, String operationName) {
        List<ValidationError> errors = new ArrayList<>();
        String location = "operations." + operationName;

        // Validate action field (required)
        if (!operation.containsKey("action")) {
            errors.add(ValidationError.builder()
                    .location(location + ".action")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'action'")
                    .build());
        } else {
            Object actionObj = operation.get("action");
            if (!(actionObj instanceof String)) {
                errors.add(ValidationError.builder()
                        .location(location + ".action")
                        .errorCode("INVALID_DATA_TYPE")
                        .message("Field 'action' must be a string")
                        .build());
            } else {
                String action = (String) actionObj;
                if (!VALID_ACTIONS.contains(action.toLowerCase())) {
                    errors.add(ValidationError.builder()
                            .location(location + ".action")
                            .errorCode("INVALID_ACTION")
                            .message("Invalid action: '" + action + "'. Valid actions: " + VALID_ACTIONS)
                            .build());
                }
            }
        }

        // Validate channel field (required)
        if (!operation.containsKey("channel")) {
            errors.add(ValidationError.builder()
                    .location(location + ".channel")
                    .errorCode("MISSING_REQUIRED_FIELD")
                    .message("Missing required field 'channel'")
                    .build());
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