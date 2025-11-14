package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.websocket.common.yaml.WebSocketYamlParser;
import kr.co.ouroboros.core.websocket.spec.model.Property;
import kr.co.ouroboros.core.websocket.spec.util.ReferenceConverter;
import kr.co.ouroboros.ui.websocket.spec.dto.CreateSchemaRequest;
import kr.co.ouroboros.ui.websocket.spec.dto.SchemaResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.UpdateSchemaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link WebsocketSchemaService}.
 * <p>
 * Manages schema definitions in the AsyncAPI components/schemas section of ourowebsocket.yml.
 * Uses {@link WebSocketYamlParser} for all YAML file operations.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSchemaServiceImpl implements WebsocketSchemaService {

    private final WebSocketYamlParser yamlParser;
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new schema in the AsyncAPI document, updates the processed spec cache.
     *
     * @param request contains the schema name and definition fields used to build and insert the new schema
     * @return the SchemaResponse representing the created schema
     */
    @Override
    public SchemaResponse createSchema(CreateSchemaRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            // Read existing document or create new one (from file, not cache)
            Map<String, Object> asyncApiDoc = yamlParser.readOrCreateDocument();

            // Check for duplicate schema name
            if (yamlParser.schemaExists(asyncApiDoc, request.getSchemaName())) {
                throw new IllegalArgumentException("Schema '" + request.getSchemaName() + "' already exists");
            }

            // Build schema definition
            Map<String, Object> schemaDefinition = buildSchemaDefinition(request);

            // Add schema to document
            yamlParser.putSchema(asyncApiDoc, request.getSchemaName(), schemaDefinition);

            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

            log.info("Created WebSocket schema: {}", request.getSchemaName());

            return convertToResponse(request.getSchemaName(), schemaDefinition);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<SchemaResponse> getAllSchemas() throws Exception {
        lock.readLock().lock();
        try {
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                return new ArrayList<>();
            }

            Map<String, Object> schemas = yamlParser.getSchemas(asyncApiDoc);

            if (schemas == null || schemas.isEmpty()) {
                return new ArrayList<>();
            }

            List<SchemaResponse> responses = new ArrayList<>();
            for (Map.Entry<String, Object> entry : schemas.entrySet()) {
                String schemaName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> schemaDefinition = (Map<String, Object>) entry.getValue();
                responses.add(convertToResponse(schemaName, schemaDefinition));
            }

            return responses;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SchemaResponse getSchema(String schemaName) throws Exception {
        lock.readLock().lock();
        try {
            // Read from cache
            Map<String, Object> asyncApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.WEB_SOCKET));
            if (asyncApiDoc == null) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            Map<String, Object> schemaDefinition = yamlParser.getSchema(asyncApiDoc, schemaName);

            if (schemaDefinition == null) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            return convertToResponse(schemaName, schemaDefinition);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Update an existing AsyncAPI schema using only the non-null fields from the request.
     *
     * @param schemaName the name of the schema to update
     * @param request container of fields to apply; only fields that are non-null on the request are updated
     * @return a SchemaResponse representing the updated schema
     * @throws IllegalArgumentException if the specification file does not exist or the named schema is not found
     * @throws Exception if processing, validation, or caching of the updated specification fails
     */
    @Override
    public SchemaResponse updateSchema(String schemaName, UpdateSchemaRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();
            Map<String, Object> existingSchema = yamlParser.getSchema(asyncApiDoc, schemaName);

            if (existingSchema == null) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            // Handle schema name change (rename) - automatically rename based on title change
            String finalSchemaName = schemaName;
            String newSchemaName = null;
            
            // Check if title changed and use title as new schema name
            if (request.getTitle() != null && !request.getTitle().isBlank()) {
                String currentTitle = safeGetString(existingSchema, "title");
                String newTitle = request.getTitle();
                
                // If title changed, convert title to valid schema name
                if (!newTitle.equals(currentTitle)) {
                    // Convert title to valid schema name (remove spaces, special chars, use PascalCase)
                    newSchemaName = convertTitleToSchemaName(newTitle);
                    
                    // If converted name is same as current name, don't rename
                    if (newSchemaName.equals(schemaName)) {
                        newSchemaName = null;
                    }
                }
            }
            
            // Perform rename if newSchemaName is determined
            if (newSchemaName != null && !newSchemaName.equals(schemaName)) {
                // Check if new name already exists
                if (yamlParser.schemaExists(asyncApiDoc, newSchemaName)) {
                    throw new IllegalArgumentException("Schema '" + newSchemaName + "' already exists");
                }
                
                // Create rename map for reference updates
                Map<String, String> schemaRenameMap = new HashMap<>();
                schemaRenameMap.put(schemaName, newSchemaName);
                
                // Update all schema references in the document
                updateAllSchemaReferences(asyncApiDoc, schemaRenameMap);
                
                // Create new schema with updated content (will be updated below)
                // Copy existing schema first
                @SuppressWarnings("unchecked")
                Map<String, Object> newSchema = new LinkedHashMap<>((Map<String, Object>) existingSchema);
                
                // Remove old schema
                yamlParser.removeSchema(asyncApiDoc, schemaName);
                
                // Add new schema (will be updated with request fields below)
                yamlParser.putSchema(asyncApiDoc, newSchemaName, newSchema);
                
                // Update finalSchemaName for response
                finalSchemaName = newSchemaName;
                existingSchema = newSchema;
                
                log.info("Renaming schema '{}' to '{}'", schemaName, newSchemaName);
            }

            // Get current type and new type
            String currentType = safeGetString(existingSchema, "type");
            String newType = request.getType();
            
            // Update type if provided
            if (newType != null) {
                existingSchema.put("type", newType);
                
                // If type changed, clean up incompatible fields
                if (!newType.equals(currentType)) {
                    if ("object".equals(newType)) {
                        // Switching to object: remove array-specific fields
                        existingSchema.remove("items");
                    } else if ("array".equals(newType)) {
                        // Switching to array: remove object-specific fields
                        existingSchema.remove("properties");
                        existingSchema.remove("required");
                        existingSchema.remove("x-ouroboros-orders");
                    }
                }
            }
            
            // Use the final type (new type if provided, otherwise current type)
            String finalType = newType != null ? newType : (currentType != null ? currentType : "object");
            
            if (request.getTitle() != null) {
                existingSchema.put("title", request.getTitle());
            }
            if (request.getDescription() != null) {
                existingSchema.put("description", request.getDescription());
            }
            
            // Update fields based on final type
            if ("object".equals(finalType)) {
                // For object type: remove array-specific fields and update object-specific fields
                existingSchema.remove("items");

                if (request.getProperties() != null) {
                    existingSchema.put("properties", buildProperties(request.getProperties()));
                }
                if (request.getRequired() != null) {
                    existingSchema.put("required", request.getRequired());
                }
                if (request.getOrders() != null) {
                    existingSchema.put("x-ouroboros-orders", request.getOrders());
                }
            } else if ("array".equals(finalType)) {
                // For array type: remove object-specific fields and update items only
                existingSchema.remove("properties");
                existingSchema.remove("required");
                existingSchema.remove("x-ouroboros-orders");

                if (request.getItems() != null) {
                    if (request.getItems() instanceof Property) {
                        existingSchema.put("items", buildProperty((Property) request.getItems()));
                    } else if (request.getItems() instanceof Map) {
                        // JSON deserialization creates Map instead of Property
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemsMap = (Map<String, Object>) request.getItems();
                        existingSchema.put("items", itemsMap);
                    } else {
                        log.warn("Items field is not a Property or Map instance, skipping");
                    }
                }
            } else {
                // For primitive types (string, number, integer, boolean, etc.):
                // remove all object/array-specific fields to prevent stale data
                existingSchema.remove("properties");
                existingSchema.remove("required");
                existingSchema.remove("x-ouroboros-orders");
                existingSchema.remove("items");
            }

            // Write to file directly (cache update will be done later when handler is implemented)
            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

            log.info("Updated WebSocket schema: {}", finalSchemaName);

            return convertToResponse(finalSchemaName, existingSchema);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a schema from the WebSocket AsyncAPI document and updates the processed spec cache.
     *
     * @param schemaName the name of the schema to remove
     * @throws IllegalArgumentException if the specification file does not exist or the named schema is not found
     * @throws Exception if processing or caching fails
     */
    @Override
    public void deleteSchema(String schemaName) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> asyncApiDoc = yamlParser.readDocumentFromFile();

            boolean removed = yamlParser.removeSchema(asyncApiDoc, schemaName);

            if (!removed) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            // Write to file directly (cache update will be done later when handler is implemented)
            // Write document directly
            yamlParser.writeDocument(asyncApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.WEB_SOCKET, asyncApiDoc);

            log.info("Deleted WebSocket schema: {}", schemaName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Helper methods for building schema structures

    private Map<String, Object> buildSchemaDefinition(CreateSchemaRequest request) {
        Map<String, Object> schema = new LinkedHashMap<>();

        String schemaType = request.getType() != null ? request.getType() : "object";
        schema.put("type", schemaType);

        if (request.getTitle() != null) {
            schema.put("title", request.getTitle());
        }

        if (request.getDescription() != null) {
            schema.put("description", request.getDescription());
        }

        // For object type: set properties, required, orders
        if ("object".equals(schemaType)) {
            if (request.getProperties() != null && !request.getProperties().isEmpty()) {
                schema.put("properties", buildProperties(request.getProperties()));
            }

            if (request.getRequired() != null && !request.getRequired().isEmpty()) {
                schema.put("required", request.getRequired());
            }

            if (request.getOrders() != null && !request.getOrders().isEmpty()) {
                schema.put("x-ouroboros-orders", request.getOrders());
            }
        }
        // For array type: set items only
        else if ("array".equals(schemaType)) {
            if (request.getItems() != null) {
                if (request.getItems() instanceof Property) {
                    schema.put("items", buildProperty((Property) request.getItems()));
                } else if (request.getItems() instanceof Map) {
                    // JSON deserialization creates Map instead of Property
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemsMap = (Map<String, Object>) request.getItems();
                    schema.put("items", itemsMap);
                } else {
                    log.warn("Items field is not a Property or Map instance, skipping");
                }
            }
        }

        return schema;
    }

    private Map<String, Object> buildProperties(Map<String, Property> properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            result.put(entry.getKey(), buildProperty(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> buildProperty(Property property) {
        Map<String, Object> propertyMap = new LinkedHashMap<>();

        // Handle schema reference
        if (property.getRef() != null && !property.getRef().isBlank()) {
            // Convert to full $ref format for YAML
            String fullRef = property.getRef().startsWith("#/components/schemas/")
                    ? property.getRef()
                    : "#/components/schemas/" + property.getRef();
            propertyMap.put("$ref", fullRef);
            return propertyMap; // Reference mode: ignore other fields
        }

        // Inline mode: build property definition
        propertyMap.put("type", property.getType() != null ? property.getType() : "string");

        if (property.getDescription() != null) {
            propertyMap.put("description", property.getDescription());
        }

        // For object types - nested properties (recursive)
        if (property.getProperties() != null && !property.getProperties().isEmpty()) {
            propertyMap.put("properties", buildProperties(property.getProperties()));
        }

        if (property.getRequired() != null && !property.getRequired().isEmpty()) {
            propertyMap.put("required", property.getRequired());
        }

        // For array types - items (recursive)
        if (property.getItems() != null) {
            propertyMap.put("items", buildProperty(property.getItems()));
        }
        if (property.getMinItems() != null) {
            propertyMap.put("minItems", property.getMinItems());
        }
        if (property.getMaxItems() != null) {
            propertyMap.put("maxItems", property.getMaxItems());
        }

        // Format
        if (property.getFormat() != null) {
            propertyMap.put("format", property.getFormat());
        }

        return propertyMap;
    }

    private SchemaResponse convertToResponse(String schemaName, Map<String, Object> schemaDefinition) {
        SchemaResponse.SchemaResponseBuilder builder = SchemaResponse.builder()
                .schemaName(schemaName)
                .type(safeGetString(schemaDefinition, "type"))
                .title(safeGetString(schemaDefinition, "title"))
                .description(safeGetString(schemaDefinition, "description"))
                .required(safeGetStringList(schemaDefinition, "required"))
                .orders(safeGetStringList(schemaDefinition, "x-ouroboros-orders"));

        // Extract properties
        Object propertiesObj = schemaDefinition.get("properties");
        if (propertiesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> propertiesMap = (Map<String, Object>) propertiesObj;
            Map<String, Property> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                    properties.put(entry.getKey(), convertToProperty(propDef));
                } else {
                    log.warn("Invalid property definition for '{}': expected Map but got {}",
                            entry.getKey(), entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null");
                }
            }
            builder.properties(properties);
        }

        // Extract items for array type
        Object itemsObj = schemaDefinition.get("items");
        if (itemsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) itemsObj;
            builder.items(convertToProperty(itemsMap));
        }

        return builder.build();
    }

    private Property convertToProperty(Map<String, Object> propertyDefinition) {
        Property.PropertyBuilder builder = Property.builder();

        // Check for schema reference in YAML ($ref) or JSON (ref)
        String dollarRef = safeGetString(propertyDefinition, "$ref");
        String ref = safeGetString(propertyDefinition, "ref");
        String refValue = dollarRef != null ? dollarRef : ref;
        
        if (refValue != null) {
            // Convert $ref or ref to simplified ref for client
            if (refValue.startsWith("#/components/schemas/")) {
                String simplifiedRef = refValue.substring("#/components/schemas/".length());
                builder.ref(simplifiedRef);
            } else {
                builder.ref(refValue);
            }
            return builder.build(); // Reference mode: return early
        }

        // Inline mode: extract all property fields
        builder.type(safeGetString(propertyDefinition, "type"))
                .description(safeGetString(propertyDefinition, "description"))
                .minItems(safeGetInteger(propertyDefinition, "minItems"))
                .maxItems(safeGetInteger(propertyDefinition, "maxItems"));

        // For object types - nested properties (recursive)
        Object propertiesObj = propertyDefinition.get("properties");
        if (propertiesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> propertiesMap = (Map<String, Object>) propertiesObj;
            Map<String, Property> nestedProperties = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                    nestedProperties.put(entry.getKey(), convertToProperty(propDef));
                }
            }
            builder.properties(nestedProperties);
        }

        // Required fields for object types
        Object requiredObj = propertyDefinition.get("required");
        if (requiredObj instanceof List) {
            builder.required(safeGetStringList(propertyDefinition, "required"));
        }

        // Handle nested items for array type (recursive)
        Object itemsObj = propertyDefinition.get("items");
        if (itemsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> items = (Map<String, Object>) itemsObj;
            builder.items(convertToProperty(items));
        }

        // Format
        builder.format(safeGetString(propertyDefinition, "format"));

        return builder.build();
    }

    /**
     * Safely extracts a String value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the String value, or null if not found or not a String
     */
    private String safeGetString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            log.warn("Expected String for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Safely extracts an Integer value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the Integer value, or null if not found or not an Integer
     */
    private Integer safeGetInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            log.warn("Expected Integer for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Retrieve a List of Strings stored under the given key, validating that every element is a String.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the list of strings if present and all elements are strings; `null` if the key is absent, the value is not a list, or any element is not a string
     */
    @SuppressWarnings("unchecked")
    private List<String> safeGetStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            try {
                List<?> list = (List<?>) value;
                // Validate all elements are Strings
                for (Object item : list) {
                    if (!(item instanceof String)) {
                        log.warn("List for key '{}' contains non-String element: {}",
                                key, item != null ? item.getClass().getSimpleName() : "null");
                        return null;
                    }
                }
                return (List<String>) value;
            } catch (ClassCastException e) {
                log.warn("Failed to cast list for key '{}': {}", key, e.getMessage());
                return null;
            }
        }
        if (value != null) {
            log.warn("Expected List for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Converts a title string to a valid schema name.
     * <p>
     * Removes spaces and special characters, converts to PascalCase.
     * Supports Unicode characters (including non-Latin scripts like Korean, Japanese, Chinese).
     * Examples:
     * <ul>
     *   <li>"Message Item" -> "MessageItem"</li>
     *   <li>"chat message" -> "ChatMessage"</li>
     *   <li>"User-Profile" -> "UserProfile"</li>
     *   <li>"사용자 메시지" -> "사용자메시지"</li>
     *   <li>"ユーザー情報" -> "ユーザー情報"</li>
     * </ul>
     *
     * @param title the title to convert
     * @return valid schema name in PascalCase
     */
    private String convertTitleToSchemaName(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }

        // Remove leading/trailing whitespace
        String trimmed = title.trim();

        // Split by spaces, hyphens, underscores, and other non-word characters
        // \p{L} matches any Unicode letter, \p{N} matches any Unicode number
        String[] words = trimmed.split("[\\s\\-_]+");

        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            // Remove non-letter/non-digit characters (supports Unicode)
            // \p{L} = any Unicode letter, \p{N} = any Unicode digit
            String cleaned = word.replaceAll("[^\\p{L}\\p{N}]", "");
            if (cleaned.isEmpty()) {
                continue;
            }

            // Convert to PascalCase: first letter uppercase, rest lowercase (locale-aware)
            if (cleaned.length() == 1) {
                result.append(cleaned.toUpperCase(Locale.ROOT));
            } else {
                // Use codePointAt for proper Unicode handling
                int firstCodePoint = cleaned.codePointAt(0);
                int upperCaseCodePoint = Character.toUpperCase(firstCodePoint);
                result.append(Character.toChars(upperCaseCodePoint))
                      .append(cleaned.substring(Character.charCount(firstCodePoint))
                              .toLowerCase(Locale.ROOT));
            }
        }

        // If result is empty after processing, use original title (sanitized)
        if (result.length() == 0) {
            String sanitized = trimmed.replaceAll("[^\\p{L}\\p{N}]", "");
            if (sanitized.isEmpty()) {
                return "Schema"; // Fallback
            }

            if (sanitized.length() == 1) {
                return sanitized.toUpperCase(Locale.ROOT);
            }

            int firstCodePoint = sanitized.codePointAt(0);
            int upperCaseCodePoint = Character.toUpperCase(firstCodePoint);
            return new String(Character.toChars(upperCaseCodePoint)) +
                   sanitized.substring(Character.charCount(firstCodePoint)).toLowerCase(Locale.ROOT);
        }

        return result.toString();
    }

    /**
     * Updates all schema references in the AsyncAPI document when a schema is renamed.
     * <p>
     * Updates references in:
     * <ul>
     *   <li>All schemas (nested schema references)</li>
     *   <li>All messages (message payload schema references)</li>
     * </ul>
     *
     * @param asyncApiDoc the AsyncAPI document
     * @param schemaRenameMap map of old schema names to new names
     */
    @SuppressWarnings("unchecked")
    private void updateAllSchemaReferences(Map<String, Object> asyncApiDoc, Map<String, String> schemaRenameMap) {
        if (schemaRenameMap.isEmpty()) {
            return;
        }

        // Update references in all schemas (for nested schema references)
        Map<String, Object> schemas = yamlParser.getSchemas(asyncApiDoc);
        if (schemas != null && !schemas.isEmpty()) {
            for (Map.Entry<String, Object> entry : schemas.entrySet()) {
                Object schemaObj = entry.getValue();
                if (schemaObj instanceof Map) {
                    updateSchemaReferences(schemaObj, schemaRenameMap);
                }
            }
        }

        // Update references in all messages (for message payload schema references)
        Map<String, Object> components = yamlParser.getOrCreateComponents(asyncApiDoc);
        if (components != null) {
            Map<String, Object> messages = yamlParser.getMessages(asyncApiDoc);
            if (messages != null && !messages.isEmpty()) {
                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    Object messageObj = entry.getValue();
                    if (messageObj instanceof Map) {
                        updateSchemaReferences(messageObj, schemaRenameMap);
                    }
                }
            }
        }
    }

    /**
     * Recursively updates all $ref references according to schema rename map.
     * Delegates to ReferenceConverter for consistency.
     *
     * @param obj the object to scan for $ref (can be Map, List, or primitive)
     * @param schemaRenameMap map of old schema names to new names
     */
    private void updateSchemaReferences(Object obj, Map<String, String> schemaRenameMap) {
        ReferenceConverter.updateSchemaReferences(obj, schemaRenameMap);
    }
}
