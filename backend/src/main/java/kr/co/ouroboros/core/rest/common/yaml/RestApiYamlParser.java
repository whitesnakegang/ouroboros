package kr.co.ouroboros.core.rest.common.yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.global.properties.OuroborosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

    private final LoaderOptions loaderOptions;
    private final DumperOptions dumperOptions;
    private final OuroborosProperties properties;
    private final ObjectMapper objectMapper;

    // Cache fields
    private volatile Map<String, Object> cachedDocument;
    private volatile long cachedFileTimestamp;
    private final Object cacheLock = new Object();

    public RestApiYamlParser(OuroborosProperties properties) {
        this.loaderOptions = new LoaderOptions();
        this.loaderOptions.setCodePointLimit(50 * 1024 * 1024); // 50MB limit to prevent buffer overflow

        this.dumperOptions = new DumperOptions();
        this.dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.dumperOptions.setPrettyFlow(true);
        this.dumperOptions.setIndent(2);

        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new Yaml instance for thread-safe parsing.
     * SnakeYAML Yaml instances are not thread-safe, so we create a new one for each operation.
     *
     * @return new Yaml instance
     */
    private Yaml createYaml() {
        SafeConstructor constructor = new SafeConstructor(loaderOptions);
        Representer representer = new Representer(dumperOptions);
        return new Yaml(constructor, representer, dumperOptions);
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
     * Reads the OpenAPI document from the YAML file with caching.
     * Uses file timestamp to invalidate cache if file was modified externally.
     * Returns a deep copy to prevent cache pollution.
     *
     * @return OpenAPI document as a map (deep copy)
     * @throws Exception if file reading fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readDocument() throws Exception {
        Path filePath = getYamlFilePath();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("YAML file does not exist: " + filePath);
        }

        long currentTimestamp = Files.getLastModifiedTime(filePath).toMillis();

        // Check if cache is valid (fast path - no synchronization needed for read)
        if (cachedDocument != null && cachedFileTimestamp == currentTimestamp) {
            log.debug("Cache hit for OpenAPI document (timestamp: {})", currentTimestamp);
            return deepCopy(cachedDocument);
        }

        // Cache miss - need to load from file
        synchronized (cacheLock) {
            // Double-check: another thread might have loaded it while we were waiting
            if (cachedDocument != null && cachedFileTimestamp == currentTimestamp) {
                log.debug("Cache hit after waiting (timestamp: {})", currentTimestamp);
                return deepCopy(cachedDocument);
            }

            // Load from file
            log.debug("Cache miss - loading from file (timestamp: {})", currentTimestamp);
            try (InputStream is = Files.newInputStream(filePath)) {
                Yaml yaml = createYaml();
                Object loaded = yaml.load(is);
                Map<String, Object> document = (loaded instanceof Map)
                        ? (Map<String, Object>) loaded
                        : new LinkedHashMap<>();

                // Update cache
                cachedDocument = deepCopy(document);
                cachedFileTimestamp = currentTimestamp;

                log.debug("OpenAPI document cached (size: {} keys, timestamp: {})",
                        document.size(), currentTimestamp);

                return deepCopy(document);
            }
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

        // Initialize components with schemas section
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("schemas", new LinkedHashMap<>());
        doc.put("components", components);

        // Add servers section from properties
        java.util.List<Map<String, Object>> servers = new java.util.ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", properties.getServer().getUrl());
        server.put("description", properties.getServer().getDescription());
        servers.add(server);
        doc.put("servers", servers);

        // Add security section (empty array)
        doc.put("security", new java.util.ArrayList<>());

        return doc;
    }

    /**
     * Writes the OpenAPI document to the YAML file and updates cache.
     * Uses write-through caching strategy.
     *
     * @param document OpenAPI document to write
     * @throws Exception if file writing fails
     */
    public void writeDocument(Map<String, Object> document) throws Exception {
        Path filePath = getYamlFilePath();

        synchronized (cacheLock) {
            // Ensure directory exists
            Files.createDirectories(filePath.getParent());

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                Yaml yaml = createYaml();
                yaml.dump(document, writer);
            }

            // Update cache with write-through strategy
            long newTimestamp = Files.getLastModifiedTime(filePath).toMillis();
            cachedDocument = document;
            cachedFileTimestamp = newTimestamp;

            log.debug("Wrote OpenAPI document to: {} (cached with timestamp: {})",
                    filePath, newTimestamp);
        }
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

    /**
     * Invalidates the cache manually.
     * Useful when file is modified externally or for testing purposes.
     */
    public void invalidateCache() {
        synchronized (cacheLock) {
            cachedDocument = null;
            cachedFileTimestamp = 0;
            log.debug("Cache invalidated manually");
        }
    }

    /**
     * Deep copies a document using Jackson serialization/deserialization.
     * This prevents cache pollution by ensuring callers cannot modify the cached instance.
     *
     * @param original the original document to copy
     * @return a deep copy of the document
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> original) {
        try {
            // Serialize to bytes and deserialize back - creates complete deep copy
            byte[] bytes = objectMapper.writeValueAsBytes(original);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("Failed to deep copy OpenAPI document, returning original (UNSAFE!)", e);
            // Fallback: return original (not ideal but prevents total failure)
            return original;
        }
    }
}