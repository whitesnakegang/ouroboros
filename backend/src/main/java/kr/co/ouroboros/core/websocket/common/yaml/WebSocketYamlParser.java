package kr.co.ouroboros.core.websocket.common.yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.global.properties.OuroborosProperties;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
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
 * Parser and manager for AsyncAPI YAML files (ourowebsocket.yml).
 * <p>
 * Provides common operations for reading, writing, and manipulating AsyncAPI 3.0.0 documents
 * including components/schemas and components/messages sections.
 * <p>
 * This class centralizes all YAML file operations to ensure consistency across
 * schema management and WebSocket API specification management.
 *
 * @since 0.1.0
 */
@Slf4j
@Component
public class WebSocketYamlParser {

    private static final String RESOURCE_PATH = System.getProperty("user.dir") + "/src/main/resources";
    private static final String YAML_FILE_PATH = "ouroboros/websocket/ourowebsocket.yml";

    private final LoaderOptions loaderOptions;
    private final DumperOptions dumperOptions;
    private final OuroborosProperties properties;
    private final ObjectMapper objectMapper;
    private final OuroApiSpecManager specManager;

    public WebSocketYamlParser(OuroborosProperties properties, OuroApiSpecManager specManager) {
        this.loaderOptions = new LoaderOptions();
        this.loaderOptions.setCodePointLimit(50 * 1024 * 1024); // 50MB limit to prevent buffer overflow

        this.dumperOptions = new DumperOptions();
        this.dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.dumperOptions.setPrettyFlow(true);
        this.dumperOptions.setIndent(2);

        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.specManager = specManager;
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
     * Gets the full path to the ourowebsocket.yml file.
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
     * Reads the AsyncAPI document from OuroApiSpecManager cache.
     * Falls back to direct file reading if cache is not available (e.g., during initialization).
     * Returns a deep copy to prevent cache pollution.
     *
     * @return AsyncAPI document as a map (deep copy)
     * @throws Exception if file reading fails or cache is not initialized
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readDocument() throws Exception {
        Path filePath = getYamlFilePath();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("YAML file does not exist: " + filePath);
        }

        try {
            // Try to get cached spec from OuroApiSpecManager
            OuroApiSpec cachedSpec = specManager.getApiSpec(Protocol.WEB_SOCKET);
            Map<String, Object> document = specManager.convertSpecToMap(cachedSpec);
            return deepCopy(document);
        } catch (Exception e) {
            // Fallback: read directly from file during initialization
            log.debug("Cache not available, reading directly from file: {}", e.getMessage());
            return readDocumentDirectly();
        }
    }

    /**
     * Reads the AsyncAPI document directly from the file without using cache.
     * Used as fallback during initialization when cache is not yet available.
     *
     * @return AsyncAPI document as a map
     * @throws Exception if file reading fails
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readDocumentDirectly() throws Exception {
        Path filePath = getYamlFilePath();
        try (InputStream is = Files.newInputStream(filePath)) {
            Yaml yaml = createYaml();
            Object loaded = yaml.load(is);
            Map<String, Object> document = (loaded instanceof Map)
                    ? (Map<String, Object>) loaded
                    : new LinkedHashMap<>();
            return deepCopy(document);
        }
    }

    /**
     * Reads or creates a new AsyncAPI document.
     * If the file doesn't exist, creates a minimal AsyncAPI 3.0.0 document structure.
     *
     * @return AsyncAPI document as a map
     * @throws Exception if file operations fail
     */
    public Map<String, Object> readOrCreateDocument() throws Exception {
        Map<String, Object> doc;
        if (fileExists()) {
            doc = readDocument();
        } else {
            doc = createMinimalDocument();
        }

        // Ensure required AsyncAPI 3.0.0 fields are present
        ensureRequiredFields(doc);
        return doc;
    }

    // 이 메서드를 추가
    private void ensureRequiredFields(Map<String, Object> doc) {
        // Ensure asyncapi version
        if (!doc.containsKey("asyncapi")) {
            doc.put("asyncapi", "3.0.0");
        }

        // Ensure info section
        if (!doc.containsKey("info")) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("title", "WebSocket API Documentation");
            info.put("version", "1.0.0");
            doc.put("info", info);
        }

