package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
import kr.co.ouroboros.core.rest.handler.helper.RequestDiffHelper;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import kr.co.ouroboros.core.rest.mock.service.RestMockLoaderService;
import kr.co.ouroboros.ui.rest.spec.dto.CreateSchemaRequest;
import kr.co.ouroboros.ui.rest.spec.dto.SchemaResponse;
import kr.co.ouroboros.ui.rest.spec.dto.UpdateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.model.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link RestSchemaService}.
 * <p>
 * Manages schema definitions in the OpenAPI components/schemas section of ourorest.yml.
 * Uses {@link RestApiYamlParser} for all YAML file operations.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestSchemaServiceImpl implements RestSchemaService {

    private final RestApiYamlParser yamlParser;
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final RestMockRegistry mockRegistry;
    private final RestMockLoaderService mockLoaderService;

    /**
     * Creates a new schema in the OpenAPI document, updates the processed spec cache, and reloads the mock registry.
     *
     * @param request contains the schema name and definition fields used to build and insert the new schema
     * @return the SchemaResponse representing the created schema
     */
    @Override
    public SchemaResponse createSchema(CreateSchemaRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            // Read existing document or create new one (from file, not cache)
            Map<String, Object> openApiDoc = yamlParser.readOrCreateDocument();

            // Check for duplicate schema name
            if (yamlParser.schemaExists(openApiDoc, request.getSchemaName())) {
                throw new IllegalArgumentException("Schema '" + request.getSchemaName() + "' already exists");
            }

            // Build schema definition
            Map<String, Object> schemaDefinition = buildSchemaDefinition(request);

            // Add schema to document
            yamlParser.putSchema(openApiDoc, request.getSchemaName(), schemaDefinition);

            // Write document directly
            yamlParser.writeDocument(openApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            // registry 초기화 후 재등록 (전체 읽기)
            reloadMockRegistry();

            log.info("Created schema: {}", request.getSchemaName());

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
            Map<String, Object> openApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.REST));
            if (openApiDoc == null) {
                return new ArrayList<>();
            }

            Map<String, Object> schemas = yamlParser.getSchemas(openApiDoc);

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
            Map<String, Object> openApiDoc = specManager.convertSpecToMap(specManager.getApiSpec(Protocol.REST));
            if (openApiDoc == null) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            // Try to find schema with exact name first
            Map<String, Object> schemaDefinition = yamlParser.getSchema(openApiDoc, schemaName);

            // If not found, try with normalized name (simple class name)
            String actualSchemaName = schemaName;
            if (schemaDefinition == null) {
                String normalizedName = RequestDiffHelper.extractClassNameFromFullName(schemaName);
                schemaDefinition = yamlParser.getSchema(openApiDoc, normalizedName);
                if (schemaDefinition != null) {
                    actualSchemaName = normalizedName;
                }
            }

            if (schemaDefinition == null) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            return convertToResponse(actualSchemaName, schemaDefinition);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Update an existing OpenAPI schema using only the non-null fields from the request.
     *
     * Applies provided values (type, title, description, properties, required, orders, xmlName)
     * to the named schema, persists and validates the updated specification, and reloads the mock registry.
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
            Map<String, Object> openApiDoc = yamlParser.readDocumentFromFile();

            // Try to find schema with exact name first
            Map<String, Object> existingSchema = yamlParser.getSchema(openApiDoc, schemaName);
            String actualSchemaName = schemaName;

            // If not found, try with normalized name (simple class name)
            if (existingSchema == null) {
                String normalizedName = RequestDiffHelper.extractClassNameFromFullName(schemaName);
                existingSchema = yamlParser.getSchema(openApiDoc, normalizedName);
                if (existingSchema != null) {
                    actualSchemaName = normalizedName;
                }
            }

            if (existingSchema == null) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            // Update only provided fields
            if (request.getType() != null) {
                existingSchema.put("type", request.getType());
            }
            if (request.getTitle() != null) {
                existingSchema.put("title", request.getTitle());
            }
            if (request.getDescription() != null) {
                existingSchema.put("description", request.getDescription());
            }
            if (request.getProperties() != null) {
                existingSchema.put("properties", buildProperties(request.getProperties()));
            }
            if (request.getRequired() != null) {
                existingSchema.put("required", request.getRequired());
            }
            if (request.getOrders() != null) {
                existingSchema.put("x-ouroboros-orders", request.getOrders());
            }
            if (request.getXmlName() != null) {
                Map<String, Object> xml = new LinkedHashMap<>();
                xml.put("name", request.getXmlName());
                existingSchema.put("xml", xml);
            }
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

            // Write document directly
            yamlParser.writeDocument(openApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            // registry 초기화 후 재등록 (전체 읽기)
            reloadMockRegistry();

            log.info("Updated schema: {}", actualSchemaName);

            return convertToResponse(actualSchemaName, existingSchema);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a schema from the REST OpenAPI document, updates the processed spec cache, and reloads mock endpoints.
     *
     * @param schemaName the name of the schema to remove
     * @throws IllegalArgumentException if the specification file does not exist or the named schema is not found
     * @throws Exception if processing, caching, or mock registry reloading fails
     */
    @Override
    public void deleteSchema(String schemaName) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            // Read from file directly (not cache) for CUD operations
            Map<String, Object> openApiDoc = yamlParser.readDocumentFromFile();

            // Try to remove schema with exact name first
            boolean removed = yamlParser.removeSchema(openApiDoc, schemaName);
            String actualSchemaName = schemaName;

            // If not found, try with normalized name (simple class name)
            if (!removed) {
                String normalizedName = RequestDiffHelper.extractClassNameFromFullName(schemaName);
                removed = yamlParser.removeSchema(openApiDoc, normalizedName);
                if (removed) {
                    actualSchemaName = normalizedName;
                }
            }

            if (!removed) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            // Write document directly
            yamlParser.writeDocument(openApiDoc);

            // Update cache (validates with scanned state + updates cache, but does not write file)
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            // registry 초기화 후 재등록 (전체 읽기)
            reloadMockRegistry();

            log.info("Deleted schema: {}", actualSchemaName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Helper methods for building schema structures

    private Map<String, Object> buildSchemaDefinition(CreateSchemaRequest request) {
        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", request.getType());

        if (request.getTitle() != null) {
            schema.put("title", request.getTitle());
        }

        if (request.getDescription() != null) {
            schema.put("description", request.getDescription());
        }

        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            schema.put("properties", buildProperties(request.getProperties()));
        }

        if (request.getRequired() != null && !request.getRequired().isEmpty()) {
            schema.put("required", request.getRequired());
        }

        if (request.getOrders() != null && !request.getOrders().isEmpty()) {
            schema.put("x-ouroboros-orders", request.getOrders());
        }

        if (request.getXmlName() != null) {
            Map<String, Object> xml = new LinkedHashMap<>();
            xml.put("name", request.getXmlName());
            schema.put("xml", xml);
        }

        // Array items 처리 (Array 타입일 때 items 필드 처리)
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

        if (property.getMockExpression() != null) {
            propertyMap.put("x-ouroboros-mock", property.getMockExpression());
        }

        // For object types - nested properties (재귀!)
        if (property.getProperties() != null && !property.getProperties().isEmpty()) {
            propertyMap.put("properties", buildProperties(property.getProperties()));
        }

        if (property.getRequired() != null && !property.getRequired().isEmpty()) {
            propertyMap.put("required", property.getRequired());
        }

        // For array types - items (재귀!)
        if (property.getItems() != null) {
            propertyMap.put("items", buildProperty(property.getItems()));
        }
        if (property.getMinItems() != null) {
            propertyMap.put("minItems", property.getMinItems());
        }
        if (property.getMaxItems() != null) {
            propertyMap.put("maxItems", property.getMaxItems());
        }

        // Format (file 타입 구분용)
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

        // Extract XML name
        Object xmlObj = schemaDefinition.get("xml");
        if (xmlObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> xml = (Map<String, Object>) xmlObj;
            builder.xmlName(safeGetString(xml, "name"));
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

        // Check for schema reference in YAML ($ref)
        String dollarRef = safeGetString(propertyDefinition, "$ref");
        if (dollarRef != null) {
            // Convert $ref to simplified ref for client
            if (dollarRef.startsWith("#/components/schemas/")) {
                String simplifiedRef = dollarRef.substring("#/components/schemas/".length());
                builder.ref(simplifiedRef);
            } else {
                builder.ref(dollarRef);
            }
            return builder.build(); // Reference mode: return early
        }

        // Inline mode: extract all property fields
        builder.type(safeGetString(propertyDefinition, "type"))
                .description(safeGetString(propertyDefinition, "description"))
                .mockExpression(safeGetString(propertyDefinition, "x-ouroboros-mock"))
                .minItems(safeGetInteger(propertyDefinition, "minItems"))
                .maxItems(safeGetInteger(propertyDefinition, "maxItems"));

        // For object types - nested properties (재귀!)
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

        // Handle nested items for array type (재귀!)
        Object itemsObj = propertyDefinition.get("items");
        if (itemsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> items = (Map<String, Object>) itemsObj;
            builder.items(convertToProperty(items));
        }

        // Format (file 타입 구분용)
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
     * Safely extracts a Number value from a Map.
     */
    private Number safeGetNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value != null) {
            log.warn("Expected Number for key '{}' but got {}", key, value.getClass().getSimpleName());
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
     * Reloads mock endpoints in the registry from YAML mock definitions.
     *
     * Clears the current registry, loads endpoint metadata from the YAML source, registers each endpoint, and logs the number of endpoints reloaded.
     */
    private void reloadMockRegistry() {
        mockRegistry.clear();
        Map<String, kr.co.ouroboros.core.rest.mock.model.EndpointMeta> endpoints = mockLoaderService.loadFromYaml();
        endpoints.values().forEach(mockRegistry::register);
        log.info("Reloaded {} mock endpoints into registry", endpoints.size());
    }

}