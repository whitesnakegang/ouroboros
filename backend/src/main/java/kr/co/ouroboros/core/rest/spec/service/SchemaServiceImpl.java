package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
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
 * Implementation of {@link SchemaService}.
 * <p>
 * Manages schema definitions in the OpenAPI components/schemas section of ourorest.yml.
 * Uses {@link RestApiYamlParser} for all YAML file operations.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaServiceImpl implements SchemaService {

    private final RestApiYamlParser yamlParser;
    private final OuroApiSpecManager specManager;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public SchemaResponse createSchema(CreateSchemaRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            log.info("🔍 Creating schema: {}", request.getSchemaName());
            
            // Read existing document or create new one
            Map<String, Object> openApiDoc = yamlParser.readOrCreateDocument();

            // Check for duplicate schema name
            if (yamlParser.schemaExists(openApiDoc, request.getSchemaName())) {
                throw new IllegalArgumentException("Schema '" + request.getSchemaName() + "' already exists");
            }

            // Build schema definition
            Map<String, Object> schemaDefinition = buildSchemaDefinition(request);
            log.info("🔍 Built schema definition: {}", schemaDefinition);

            // Add schema to document
            yamlParser.putSchema(openApiDoc, request.getSchemaName(), schemaDefinition);

            // Process and cache: writes to file + validates with scanned state + updates cache
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            log.info("✅ Created schema: {}", request.getSchemaName());

            return convertToResponse(request.getSchemaName(), schemaDefinition);
        } catch (Exception e) {
            log.error("❌ Failed to create schema: {}", request.getSchemaName(), e);
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<SchemaResponse> getAllSchemas() throws Exception {
        lock.readLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                return new ArrayList<>();
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
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
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
            Map<String, Object> schemaDefinition = yamlParser.getSchema(openApiDoc, schemaName);

            if (schemaDefinition == null) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            return convertToResponse(schemaName, schemaDefinition);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SchemaResponse updateSchema(String schemaName, UpdateSchemaRequest request) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();
            Map<String, Object> existingSchema = yamlParser.getSchema(openApiDoc, schemaName);

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
                existingSchema.put("items", request.getItems());
            }

            // Process and cache: writes to file + validates with scanned state + updates cache
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            log.info("Updated schema: {}", schemaName);

            return convertToResponse(schemaName, existingSchema);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteSchema(String schemaName) throws Exception {
        lock.writeLock().lock();
        try {
            if (!yamlParser.fileExists()) {
                throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
            }

            Map<String, Object> openApiDoc = yamlParser.readDocument();

            boolean removed = yamlParser.removeSchema(openApiDoc, schemaName);

            if (!removed) {
                throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
            }

            // Process and cache: writes to file + validates with scanned state + updates cache
            specManager.processAndCacheSpec(Protocol.REST, openApiDoc);

            log.info("Deleted schema: {}", schemaName);
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
            schema.put("items", request.getItems());
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
        
        // Additional constraints
        if (property.getFormat() != null) {
            propertyMap.put("format", property.getFormat());
        }
        
        if (property.getEnumValues() != null && !property.getEnumValues().isEmpty()) {
            propertyMap.put("enum", property.getEnumValues());
        }
        
        if (property.getPattern() != null) {
            propertyMap.put("pattern", property.getPattern());
        }
        
        if (property.getMinLength() != null) {
            propertyMap.put("minLength", property.getMinLength());
        }
        
        if (property.getMaxLength() != null) {
            propertyMap.put("maxLength", property.getMaxLength());
        }
        
        if (property.getMinimum() != null) {
            propertyMap.put("minimum", property.getMinimum());
        }
        
        if (property.getMaximum() != null) {
            propertyMap.put("maximum", property.getMaximum());
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
        
        // Additional constraints
        builder.format(safeGetString(propertyDefinition, "format"))
               .pattern(safeGetString(propertyDefinition, "pattern"))
               .minLength(safeGetInteger(propertyDefinition, "minLength"))
               .maxLength(safeGetInteger(propertyDefinition, "maxLength"))
               .minimum(safeGetNumber(propertyDefinition, "minimum"))
               .maximum(safeGetNumber(propertyDefinition, "maximum"));
        
        // enum 값 파싱
        Object enumObj = propertyDefinition.get("enum");
        if (enumObj instanceof java.util.Collection<?> enumCollection) {
            List<String> enumValues = new java.util.ArrayList<>();
            for (Object item : enumCollection) {
                enumValues.add(item != null ? item.toString() : "");
            }
            builder.enumValues(enumValues);
        }

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
     * Safely extracts a List of Strings from a Map.
     * 
     * @param map the source map
     * @param key the key to look up
     * @return the List of Strings, or null if not found or not a valid list
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
}