        // Ensure defaultContentType
        if (!doc.containsKey("defaultContentType")) {
            doc.put("defaultContentType", "application/json");
        }

        // Ensure required sections exist
        if (!doc.containsKey("servers")) {
            doc.put("servers", new LinkedHashMap<>());
        }
        if (!doc.containsKey("channels")) {
            doc.put("channels", new LinkedHashMap<>());
        }
        if (!doc.containsKey("operations")) {
            doc.put("operations", new LinkedHashMap<>());
        }
        if (!doc.containsKey("components")) {
            doc.put("components", new LinkedHashMap<>());
        }
    }


    /**
     * Creates a minimal AsyncAPI 3.0.0 document structure.
     *
     * @return new AsyncAPI document with basic structure
     */
    private Map<String, Object> createMinimalDocument() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("asyncapi", "3.0.0");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "WebSocket API Documentation");
        info.put("version", "1.0.0");
        doc.put("info", info);

        doc.put("defaultContentType", "application/json");
        doc.put("servers", new LinkedHashMap<>());
        doc.put("channels", new LinkedHashMap<>());
        doc.put("operations", new LinkedHashMap<>());
        doc.put("components", new LinkedHashMap<>());

        return doc;
    }

    /**
     * Writes the AsyncAPI document to the YAML file.
     *
     * @param document AsyncAPI document to write
     * @throws IOException if writing fails
     */
    public void writeDocument(Map<String, Object> document) throws IOException {
        Path filePath = getYamlFilePath();
        Files.createDirectories(filePath.getParent());

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            Yaml yaml = createYaml();
            yaml.dump(document, writer);
        }
    }

    /**
     * Gets or creates the components section in the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return components section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateComponents(Map<String, Object> asyncApiDoc) {
        Map<String, Object> components = (Map<String, Object>) asyncApiDoc.get("components");
        if (components == null) {
            components = new LinkedHashMap<>();
            asyncApiDoc.put("components", components);
        }
        return components;
    }

    /**
     * Gets or creates the schemas section in components.
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
     * Gets or creates the messages section in components.
     *
     * @param components components section
     * @return messages section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateMessages(Map<String, Object> components) {
        Map<String, Object> messages = (Map<String, Object>) components.get("messages");
        if (messages == null) {
            messages = new LinkedHashMap<>();
            components.put("messages", messages);
        }
        return messages;
    }

    /**
     * Gets the schemas section from the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return schemas section, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchemas(Map<String, Object> asyncApiDoc) {
        Map<String, Object> components = (Map<String, Object>) asyncApiDoc.get("components");
        if (components == null) {
            return null;
        }
        return (Map<String, Object>) components.get("schemas");
    }

    /**
     * Gets a specific schema by name.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param schemaName schema name
     * @return schema definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchema(Map<String, Object> asyncApiDoc, String schemaName) {
        Map<String, Object> schemas = getSchemas(asyncApiDoc);
        if (schemas == null) {
            return null;
        }
        return (Map<String, Object>) schemas.get(schemaName);
    }

    /**
     * Checks if a schema exists.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param schemaName schema name
     * @return true if schema exists, false otherwise
     */
    public boolean schemaExists(Map<String, Object> asyncApiDoc, String schemaName) {
        return getSchema(asyncApiDoc, schemaName) != null;
    }

    /**
     * Adds or updates a schema in the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param schemaName schema name
     * @param schemaDefinition schema definition
     */
    public void putSchema(Map<String, Object> asyncApiDoc, String schemaName, Map<String, Object> schemaDefinition) {
        Map<String, Object> components = getOrCreateComponents(asyncApiDoc);
        Map<String, Object> schemas = getOrCreateSchemas(components);
        schemas.put(schemaName, schemaDefinition);
    }

    /**
     * Removes a schema from the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param schemaName schema name
     * @return true if schema was removed, false if it didn't exist
     */
    public boolean removeSchema(Map<String, Object> asyncApiDoc, String schemaName) {
        Map<String, Object> schemas = getSchemas(asyncApiDoc);
        if (schemas == null) {
            return false;
        }
        return schemas.remove(schemaName) != null;
    }

    /**
     * Gets the messages section from the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return messages section, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMessages(Map<String, Object> asyncApiDoc) {
        Map<String, Object> components = (Map<String, Object>) asyncApiDoc.get("components");
        if (components == null) {
            return null;
        }
        return (Map<String, Object>) components.get("messages");
    }

    /**
     * Gets a specific message by name.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param messageName message name
     * @return message definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMessage(Map<String, Object> asyncApiDoc, String messageName) {
        Map<String, Object> messages = getMessages(asyncApiDoc);
        if (messages == null) {
            return null;
        }
        return (Map<String, Object>) messages.get(messageName);
    }

    /**
     * Checks if a message exists.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param messageName message name
     * @return true if message exists, false otherwise
     */
    public boolean messageExists(Map<String, Object> asyncApiDoc, String messageName) {
        return getMessage(asyncApiDoc, messageName) != null;
    }

    /**
     * Adds or updates a message in the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param messageName message name
     * @param messageDefinition message definition
     */
    public void putMessage(Map<String, Object> asyncApiDoc, String messageName, Map<String, Object> messageDefinition) {
        Map<String, Object> components = getOrCreateComponents(asyncApiDoc);
        Map<String, Object> messages = getOrCreateMessages(components);
        messages.put(messageName, messageDefinition);
    }

    /**
     * Removes a message from the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param messageName message name
     * @return true if message was removed, false if it didn't exist
     */
    public boolean removeMessage(Map<String, Object> asyncApiDoc, String messageName) {
        Map<String, Object> messages = getMessages(asyncApiDoc);
        if (messages == null) {
            return false;
        }
        return messages.remove(messageName) != null;
    }

    /**
     * Gets or creates the operations section in the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return operations section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateOperations(Map<String, Object> asyncApiDoc) {
        Map<String, Object> operations = (Map<String, Object>) asyncApiDoc.get("operations");
        if (operations == null) {
            operations = new LinkedHashMap<>();
            asyncApiDoc.put("operations", operations);
        }
        return operations;
    }

    /**
     * Gets the operations section from the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return operations section, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperations(Map<String, Object> asyncApiDoc) {
        return (Map<String, Object>) asyncApiDoc.get("operations");
    }

    /**
     * Gets a specific operation by name.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param operationName operation name
     * @return operation definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperation(Map<String, Object> asyncApiDoc, String operationName) {
        Map<String, Object> operations = getOperations(asyncApiDoc);
        if (operations == null) {
            return null;
        }
        return (Map<String, Object>) operations.get(operationName);
    }

    /**
     * Finds an operation by its x-ouroboros-id.
     * <p>
     * Iterates through all operations to find one with matching id.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param id x-ouroboros-id to search for
     * @return map entry containing operation name and definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map.Entry<String, Map<String, Object>> findOperationById(Map<String, Object> asyncApiDoc, String id) {
        Map<String, Object> operations = getOperations(asyncApiDoc);
        if (operations == null || operations.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, Object> entry : operations.entrySet()) {
            Map<String, Object> operationDef = (Map<String, Object>) entry.getValue();
            String operationId = (String) operationDef.get("x-ouroboros-id");
            if (id.equals(operationId)) {
                return new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), operationDef);
            }
        }

        return null;
    }

    /**
     * Checks if an operation exists.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param operationName operation name
     * @return true if operation exists, false otherwise
     */
    public boolean operationExists(Map<String, Object> asyncApiDoc, String operationName) {
        return getOperation(asyncApiDoc, operationName) != null;
    }

    /**
     * Adds or updates an operation in the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param operationName operation name
     * @param operationDefinition operation definition
     */
    public void putOperation(Map<String, Object> asyncApiDoc, String operationName, Map<String, Object> operationDefinition) {
        Map<String, Object> operations = getOrCreateOperations(asyncApiDoc);
        operations.put(operationName, operationDefinition);
    }

    /**
     * Removes an operation from the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param operationName operation name
     * @return true if operation was removed, false if it didn't exist
     */
    public boolean removeOperation(Map<String, Object> asyncApiDoc, String operationName) {
        Map<String, Object> operations = getOperations(asyncApiDoc);
        if (operations == null) {
            return false;
        }
        return operations.remove(operationName) != null;
    }

    /**
     * Gets or creates the channels section in the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return channels section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateChannels(Map<String, Object> asyncApiDoc) {
        Map<String, Object> channels = (Map<String, Object>) asyncApiDoc.get("channels");
        if (channels == null) {
            channels = new LinkedHashMap<>();
            asyncApiDoc.put("channels", channels);
        }
        return channels;
    }

    /**
     * Gets the channels section from the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return channels section, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getChannels(Map<String, Object> asyncApiDoc) {
        return (Map<String, Object>) asyncApiDoc.get("channels");
    }

    /**
     * Gets a specific channel by name.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name
     * @return channel definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getChannel(Map<String, Object> asyncApiDoc, String channelName) {
        Map<String, Object> channels = getChannels(asyncApiDoc);
        if (channels == null) {
            return null;
        }
        return (Map<String, Object>) channels.get(channelName);
    }

    /**
     * Checks if a channel exists.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name
     * @return true if channel exists, false otherwise
     */
    public boolean channelExists(Map<String, Object> asyncApiDoc, String channelName) {
        return getChannel(asyncApiDoc, channelName) != null;
    }

    /**
     * Adds or updates a channel in the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name
     * @param channelDefinition channel definition
     */
    public void putChannel(Map<String, Object> asyncApiDoc, String channelName, Map<String, Object> channelDefinition) {
        Map<String, Object> channels = getOrCreateChannels(asyncApiDoc);
        channels.put(channelName, channelDefinition);
    }

    /**
     * Removes a channel from the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param channelName channel name
     * @return true if channel was removed, false if it didn't exist
     */
    public boolean removeChannel(Map<String, Object> asyncApiDoc, String channelName) {
        Map<String, Object> channels = getChannels(asyncApiDoc);
        if (channels == null) {
            return false;
        }
        return channels.remove(channelName) != null;
    }

    /**
     * Gets or creates the servers section in the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return servers section
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrCreateServers(Map<String, Object> asyncApiDoc) {
        Map<String, Object> servers = (Map<String, Object>) asyncApiDoc.get("servers");
        if (servers == null) {
            servers = new LinkedHashMap<>();
            asyncApiDoc.put("servers", servers);
        }
        return servers;
    }

    /**
     * Gets the servers section from the AsyncAPI document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @return servers section, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getServers(Map<String, Object> asyncApiDoc) {
        return (Map<String, Object>) asyncApiDoc.get("servers");
    }

    /**
     * Gets a specific server by name.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param serverName server name
     * @return server definition, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getServer(Map<String, Object> asyncApiDoc, String serverName) {
        Map<String, Object> servers = getServers(asyncApiDoc);
        if (servers == null) {
            return null;
        }
        return (Map<String, Object>) servers.get(serverName);
    }

    /**
     * Checks if a server exists.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param serverName server name
     * @return true if server exists, false otherwise
     */
    public boolean serverExists(Map<String, Object> asyncApiDoc, String serverName) {
        return getServer(asyncApiDoc, serverName) != null;
    }

    /**
     * Adds or updates a server in the document.
     *
     * @param asyncApiDoc AsyncAPI document
     * @param serverName server name
     * @param serverDefinition server definition
     */
    public void putServer(Map<String, Object> asyncApiDoc, String serverName, Map<String, Object> serverDefinition) {
        Map<String, Object> servers = getOrCreateServers(asyncApiDoc);
        servers.put(serverName, serverDefinition);
    }

    /**
     * Creates a deep copy of the document to prevent cache pollution.
     *
     * @param document original document
     * @return deep copy of the document
     * @throws Exception if copying fails
     */
    private Map<String, Object> deepCopy(Map<String, Object> document) throws Exception {
        return objectMapper.readValue(
                objectMapper.writeValueAsString(document),
                new TypeReference<Map<String, Object>>() {}
        );
    }
}