package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser;
import kr.co.ouroboros.core.rest.spec.dto.CreateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.dto.SchemaResponse;
import kr.co.ouroboros.core.rest.spec.dto.UpdateSchemaRequest;
import kr.co.ouroboros.core.rest.spec.model.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

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

    @Override
    public SchemaResponse createSchema(CreateSchemaRequest request) throws Exception {
        // Read existing document or create new one
        Map<String, Object> openApiDoc = yamlParser.readOrCreateDocument();

        // Check for duplicate schema name
        if (yamlParser.schemaExists(openApiDoc, request.getSchemaName())) {
            throw new IllegalArgumentException("Schema '" + request.getSchemaName() + "' already exists");
        }

        // Build schema definition
        Map<String, Object> schemaDefinition = buildSchemaDefinition(request);

        // Add schema to document
        yamlParser.putSchema(openApiDoc, request.getSchemaName(), schemaDefinition);

        // Write back to file
        yamlParser.writeDocument(openApiDoc);

        log.info("Created schema: {}", request.getSchemaName());

        return convertToResponse(request.getSchemaName(), schemaDefinition);
    }

    @Override
    public List<SchemaResponse> getAllSchemas() throws Exception {
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
    }

    @Override
    public SchemaResponse getSchema(String schemaName) throws Exception {
        if (!yamlParser.fileExists()) {
            throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
        }

        Map<String, Object> openApiDoc = yamlParser.readDocument();
        Map<String, Object> schemaDefinition = yamlParser.getSchema(openApiDoc, schemaName);

        if (schemaDefinition == null) {
            throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
        }

        return convertToResponse(schemaName, schemaDefinition);
    }

    @Override
    public SchemaResponse updateSchema(String schemaName, UpdateSchemaRequest request) throws Exception {
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

        // Write back to file
        yamlParser.writeDocument(openApiDoc);

        log.info("Updated schema: {}", schemaName);

        return convertToResponse(schemaName, existingSchema);
    }

    @Override
    public void deleteSchema(String schemaName) throws Exception {
        if (!yamlParser.fileExists()) {
            throw new IllegalArgumentException("No schemas found. The specification file does not exist.");
        }

        Map<String, Object> openApiDoc = yamlParser.readDocument();

        boolean removed = yamlParser.removeSchema(openApiDoc, schemaName);

        if (!removed) {
            throw new IllegalArgumentException("Schema '" + schemaName + "' not found");
        }

        // Write back to file
        yamlParser.writeDocument(openApiDoc);

        log.info("Deleted schema: {}", schemaName);
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

        // For array types
        if (property.getItems() != null) {
            propertyMap.put("items", buildProperty(property.getItems()));
        }

        if (property.getMinItems() != null) {
            propertyMap.put("minItems", property.getMinItems());
        }

        if (property.getMaxItems() != null) {
            propertyMap.put("maxItems", property.getMaxItems());
        }

        return propertyMap;
    }

    @SuppressWarnings("unchecked")
    private SchemaResponse convertToResponse(String schemaName, Map<String, Object> schemaDefinition) {
        SchemaResponse.SchemaResponseBuilder builder = SchemaResponse.builder()
                .schemaName(schemaName)
                .type((String) schemaDefinition.get("type"))
                .title((String) schemaDefinition.get("title"))
                .description((String) schemaDefinition.get("description"))
                .required((List<String>) schemaDefinition.get("required"))
                .orders((List<String>) schemaDefinition.get("x-ouroboros-orders"));

        // Extract properties
        Map<String, Object> propertiesMap = (Map<String, Object>) schemaDefinition.get("properties");
        if (propertiesMap != null) {
            Map<String, Property> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                properties.put(entry.getKey(), convertToProperty(propDef));
            }
            builder.properties(properties);
        }

        // Extract XML name
        Map<String, Object> xml = (Map<String, Object>) schemaDefinition.get("xml");
        if (xml != null) {
            builder.xmlName((String) xml.get("name"));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Property convertToProperty(Map<String, Object> propertyDefinition) {
        Property.PropertyBuilder builder = Property.builder();

        // Check for schema reference in YAML ($ref)
        String dollarRef = (String) propertyDefinition.get("$ref");
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
        builder.type((String) propertyDefinition.get("type"))
                .description((String) propertyDefinition.get("description"))
                .mockExpression((String) propertyDefinition.get("x-ouroboros-mock"))
                .minItems((Integer) propertyDefinition.get("minItems"))
                .maxItems((Integer) propertyDefinition.get("maxItems"));

        // Handle nested items for array type
        Map<String, Object> items = (Map<String, Object>) propertyDefinition.get("items");
        if (items != null) {
            builder.items(convertToProperty(items));
        }

        return builder.build();
    }
}