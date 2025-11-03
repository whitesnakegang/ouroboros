package kr.co.ouroboros.core.rest.spec.validator;

import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates and enriches ourorest.yml files with required Ouroboros custom fields.
 * <p>
 * This validator automatically runs during application startup (ApplicationReadyEvent) to ensure
 * that the OpenAPI specification file contains all necessary Ouroboros-specific extensions.
 * The validation process is non-blocking and never causes application startup to fail.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Creates default template if file doesn't exist</li>
 *   <li>Validates OpenAPI 3.x structure (non-blocking warnings)</li>
 *   <li>Enriches operations with x-ouroboros-* fields</li>
 *   <li>Enriches schemas with x-ouroboros-mock and x-ouroboros-orders</li>
 *   <li>Preserves existing user values</li>
 *   <li>Non-destructive updates only</li>
 * </ul>
 * <p>
 * <b>Validation Lifecycle:</b>
 * <pre>
 * Application Startup
 *   → ApplicationReadyEvent
 *   → OpenApiDumpOnReady.onReady()
 *   → validateAndEnrich() ← Entry point
 *   → Protocol initialization continues
 * </pre>
 *
 * @see kr.co.ouroboros.core.global.runner.OpenApiDumpOnReady
 * @see kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OurorestYamlValidator {

    private final RestApiYamlParser yamlParser;
    private final SchemaValidator schemaValidator;

    // HTTP methods to check for operations
    private static final List<String> HTTP_METHODS = Arrays.asList(
            "get", "post", "put", "delete", "patch", "options", "head", "trace"
    );

    // Valid path item keys (besides HTTP methods)
    private static final List<String> VALID_PATH_ITEM_KEYS = Arrays.asList(
            "get", "post", "put", "delete", "patch", "options", "head", "trace",
            "summary", "description", "servers", "parameters", "$ref"
    );

    /**
     * Validates and enriches ourorest.yml with required Ouroboros custom fields.
     * <p>
     * This method is called during application startup (ApplicationReadyEvent) and:
     * <ol>
     *   <li>Creates default template if file doesn't exist</li>
     *   <li>Parses and validates OpenAPI structure</li>
     *   <li>Adds missing x-ouroboros-* fields to operations</li>
     *   <li>Adds missing x-ouroboros-mock and x-ouroboros-orders to schemas</li>
     *   <li>Saves enriched document back to file</li>
     * </ol>
     * <p>
     * All errors are non-blocking and logged only. The application continues startup normally
     * even if validation fails.
     */
    public void validateAndEnrich() {
        try {
            log.info("========================================");
            log.info("📝 Starting ourorest.yml validation...");
            log.info("========================================");

            // Step 1: Check file existence
            if (!yamlParser.fileExists()) {
                log.info("📝 ourorest.yml not found. Creating default template...");
                createDefaultTemplate();
                log.info("✅ Default ourorest.yml created successfully");
                log.info("========================================");
                return;
            }

            // Step 2: Parse YAML file
            Map<String, Object> document = parseYaml();
            if (document == null) {
                log.info("========================================");
                return; // Parse error already logged
            }

            // Step 3: Validate OpenAPI structure (non-blocking)
            validateOpenApiStructure(document);

            // Step 3.5: Fix malformed fields
            boolean hasFixedFields = fixMalformedFields(document);

            // Step 4: Enrich operation-level fields
            int operationCount = enrichOperations(document);

            // Step 5: Enrich schema-level fields
            int schemaCount = enrichSchemas(document);

            // Step 6: Save enriched file
            if (hasFixedFields || operationCount > 0 || schemaCount > 0) {
                yamlParser.writeDocument(document);
                log.info("💾 Saved enriched ourorest.yml");
            }

            // Summary
            log.info("========================================");
            log.info("✅ ourorest.yml Validation Completed");
            log.info("   📊 Operations enriched: {}", operationCount);
            log.info("   📊 Schemas enriched: {}", schemaCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Unexpected error during validation: {}", e.getMessage(), e);
            log.error("⚠️  Validation aborted, but application will continue startup");
            log.info("========================================");
        }
    }

    /**
     * Creates a default OpenAPI template file.
     * <p>
     * The template includes all required OpenAPI 3.1.0 fields and empty sections for
     * paths, components, schemas, servers, and security.
     *
     * @throws Exception if file creation fails
     */
    private void createDefaultTemplate() throws Exception {
        Map<String, Object> document = yamlParser.readOrCreateDocument();
        yamlParser.writeDocument(document);
    }

    /**
     * Parses the YAML file and returns the document map.
     * <p>
     * Logs ERROR and returns null if parsing fails. Does not throw exceptions.
     *
     * @return the parsed document, or null if parsing failed
     */
    private Map<String, Object> parseYaml() {
        try {
            Map<String, Object> doc = yamlParser.readDocument();
            log.info("✅ Successfully parsed ourorest.yml");
            return doc;
        } catch (Exception e) {
            log.error("❌ Failed to parse ourorest.yml: {}", e.getMessage());
            log.error("⚠️  Validation aborted. Please fix YAML syntax errors.");
            return null;
        }
    }

    /**
     * Validates the OpenAPI structure with non-blocking warnings.
     * <p>
     * Checks for:
     * <ul>
     *   <li>'openapi' field with version 3.x.x</li>
     *   <li>'info' field with title and version</li>
     *   <li>'paths' field (can be empty)</li>
     * </ul>
     * <p>
     * All checks are non-blocking and only log warnings.
     *
     * @param document the OpenAPI document to validate
     */
    private void validateOpenApiStructure(Map<String, Object> document) {
        // Check 'openapi' field
        if (!document.containsKey("openapi")) {
            log.warn("⚠️  Missing 'openapi' field in ourorest.yml");
            log.warn("    Expected: openapi: 3.1.0");
        } else {
            Object versionObj = document.get("openapi");
            if (versionObj instanceof String) {
                String version = (String) versionObj;
                if (!version.startsWith("3.")) {
                    log.warn("⚠️  Unsupported OpenAPI version: {}", version);
                    log.warn("    Expected: 3.x.x");
                }
            }
        }

        // Check 'info' field
        if (!document.containsKey("info")) {
            log.warn("⚠️  Missing 'info' field in ourorest.yml");
        }

        // Check 'paths' field
        if (!document.containsKey("paths")) {
            log.warn("⚠️  Missing 'paths' field in ourorest.yml");
        }
    }

    /**
     * Fixes malformed fields in the OpenAPI document.
     * <p>
     * Corrects common issues:
     * <ul>
     *   <li>Replaces null components with empty object</li>
     *   <li>Replaces null paths with empty object</li>
     *   <li>Ensures components.schemas exists</li>
     * </ul>
     *
     * @param document the OpenAPI document to fix
     * @return true if any fields were fixed, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean fixMalformedFields(Map<String, Object> document) {
        boolean fixed = false;

        // Fix components: null → components: {}
        if (document.containsKey("components") && document.get("components") == null) {
            Map<String, Object> components = new LinkedHashMap<>();
            components.put("schemas", new LinkedHashMap<>());
            document.put("components", components);
            log.info("🔧 Fixed: components: null → components: {{schemas: {{}}}}");
            fixed = true;
        } else if (document.containsKey("components")) {
            // Ensure components.schemas exists
            Map<String, Object> components = (Map<String, Object>) document.get("components");
            if (components != null && !components.containsKey("schemas")) {
                components.put("schemas", new LinkedHashMap<>());
                log.info("🔧 Fixed: Added missing components.schemas");
                fixed = true;
            }
        }

        // Fix paths: null → paths: {}
        if (document.containsKey("paths") && document.get("paths") == null) {
            document.put("paths", new LinkedHashMap<>());
            log.info("🔧 Fixed: paths: null → paths: {{}}");
            fixed = true;
        }

        // Fix security: null → security: []
        if (document.containsKey("security") && document.get("security") == null) {
            document.put("security", new ArrayList<>());
            log.info("🔧 Fixed: security: null → security: []");
            fixed = true;
        }

        if (fixed) {
            log.info("ℹ️  Malformed fields have been corrected");
        }

        return fixed;
    }

    /**
     * Enriches all operations in the document with missing x-ouroboros-* fields.
     * <p>
     * Iterates through all paths and HTTP methods, adding:
     * <ul>
     *   <li>x-ouroboros-id (UUID)</li>
     *   <li>x-ouroboros-progress ("mock")</li>
     *   <li>x-ouroboros-tag ("none")</li>
     *   <li>x-ouroboros-diff ("none")</li>
     * </ul>
     * <p>
     * Only adds fields that don't already exist (non-destructive).
     *
     * @param document the OpenAPI document
     * @return the number of operations that were enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichOperations(Map<String, Object> document) {
        int enrichedCount = 0;
        Map<String, Object> paths = (Map<String, Object>) document.get("paths");

        if (paths == null || paths.isEmpty()) {
            log.info("ℹ️  No paths to enrich");
            return 0;
        }

        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            if (!(pathEntry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

            // Check for invalid keys in path item
            for (String key : pathItem.keySet()) {
                if (!VALID_PATH_ITEM_KEYS.contains(key) && !key.startsWith("x-")) {
                    log.warn("⚠️  Invalid key '{}' in path '{}' (will be ignored)", key, path);

                    // Suggest common typos
                    if (key.equalsIgnoreCase("gets")) {
                        log.warn("    Did you mean 'get'?");
                    } else if (key.equalsIgnoreCase("posts")) {
                        log.warn("    Did you mean 'post'?");
                    } else if (key.equalsIgnoreCase("puts")) {
                        log.warn("    Did you mean 'put'?");
                    } else if (key.equalsIgnoreCase("deletes")) {
                        log.warn("    Did you mean 'delete'?");
                    }
                }
            }

            for (String method : HTTP_METHODS) {
                if (pathItem.containsKey(method)) {
                    Object operationObj = pathItem.get(method);
                    if (operationObj instanceof Map) {
                        Map<String, Object> operation = (Map<String, Object>) operationObj;
                        if (enrichOperationFields(operation)) {
                            enrichedCount++;
                            log.debug("🔧 Enriched {} {}", method.toUpperCase(), path);
                        }
                    }
                }
            }
        }

        return enrichedCount;
    }

    /**
     * Enriches a single operation with missing x-ouroboros-* fields.
     * <p>
     * Adds the following fields if they don't exist:
     * <ul>
     *   <li>x-ouroboros-id: UUID.randomUUID()</li>
     *   <li>x-ouroboros-progress: "mock"</li>
     *   <li>x-ouroboros-tag: "none"</li>
     *   <li>x-ouroboros-diff: "none"</li>
     * </ul>
     *
     * @param operation the operation map to enrich
     * @return true if any field was added, false if all fields already existed
     */
    private boolean enrichOperationFields(Map<String, Object> operation) {
        boolean modified = false;

        if (!operation.containsKey("x-ouroboros-id")) {
            operation.put("x-ouroboros-id", UUID.randomUUID().toString());
            modified = true;
        }

        if (!operation.containsKey("x-ouroboros-progress")) {
            operation.put("x-ouroboros-progress", "mock");
            modified = true;
        }

        if (!operation.containsKey("x-ouroboros-tag")) {
            operation.put("x-ouroboros-tag", "none");
            modified = true;
        }

        if (!operation.containsKey("x-ouroboros-diff")) {
            operation.put("x-ouroboros-diff", "none");
            modified = true;
        }

        return modified;
    }

    /**
     * Enriches all schemas in the document with missing x-ouroboros-* fields.
     * <p>
     * Processes both:
     * <ul>
     *   <li>Component schemas in components/schemas section</li>
     *   <li>Inline schemas in operation responses and requestBody</li>
     * </ul>
     *
     * @param document the OpenAPI document
     * @return the number of schemas that were enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichSchemas(Map<String, Object> document) {
        int enrichedCount = 0;

        // Enrich component schemas
        Map<String, Object> components = (Map<String, Object>) document.get("components");
        if (components != null) {
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
            if (schemas != null) {
                for (Object schemaObj : schemas.values()) {
                    if (schemaObj instanceof Map) {
                        if (enrichSchemaFields((Map<String, Object>) schemaObj)) {
                            enrichedCount++;
                        }
                    }
                }
            }
        }

        // Enrich inline schemas in paths
        Map<String, Object> paths = (Map<String, Object>) document.get("paths");
        if (paths != null) {
            enrichedCount += enrichInlineSchemas(paths);
        }

        return enrichedCount;
    }

    /**
     * Enriches inline schemas found in operation responses and requestBody.
     * <p>
     * Navigates through paths → methods → responses/requestBody → content → mediaTypes → schema
     *
     * @param paths the paths section of the OpenAPI document
     * @return the number of inline schemas that were enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichInlineSchemas(Map<String, Object> paths) {
        int count = 0;

        for (Object pathItemObj : paths.values()) {
            if (!(pathItemObj instanceof Map)) continue;
            Map<String, Object> pathItem = (Map<String, Object>) pathItemObj;

            for (String method : HTTP_METHODS) {
                if (!pathItem.containsKey(method)) continue;
                Object operationObj = pathItem.get(method);
                if (!(operationObj instanceof Map)) continue;
                Map<String, Object> operation = (Map<String, Object>) operationObj;

                // Check responses
                Object responsesObj = operation.get("responses");
                if (responsesObj instanceof Map) {
                    Map<String, Object> responses = (Map<String, Object>) responsesObj;
                    for (Object responseObj : responses.values()) {
                        if (responseObj instanceof Map) {
                            count += enrichSchemasInContent((Map<String, Object>) responseObj);
                        }
                    }
                }

                // Check requestBody
                Object requestBodyObj = operation.get("requestBody");
                if (requestBodyObj instanceof Map) {
                    count += enrichSchemasInContent((Map<String, Object>) requestBodyObj);
                }
            }
        }

        return count;
    }

    /**
     * Enriches schemas found in a content section (response or requestBody).
     * <p>
     * Navigates through content → mediaTypes → schema
     *
     * @param contentContainer the response or requestBody map containing a 'content' field
     * @return the number of schemas enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichSchemasInContent(Map<String, Object> contentContainer) {
        int count = 0;

        Object contentObj = contentContainer.get("content");
        if (!(contentObj instanceof Map)) return 0;
        Map<String, Object> content = (Map<String, Object>) contentObj;

        for (Object mediaTypeObj : content.values()) {
            if (!(mediaTypeObj instanceof Map)) continue;
            Map<String, Object> mediaType = (Map<String, Object>) mediaTypeObj;

            Object schemaObj = mediaType.get("schema");
            if (schemaObj instanceof Map) {
                Map<String, Object> schema = (Map<String, Object>) schemaObj;
                // Skip $ref schemas
                if (!schema.containsKey("$ref") && !schema.containsKey("ref")) {
                    if (enrichSchemaFields(schema)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Enriches a schema with missing x-ouroboros-* fields.
     * <p>
     * Adds:
     * <ul>
     *   <li>x-ouroboros-mock: "" to each property (if schema has properties)</li>
     *   <li>x-ouroboros-orders: array of property keys (if schema has properties)</li>
     * </ul>
     * <p>
     * Only processes schemas that have a 'properties' field. Skips $ref schemas.
     * Also validates and corrects minItems/maxItems constraints.
     *
     * @param schema the schema map to enrich
     * @return true if any field was added or corrected, false if all fields already existed
     */
    @SuppressWarnings("unchecked")
    private boolean enrichSchemaFields(Map<String, Object> schema) {
        if (schema == null || !schema.containsKey("properties")) {
            return false;
        }

        boolean modified = false;

        // Validate schema constraints (minItems/maxItems, etc.)
        if (schemaValidator.validateAndCorrectSchemaMap(schema)) {
            modified = true;
        }

        Object propertiesObj = schema.get("properties");
        if (!(propertiesObj instanceof Map)) {
            return modified;
        }
        Map<String, Object> properties = (Map<String, Object>) propertiesObj;

        // Add x-ouroboros-mock: "" to each property
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object propertyObj = entry.getValue();
            if (propertyObj instanceof Map) {
                Map<String, Object> property = (Map<String, Object>) propertyObj;
                // Skip $ref properties
                if (!property.containsKey("$ref") && !property.containsKey("ref")) {
                    if (!property.containsKey("x-ouroboros-mock")) {
                        property.put("x-ouroboros-mock", "");
                        modified = true;
                    }
                }
            }
        }

        // Add x-ouroboros-orders array
        if (!schema.containsKey("x-ouroboros-orders")) {
            schema.put("x-ouroboros-orders", new ArrayList<>(properties.keySet()));
            modified = true;
        }

        return modified;
    }
}
