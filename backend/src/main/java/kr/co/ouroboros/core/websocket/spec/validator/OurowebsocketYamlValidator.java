package kr.co.ouroboros.core.websocket.spec.validator;

import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates and enriches ourowebsocket.yml files with required Ouroboros custom fields.
 * <p>
 * This validator automatically runs during application startup (ApplicationReadyEvent) to ensure
 * that the AsyncAPI specification file contains all necessary Ouroboros-specific extensions.
 * The validation process is non-blocking and never causes application startup to fail.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Creates default template if file doesn't exist</li>
 *   <li>Validates AsyncAPI 3.x structure (non-blocking warnings)</li>
 *   <li>Enriches channels with x-ouroboros-* fields</li>
 *   <li>Enriches operations with x-ouroboros-* fields</li>
 *   <li>Enriches message payloads with x-ouroboros-mock and x-ouroboros-orders</li>
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
 * @see kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OurowebsocketYamlValidator {

    private final WebSocketYamlParser yamlParser;

    // Valid action types for AsyncAPI operations
    private static final List<String> VALID_ACTIONS = Arrays.asList("send", "receive");

    /**
     * Validates and enriches ourowebsocket.yml with required Ouroboros custom fields.
     * <p>
     * This method is called during application startup (ApplicationReadyEvent) and:
     * <ol>
     *   <li>Creates default template if file doesn't exist</li>
     *   <li>Parses and validates AsyncAPI structure</li>
     *   <li>Adds missing x-ouroboros-* fields to channels</li>
     *   <li>Adds missing x-ouroboros-* fields to operations</li>
     *   <li>Adds missing x-ouroboros-mock and x-ouroboros-orders to message payloads</li>
     *   <li>Saves enriched document back to file</li>
     * </ol>
     * <p>
     * All errors are non-blocking and logged only. The application continues startup normally
     * even if validation fails.
     */
    public void validateAndEnrich() {
        try {
            log.info("========================================");
            log.info("📝 Starting ourowebsocket.yml validation...");
            log.info("========================================");

            // Step 1: Check file existence
            if (!yamlParser.fileExists()) {
                log.info("📝 ourowebsocket.yml not found. Creating default template...");
                createDefaultTemplate();
                log.info("✅ Default ourowebsocket.yml created successfully");
                log.info("========================================");
                return;
            }

            // Step 2: Parse YAML file
            Map<String, Object> document = parseYaml();
            if (document == null) {
                log.info("========================================");
                return; // Parse error already logged
            }

            // Step 3: Validate AsyncAPI structure (non-blocking)
            validateAsyncApiStructure(document);

            // Step 4: Fix malformed fields
            boolean hasFixedFields = fixMalformedFields(document);

            // Step 5: Enrich channel-level fields
            int channelCount = enrichChannels(document);

            // Step 6: Enrich operation-level fields
            int operationCount = enrichOperations(document);

            // Step 7: Enrich message payloads
            int messageCount = enrichMessages(document);

            // Step 8: Save enriched file
            if (hasFixedFields || channelCount > 0 || operationCount > 0 || messageCount > 0) {
                yamlParser.writeDocument(document);
                log.info("💾 Saved enriched ourowebsocket.yml");
            }

            // Summary
            log.info("========================================");
            log.info("✅ ourowebsocket.yml Validation Completed");
            log.info("   📊 Channels enriched: {}", channelCount);
            log.info("   📊 Operations enriched: {}", operationCount);
            log.info("   📊 Messages enriched: {}", messageCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ Unexpected error during validation: {}", e.getMessage(), e);
            log.error("⚠️  Validation aborted, but application will continue startup");
            log.info("========================================");
        }
    }

    /**
     * Creates a default AsyncAPI template file.
     * <p>
     * The template includes all required AsyncAPI 3.0.0 fields and empty sections for
     * channels, operations, components, servers.
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
            Map<String, Object> doc = yamlParser.readOrCreateDocument();
            log.info("✅ Successfully parsed ourowebsocket.yml");
            return doc;
        } catch (Exception e) {
            log.error("❌ Failed to parse ourowebsocket.yml: {}", e.getMessage());
            log.error("⚠️  Validation aborted. Please fix YAML syntax errors.");
            return null;
        }
    }

    /**
     * Validates the AsyncAPI structure with non-blocking warnings.
     * <p>
     * Checks for:
     * <ul>
     *   <li>'asyncapi' field with version 3.x.x</li>
     *   <li>'info' field with title and version</li>
     *   <li>'channels' field (can be empty)</li>
     *   <li>'operations' field (can be empty)</li>
     * </ul>
     * <p>
     * All checks are non-blocking and only log warnings.
     *
     * @param document the AsyncAPI document to validate
     */
    private void validateAsyncApiStructure(Map<String, Object> document) {
        // Check 'asyncapi' field
        if (!document.containsKey("asyncapi")) {
            log.warn("⚠️  Missing 'asyncapi' field in ourowebsocket.yml");
            log.warn("    Expected: asyncapi: 3.0.0");
        } else {
            Object versionObj = document.get("asyncapi");
            if (versionObj instanceof String) {
                String version = (String) versionObj;
                if (!version.startsWith("3.")) {
                    log.warn("⚠️  Unsupported AsyncAPI version: {}", version);
                    log.warn("    Expected: 3.x.x");
                }
            }
        }

        // Check 'info' field
        if (!document.containsKey("info")) {
            log.warn("⚠️  Missing 'info' field in ourowebsocket.yml");
        }

        // Check 'channels' field
        if (!document.containsKey("channels")) {
            log.warn("⚠️  Missing 'channels' field in ourowebsocket.yml");
        }

        // Check 'operations' field
        if (!document.containsKey("operations")) {
            log.warn("⚠️  Missing 'operations' field in ourowebsocket.yml");
        }
    }

    /**
     * Fixes malformed fields in the AsyncAPI document.
     * <p>
     * Corrects common issues:
     * <ul>
     *   <li>Replaces null components with empty object</li>
     *   <li>Replaces null channels with empty object</li>
     *   <li>Replaces null operations with empty object</li>
     *   <li>Ensures components.schemas exists</li>
     *   <li>Ensures components.messages exists</li>
     * </ul>
     *
     * @param document the AsyncAPI document to fix
     * @return true if any fields were fixed, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean fixMalformedFields(Map<String, Object> document) {
        boolean fixed = false;

        // Fix components: null → components: {schemas: {}, messages: {}}
        if (document.containsKey("components") && document.get("components") == null) {
            Map<String, Object> components = new LinkedHashMap<>();
            components.put("schemas", new LinkedHashMap<>());
            components.put("messages", new LinkedHashMap<>());
            document.put("components", components);
            log.info("🔧 Fixed: components: null → components: {{schemas: {{}}, messages: {{}}}}");
            fixed = true;
        } else if (document.containsKey("components")) {
            Map<String, Object> components = (Map<String, Object>) document.get("components");
            if (components != null) {
                if (!components.containsKey("schemas")) {
                    components.put("schemas", new LinkedHashMap<>());
                    log.info("🔧 Fixed: Added missing components.schemas");
                    fixed = true;
                }
                if (!components.containsKey("messages")) {
                    components.put("messages", new LinkedHashMap<>());
                    log.info("🔧 Fixed: Added missing components.messages");
                    fixed = true;
                }
            }
        }

        // Fix channels: null → channels: {}
        if (document.containsKey("channels") && document.get("channels") == null) {
            document.put("channels", new LinkedHashMap<>());
            log.info("🔧 Fixed: channels: null → channels: {{}}");
            fixed = true;
        }

        // Fix operations: null → operations: {}
        if (document.containsKey("operations") && document.get("operations") == null) {
            document.put("operations", new LinkedHashMap<>());
            log.info("🔧 Fixed: operations: null → operations: {{}}");
            fixed = true;
        }

        // Fix servers: null → servers: {}
        if (document.containsKey("servers") && document.get("servers") == null) {
            document.put("servers", new LinkedHashMap<>());
            log.info("🔧 Fixed: servers: null → servers: {{}}");
            fixed = true;
        }

        if (fixed) {
            log.info("ℹ️  Malformed fields have been corrected");
        }

        return fixed;
    }

    /**
     * Enriches all channels in the document with missing x-ouroboros-* fields.
     * <p>
     * Adds:
     * <ul>
     *   <li>x-ouroboros-id (UUID)</li>
     *   <li>x-ouroboros-progress ("mock")</li>
     *   <li>x-ouroboros-tag ("none")</li>
     *   <li>x-ouroboros-diff ("none")</li>
     *   <li>x-ouroboros-isvalid (true)</li>
     * </ul>
     * <p>
     * Only adds fields that don't already exist (non-destructive).
     *
     * @param document the AsyncAPI document
     * @return the number of channels that were enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichChannels(Map<String, Object> document) {
        int enrichedCount = 0;
        Map<String, Object> channels = (Map<String, Object>) document.get("channels");

        if (channels == null || channels.isEmpty()) {
            log.info("ℹ️  No channels to enrich");
            return 0;
        }

        for (Map.Entry<String, Object> channelEntry : channels.entrySet()) {
            String channelName = channelEntry.getKey();
            if (!(channelEntry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> channel = (Map<String, Object>) channelEntry.getValue();

            if (enrichChannelFields(channel)) {
                enrichedCount++;
                log.debug("🔧 Enriched channel: {}", channelName);
            }
        }

        return enrichedCount;
    }

    /**
     * Enriches a single channel with missing x-ouroboros-* fields.
     * <p>
     * Adds the following fields if they don't exist:
     * <ul>
     *   <li>x-ouroboros-id: UUID.randomUUID()</li>
     *   <li>x-ouroboros-progress: "mock"</li>
     *   <li>x-ouroboros-tag: "none"</li>
     *   <li>x-ouroboros-diff: "none"</li>
     *   <li>x-ouroboros-isvalid: true</li>
     * </ul>
     *
     * @param channel the channel map to enrich
     * @return true if any field was added, false if all fields already existed
     */
    private boolean enrichChannelFields(Map<String, Object> channel) {
        boolean modified = false;

        if (!channel.containsKey("x-ouroboros-id")) {
            channel.put("x-ouroboros-id", UUID.randomUUID().toString());
            modified = true;
        }

        if (!channel.containsKey("x-ouroboros-progress")) {
            channel.put("x-ouroboros-progress", "mock");
            modified = true;
        }

        if (!channel.containsKey("x-ouroboros-tag")) {
            channel.put("x-ouroboros-tag", "none");
            modified = true;
        }

        if (!channel.containsKey("x-ouroboros-diff")) {
            channel.put("x-ouroboros-diff", "none");
            modified = true;
        }

        if (!channel.containsKey("x-ouroboros-isvalid")) {
            channel.put("x-ouroboros-isvalid", true);
            modified = true;
        }

        return modified;
    }

    /**
     * Enriches all operations in the document with missing x-ouroboros-* fields.
     * <p>
     * Validates action types (send/receive) and adds:
     * <ul>
     *   <li>x-ouroboros-id (UUID)</li>
     *   <li>x-ouroboros-progress ("mock")</li>
     *   <li>x-ouroboros-tag ("none")</li>
     * </ul>
     * <p>
     * Only adds fields that don't already exist (non-destructive).
     *
     * @param document the AsyncAPI document
     * @return the number of operations that were enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichOperations(Map<String, Object> document) {
        int enrichedCount = 0;
        Map<String, Object> operations = (Map<String, Object>) document.get("operations");

        if (operations == null || operations.isEmpty()) {
            log.info("ℹ️  No operations to enrich");
            return 0;
        }

        for (Map.Entry<String, Object> operationEntry : operations.entrySet()) {
            String operationName = operationEntry.getKey();
            if (!(operationEntry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> operation = (Map<String, Object>) operationEntry.getValue();

            // Validate action type
            if (operation.containsKey("action")) {
                Object actionObj = operation.get("action");
                if (actionObj instanceof String) {
                    String action = (String) actionObj;
                    if (!VALID_ACTIONS.contains(action.toLowerCase())) {
                        log.warn("⚠️  Invalid action '{}' in operation '{}'. Valid actions: {}",
                                action, operationName, VALID_ACTIONS);
                    }
                }
            }

            if (enrichOperationFields(operation)) {
                enrichedCount++;
                log.debug("🔧 Enriched operation: {}", operationName);
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

        return modified;
    }

    /**
     * Enriches all message payloads in the document with missing x-ouroboros-* fields.
     * <p>
     * Processes messages in components/messages section and adds:
     * <ul>
     *   <li>x-ouroboros-mock: "" to each property</li>
     *   <li>x-ouroboros-orders: array of property keys</li>
     * </ul>
     *
     * @param document the AsyncAPI document
     * @return the number of messages that were enriched
     */
    @SuppressWarnings("unchecked")
    private int enrichMessages(Map<String, Object> document) {
        int enrichedCount = 0;

        Map<String, Object> components = (Map<String, Object>) document.get("components");
        if (components != null) {
            Map<String, Object> messages = (Map<String, Object>) components.get("messages");
            if (messages != null) {
                for (Map.Entry<String, Object> messageEntry : messages.entrySet()) {
                    String messageName = messageEntry.getKey();
                    if (!(messageEntry.getValue() instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> message = (Map<String, Object>) messageEntry.getValue();

                    // Get payload from message
                    Object payloadObj = message.get("payload");
                    if (payloadObj instanceof Map) {
                        Map<String, Object> payload = (Map<String, Object>) payloadObj;
                        if (enrichPayloadFields(payload)) {
                            enrichedCount++;
                            log.debug("🔧 Enriched message payload: {}", messageName);
                        }
                    }
                }
            }
        }

        return enrichedCount;
    }

    /**
     * Enriches a message payload with missing x-ouroboros-* fields.
     * <p>
     * Adds:
     * <ul>
     *   <li>x-ouroboros-mock: "" to each property (if payload has properties)</li>
     *   <li>x-ouroboros-orders: array of property keys (if payload has properties)</li>
     * </ul>
     * <p>
     * Only processes payloads that have a 'properties' field. Skips $ref payloads.
     *
     * @param payload the payload map to enrich
     * @return true if any field was added or corrected, false if all fields already existed
     */
    @SuppressWarnings("unchecked")
    private boolean enrichPayloadFields(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("properties")) {
            return false;
        }

        boolean modified = false;

        Object propertiesObj = payload.get("properties");
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
        if (!payload.containsKey("x-ouroboros-orders")) {
            payload.put("x-ouroboros-orders", new ArrayList<>(properties.keySet()));
            modified = true;
        }

        return modified;
    }
}