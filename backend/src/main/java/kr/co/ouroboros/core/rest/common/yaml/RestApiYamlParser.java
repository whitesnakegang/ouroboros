package kr.co.ouroboros.core.rest.common.yaml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser and manager for OpenAPI YAML files (ourorest.yml).
 * <p>
 * Provides common operations for reading, writing, and manipulating OpenAPI 3.1.0 documents
 * including components/schemas and paths sections.
 * <p>
 * This class centralizes all YAML file operations to ensure consistency across
 * schema management and REST API specification management.
 *
 * @since 0.0.1
 */
@Slf4j
@Component
public class RestApiYamlParser {

    private static final String RESOURCE_PATH = System.getProperty("user.dir") + "/src/main/resources";
    private static final String YAML_FILE_PATH = "ouroboros/rest/ourorest.yml";

    private final Yaml yaml;

    public RestApiYamlParser() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    /**
     * Gets the full path to the ourorest.yml file.
     *
     * @return Path to the YAML file
     */
    public Path getYamlFilePath() {
        return Paths.get(RESOURCE_PATH, YAML_FILE_PATH);
    }

    /**
     * Checks if the YAML file exists.
     *
     * @return true if file exists, false otherwise
     */
    public boolean fileExists() {
        return Files.exists(getYamlFilePath());
    }

    /**
     * Reads the OpenAPI document from the YAML file.
     *
     * @return OpenAPI document as a map
     * @throws Exception if file reading fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readDocument() throws Exception {
        Path filePath = getYamlFilePath();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("YAML file does not exist: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Object loaded = yaml.load(fis);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
            return new LinkedHashMap<>();
        }
    }

    /**
     * Reads the OpenAPI document or creates a new one if it doesn't exist.
     *
     * @return OpenAPI document as a map
     * @throws Exception if file reading fails
     */
    public Map<String, Object> readOrCreateDocument() throws Exception {
        Path filePath = getYamlFilePath();
        if (Files.exists(filePath)) {
            return readDocument();
        }

        // Create new OpenAPI document structure
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("openapi", "3.1.0");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "API Documentation");
        info.put("version", "1.0.0");
        doc.put("info", info);

        doc.put("paths", new LinkedHashMap<>());
        doc.put("components", new LinkedHashMap<>());

