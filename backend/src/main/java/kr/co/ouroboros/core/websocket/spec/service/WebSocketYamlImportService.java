package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.ui.websocket.spec.dto.ImportYamlResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.RenamedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.*;

/**
 * Service for importing AsyncAPI YAML files.
 * <p>
 * Handles parsing, merging, duplicate handling, and enrichment of imported AsyncAPI documents.
 * This is a component used by WebSocketOperationServiceImpl, not a standalone service.
 *
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketYamlImportService {

    private final WebSocketYamlParser yamlParser;
    private final WebSocketReferenceUpdater referenceUpdater;

    /**
     * Imports external AsyncAPI 3.0.0 YAML file and merges into ourowebsocket.yml.
     * <p>
     * Validates the uploaded YAML file, handles duplicate channels/operations/schemas/messages by auto-renaming,
     * enriches with Ouroboros custom fields, and updates $ref references accordingly.
     *
     * @param yamlContent the AsyncAPI YAML content to import
     * @return import result with counts and renamed items
     * @throws Exception if validation fails or import operation fails
     */
    public ImportYamlResponse importYaml(String yamlContent) throws Exception {
        log.info("========================================");
        log.info("📥 Starting AsyncAPI YAML import...");

        // Step 1: Parse imported YAML (validation already done in controller)
        // Use SafeConstructor to prevent arbitrary object deserialization
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        @SuppressWarnings("unchecked")
        Map<String, Object> importedDoc = (Map<String, Object>) yaml.load(yamlContent);

        // Step 2: Read existing document or create new one (from file, not cache)
        Map<String, Object> existingDoc = yamlParser.readOrCreateDocument();

        // Step 3: Prepare renamed tracking
        List<RenamedItem> renamedList = new ArrayList<>();
        Map<String, String> schemaRenameMap = new HashMap<>();  // old name -> new name
        Map<String, String> messageRenameMap = new HashMap<>(); // old name -> new name

        // Step 4: Process schemas first
        int importedSchemas = importSchemas(importedDoc, existingDoc, renamedList, schemaRenameMap);

        // Step 5: Process messages (with schema reference updates)
        int importedMessages = importMessages(importedDoc, existingDoc, renamedList, messageRenameMap, schemaRenameMap);

        // Step 5.5: Process servers
        int importedServers = importServers(importedDoc, existingDoc, renamedList);

        // Step 5.6: Extract entrypoint from imported servers for operations
        String entrypoint = extractFirstServerPathname(importedDoc);

        // Step 6: Process channels (with message reference updates)
        int importedChannels = importChannels(importedDoc, existingDoc, renamedList, messageRenameMap);

        // Step 7: Process operations (with message reference updates)
        int importedOperations = importOperations(importedDoc, existingDoc, renamedList, messageRenameMap, entrypoint);

        // Step 7.5: Update schema references in messages (after all messages are imported)
        referenceUpdater.updateSchemaReferencesInMessages(existingDoc, schemaRenameMap);

        // Step 8: Save to file
        yamlParser.writeDocument(existingDoc);

        // Step 9: Build response
        String summary = String.format("Successfully imported %d channels, %d operations, %d schemas, %d messages%s",
                importedChannels, importedOperations, importedSchemas, importedMessages,
                !renamedList.isEmpty() ? ", renamed " + renamedList.size() + " items due to duplicates" : "");

        log.info("========================================");
        log.info("✅ AsyncAPI YAML Import Completed");
        log.info("   📊 Servers imported: {}", importedServers);
        log.info("   📊 Channels imported: {}", importedChannels);
        log.info("   📊 Operations imported: {}", importedOperations);
        log.info("   📊 Schemas imported: {}", importedSchemas);
        log.info("   📊 Messages imported: {}", importedMessages);
        log.info("   📊 Items renamed: {}", renamedList.size());
        log.info("========================================");

        return ImportYamlResponse.builder()
                .importedChannels(importedChannels)
                .importedOperations(importedOperations)
                .renamed(renamedList.size())
                .summary(summary)
                .renamedList(renamedList)
                .build();
    }

    /**
     * Imports schemas from imported document into existing document.
     * <p>
     * Handles duplicate schema names by auto-renaming with "-import" suffix.
     *
     * @param importedDoc imported AsyncAPI document
     * @param existingDoc existing document to merge into
     * @param renamedList list to track renamed items
     * @param schemaRenameMap map to store schema rename mappings
     * @return number of schemas imported
     */
    @SuppressWarnings("unchecked")
    private int importSchemas(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                              List<RenamedItem> renamedList, Map<String, String> schemaRenameMap) {
        int count = 0;

        Map<String, Object> existingComponents = yamlParser.getOrCreateComponents(existingDoc);
        Map<String, Object> existingSchemas = yamlParser.getOrCreateSchemas(existingComponents);

        Map<String, Object> importedComponents = (Map<String, Object>) importedDoc.get("components");
        if (importedComponents == null) {
            return 0;
        }

        Map<String, Object> importedSchemas = (Map<String, Object>) importedComponents.get("schemas");
        if (importedSchemas == null || importedSchemas.isEmpty()) {
            return 0;
        }

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

            existingSchemas.put(finalName, entry.getValue());
            count++;
        }

        return count;
    }

    /**
     * Imports messages from imported document into existing document.
     * <p>
     * Handles duplicate message names by auto-renaming with "-import" suffix.
     * Updates schema references in message payloads according to schema rename map.
     *
     * @param importedDoc imported AsyncAPI document
     * @param existingDoc existing document to merge into
     * @param renamedList list to track renamed items
     * @param messageRenameMap map to store message rename mappings
     * @param schemaRenameMap map of schema rename mappings for reference updates
     * @return number of messages imported
     */
    @SuppressWarnings("unchecked")
    private int importMessages(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                               List<RenamedItem> renamedList, Map<String, String> messageRenameMap,
                               Map<String, String> schemaRenameMap) {
        int count = 0;

        Map<String, Object> existingComponents = yamlParser.getOrCreateComponents(existingDoc);
        Map<String, Object> existingMessages = yamlParser.getOrCreateMessages(existingComponents);

        Map<String, Object> importedComponents = (Map<String, Object>) importedDoc.get("components");
        if (importedComponents == null) {
            return 0;
        }

        Map<String, Object> importedMessages = (Map<String, Object>) importedComponents.get("messages");
        if (importedMessages == null || importedMessages.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedMessages.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingMessages.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingMessages.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("message")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                messageRenameMap.put(originalName, finalName);
                log.info("🔄 Message '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            // Update message references in the message itself (for payload schema refs)
            Map<String, Object> message = (Map<String, Object>) entry.getValue();
            referenceUpdater.updateSchemaReferencesInMessage(message, schemaRenameMap);

            existingMessages.put(finalName, message);
            count++;
        }

        return count;
    }

    /**
     * Imports channels from imported document into existing document.
     * <p>
     * Handles duplicate channel names by auto-renaming with "-import" suffix.
     * Updates message references according to message rename map.
     *
     * @param importedDoc imported AsyncAPI document
     * @param existingDoc existing document to merge into
     * @param renamedList list to track renamed items
     * @param messageRenameMap map of message rename mappings for reference updates
     * @return number of channels imported
     */
    @SuppressWarnings("unchecked")
    private int importChannels(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                               List<RenamedItem> renamedList, Map<String, String> messageRenameMap) {
        int count = 0;

        Map<String, Object> existingChannels = yamlParser.getOrCreateChannels(existingDoc);

        Map<String, Object> importedChannels = (Map<String, Object>) importedDoc.get("channels");
        if (importedChannels == null || importedChannels.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedChannels.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingChannels.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingChannels.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("channel")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                log.info("🔄 Channel '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            // Enrich channel with Ouroboros fields
            Map<String, Object> channel = (Map<String, Object>) entry.getValue();
            enrichChannelWithOuroborosFields(channel);

            // Update message references in channel messages
            referenceUpdater.updateMessageReferencesInChannel(channel, messageRenameMap);

            existingChannels.put(finalName, channel);
            count++;
        }

        return count;
    }

    /**
     * Imports servers from imported document into existing document.
     * <p>
     * Handles duplicate server names by auto-renaming with "-import" suffix.
     *
     * @param importedDoc imported AsyncAPI document
     * @param existingDoc existing document to merge into
     * @param renamedList list to track renamed items
     * @return number of servers imported
     */
    @SuppressWarnings("unchecked")
    private int importServers(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                              List<RenamedItem> renamedList) {
        int count = 0;

        // Get or create servers section
        Map<String, Object> existingServers = (Map<String, Object>) existingDoc.get("servers");
        if (existingServers == null) {
            existingServers = new LinkedHashMap<>();
            existingDoc.put("servers", existingServers);
        }

        Map<String, Object> importedServers = (Map<String, Object>) importedDoc.get("servers");
        if (importedServers == null || importedServers.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedServers.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingServers.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingServers.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                renamedList.add(RenamedItem.builder()
                        .type("server")
                        .original(originalName)
                        .renamed(finalName)
                        .build());

                log.info("🔄 Server '{}' renamed to '{}' due to duplicate", originalName, finalName);
            }

            existingServers.put(finalName, entry.getValue());
            count++;
        }

        return count;
    }

    /**
     * Imports operations from imported document into existing document.
     * <p>
     * Handles duplicate operation names by auto-renaming with "-import" suffix.
     * Updates message references according to message rename map.
     *
     * @param importedDoc imported AsyncAPI document
     * @param existingDoc existing document to merge into
     * @param renamedList list to track renamed items
     * @param messageRenameMap map of message rename mappings for reference updates
     * @param entrypoint server pathname for x-ouroboros-entrypoint
     * @return number of operations imported
     */
    @SuppressWarnings("unchecked")
    private int importOperations(Map<String, Object> importedDoc, Map<String, Object> existingDoc,
                                 List<RenamedItem> renamedList, Map<String, String> messageRenameMap, String entrypoint) {
        int count = 0;

        Map<String, Object> existingOperations = yamlParser.getOrCreateOperations(existingDoc);

        Map<String, Object> importedOperations = (Map<String, Object>) importedDoc.get("operations");
        if (importedOperations == null || importedOperations.isEmpty()) {
            return 0;
        }

        for (Map.Entry<String, Object> entry : importedOperations.entrySet()) {
            String originalName = entry.getKey();
            String finalName = originalName;

            if (existingOperations.containsKey(originalName)) {
                finalName = originalName + "-import";
                int counter = 1;
                while (existingOperations.containsKey(finalName)) {
                    finalName = originalName + "-import" + counter;
                    counter++;
                }

                Map<String, Object> operation = (Map<String, Object>) entry.getValue();
                String action = (String) operation.get("action");

                renamedList.add(RenamedItem.builder()
                        .type("operation")
                        .original(originalName)
                        .renamed(finalName)
                        .action(action)
                        .build());

                log.info("🔄 Operation '{}' ({}) renamed to '{}' due to duplicate",
                        originalName, action, finalName);
            }

            // Enrich operation with Ouroboros fields
            Map<String, Object> operation = (Map<String, Object>) entry.getValue();
            enrichOperationWithOuroborosFields(operation, entrypoint);

            // Update message references in operation
            referenceUpdater.updateMessageReferencesInOperation(operation, messageRenameMap);

            existingOperations.put(finalName, operation);
            count++;
        }

        return count;
    }

    /**
     * Extracts the pathname from the first server in the imported document.
     *
     * @param importedDoc the imported AsyncAPI document
     * @return the pathname of the first server, or null if not found
     */
    @SuppressWarnings("unchecked")
    private String extractFirstServerPathname(Map<String, Object> importedDoc) {
        Object serversObj = importedDoc.get("servers");
        if (!(serversObj instanceof Map)) {
            return null;
        }

        Map<String, Object> servers = (Map<String, Object>) serversObj;
        if (servers.isEmpty()) {
            return null;
        }

        // Get first server
        Map.Entry<String, Object> firstServerEntry = servers.entrySet().iterator().next();
        Object serverObj = firstServerEntry.getValue();
        if (!(serverObj instanceof Map)) {
            return null;
        }

        Map<String, Object> server = (Map<String, Object>) serverObj;
        Object pathnameObj = server.get("pathname");
        return pathnameObj instanceof String ? (String) pathnameObj : null;
    }

    /**
     * Enriches a channel with missing x-ouroboros-* fields.
     * <p>
     * For import operations, channels are not enriched with Ouroboros fields.
     *
     * @param channel channel definition to enrich
     */
    private void enrichChannelWithOuroborosFields(Map<String, Object> channel) {
        // No enrichment for imported channels
    }

    /**
     * Enriches an operation with missing x-ouroboros-* fields.
     * <p>
     * Adds x-ouroboros-id (UUID), x-ouroboros-progress ("none"),
     * x-ouroboros-diff ("none"), and x-ouroboros-entrypoint (server pathname).
     *
     * @param operation the operation to enrich
     * @param entrypoint the server pathname to use as entrypoint
     */
    private void enrichOperationWithOuroborosFields(Map<String, Object> operation, String entrypoint) {
        if (!operation.containsKey("x-ouroboros-id")) {
            operation.put("x-ouroboros-id", UUID.randomUUID().toString());
        }
        if (!operation.containsKey("x-ouroboros-progress")) {
            operation.put("x-ouroboros-progress", "none");
        }
        if (!operation.containsKey("x-ouroboros-diff")) {
            operation.put("x-ouroboros-diff", "none");
        }
        if (!operation.containsKey("x-ouroboros-entrypoint") && entrypoint != null) {
            operation.put("x-ouroboros-entrypoint", entrypoint);
        }
    }
}