        return doc;
    }

    /**
     * Writes the OpenAPI document to the YAML file.
     *
     * @param document OpenAPI document to write
     * @throws Exception if file writing fails
     */
    public void writeDocument(Map<String, Object> document) throws Exception {
        Path filePath = getYamlFilePath();

        // Ensure directory exists
        Files.createDirectories(filePath.getParent());

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            yaml.dump(document, writer);
        }

        log.debug("Wrote OpenAPI document to: {}", filePath);
    }

    /**
     * Gets or creates the components section in the OpenAPI document.
     *
     * @param openApiDoc OpenAPI document
     * @return components section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateComponents(Map<String, Object> openApiDoc) {
        Map<String, Object> components = (Map<String, Object>) openApiDoc.get("components");
        if (components == null) {
            components = new LinkedHashMap<>();
            openApiDoc.put("components", components);
        }
        return components;
    }

    /**
     * Gets or creates the schemas section in the components.
     *
     * @param components components section
     * @return schemas section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateSchemas(Map<String, Object> components) {
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        if (schemas == null) {
            schemas = new LinkedHashMap<>();
            components.put("schemas", schemas);
        }
        return schemas;
    }

    /**
     * Gets or creates the paths section in the OpenAPI document.
     *
     * @param openApiDoc OpenAPI document
     * @return paths section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreatePaths(Map<String, Object> openApiDoc) {
        Map<String, Object> paths = (Map<String, Object>) openApiDoc.get("paths");
        if (paths == null) {
            paths = new LinkedHashMap<>();
            openApiDoc.put("paths", paths);
        }
        return paths;
    }

    /**
     * Gets the schemas section from the OpenAPI document.
     *
     * @param openApiDoc OpenAPI document
     * @return schemas section, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchemas(Map<String, Object> openApiDoc) {
        Map<String, Object> components = (Map<String, Object>) openApiDoc.get("components");
        if (components == null) {
            return null;
        }
        return (Map<String, Object>) components.get("schemas");
    }

    /**
     * Gets a specific schema by name.
     *
     * @param openApiDoc OpenAPI document
     * @param schemaName schema name
     * @return schema definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchema(Map<String, Object> openApiDoc, String schemaName) {
        Map<String, Object> schemas = getSchemas(openApiDoc);
        if (schemas == null) {
            return null;
        }
        return (Map<String, Object>) schemas.get(schemaName);
    }

    /**
     * Checks if a schema exists.
     *
     * @param openApiDoc OpenAPI document
     * @param schemaName schema name
     * @return true if schema exists, false otherwise
     */
    public boolean schemaExists(Map<String, Object> openApiDoc, String schemaName) {
        return getSchema(openApiDoc, schemaName) != null;
    }

    /**
     * Adds or updates a schema in the document.
     *
     * @param openApiDoc OpenAPI document
     * @param schemaName schema name
     * @param schemaDefinition schema definition
     */
    public void putSchema(Map<String, Object> openApiDoc, String schemaName, Map<String, Object> schemaDefinition) {
        Map<String, Object> components = getOrCreateComponents(openApiDoc);
        Map<String, Object> schemas = getOrCreateSchemas(components);
        schemas.put(schemaName, schemaDefinition);
    }

    /**
     * Removes a schema from the document.
     *
     * @param openApiDoc OpenAPI document
     * @param schemaName schema name
     * @return true if schema was removed, false if it didn't exist
     */
    public boolean removeSchema(Map<String, Object> openApiDoc, String schemaName) {
        Map<String, Object> schemas = getSchemas(openApiDoc);
        if (schemas == null) {
            return false;
        }
        return schemas.remove(schemaName) != null;
    }

    /**
     * Gets a specific API path definition.
     *
     * @param openApiDoc OpenAPI document
     * @param path API path (e.g., "/api/users")
     * @return path definition containing methods, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPath(Map<String, Object> openApiDoc, String path) {
        Map<String, Object> paths = (Map<String, Object>) openApiDoc.get("paths");
        if (paths == null) {
            return null;
        }
        return (Map<String, Object>) paths.get(path);
    }

    /**
     * Gets a specific API operation (path + method).
     *
     * @param openApiDoc OpenAPI document
     * @param path API path
     * @param method HTTP method (lowercase)
     * @return operation definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperation(Map<String, Object> openApiDoc, String path, String method) {
        Map<String, Object> pathItem = getPath(openApiDoc, path);
        if (pathItem == null) {
            return null;
        }
        return (Map<String, Object>) pathItem.get(method.toLowerCase());
    }

    /**
     * Checks if an API operation exists.
     *
     * @param openApiDoc OpenAPI document
     * @param path API path
     * @param method HTTP method
     * @return true if operation exists, false otherwise
     */
    public boolean operationExists(Map<String, Object> openApiDoc, String path, String method) {
        return getOperation(openApiDoc, path, method) != null;
    }

    /**
     * Adds or updates an API operation in the document.
     *
     * @param openApiDoc OpenAPI document
     * @param path API path
     * @param method HTTP method
     * @param operation operation definition
     */
    @SuppressWarnings("unchecked")
    public void putOperation(Map<String, Object> openApiDoc, String path, String method, Map<String, Object> operation) {
        Map<String, Object> paths = getOrCreatePaths(openApiDoc);

        // Get or create path item
        Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
        if (pathItem == null) {
            pathItem = new LinkedHashMap<>();
            paths.put(path, pathItem);
        }

        // Add operation to path item
        pathItem.put(method.toLowerCase(), operation);
    }

    /**
     * Removes an API operation from the document.
     *
     * @param openApiDoc OpenAPI document
     * @param path API path
     * @param method HTTP method
     * @return true if operation was removed, false if it didn't exist
     */
    @SuppressWarnings("unchecked")
    public boolean removeOperation(Map<String, Object> openApiDoc, String path, String method) {
        Map<String, Object> pathItem = getPath(openApiDoc, path);
        if (pathItem == null) {
            return false;
        }

        boolean removed = pathItem.remove(method.toLowerCase()) != null;

        // If path item is now empty, remove the entire path
        if (pathItem.isEmpty()) {
            Map<String, Object> paths = (Map<String, Object>) openApiDoc.get("paths");
            if (paths != null) {
                paths.remove(path);
            }
        }

        return removed;
    }
}