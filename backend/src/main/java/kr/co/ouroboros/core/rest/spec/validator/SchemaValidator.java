package kr.co.ouroboros.core.rest.spec.validator;

import kr.co.ouroboros.core.rest.spec.model.Property;
import kr.co.ouroboros.core.rest.spec.model.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates and auto-corrects schema constraints.
 * <p>
 * Ensures that min/max constraints are logically valid:
 * <ul>
 *   <li>If minItems &gt; maxItems, swaps the values automatically</li>
 * </ul>
 * <p>
 * This validator is applied during:
 * <ul>
 *   <li>API specification creation (POST /ouro/rest-specs)</li>
 *   <li>YAML import (POST /ouro/rest/import)</li>
 *   <li>Initial YAML loading (application startup)</li>
 * </ul>
 *
 * @since 0.0.1
 */
@Slf4j
@Component
public class SchemaValidator {

    /**
     * Validates and auto-corrects schema constraints.
     * <p>
     * If the schema has properties, validates each property recursively.
     *
     * @param schema the schema to validate (can be null)
     * @return true if any corrections were made, false otherwise
     */
    public boolean validateAndCorrect(Schema schema) {
        if (schema == null) {
            return false;
        }

        // Skip validation for $ref schemas
        if (schema.getRef() != null) {
            return false;
        }

        boolean corrected = false;

        // Validate properties if present
        Map<String, Property> properties = schema.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Property> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Property property = entry.getValue();
                if (validateAndCorrectProperty(propertyName, property)) {
                    corrected = true;
                }
            }
        }

        return corrected;
    }

    /**
     * Validates and auto-corrects a single property.
     * <p>
     * Checks minItems/maxItems and swaps if necessary.
     * Recursively validates nested properties (items field for arrays).
     *
     * @param propertyName the name of the property (for logging)
     * @param property the property to validate (can be null)
     * @return true if any corrections were made, false otherwise
     */
    public boolean validateAndCorrectProperty(String propertyName, Property property) {
        if (property == null) {
            return false;
        }

        // Skip validation for $ref properties
        if (property.getRef() != null) {
            return false;
        }

        boolean corrected = false;

        // Validate minItems/maxItems for array types
        if ("array".equalsIgnoreCase(property.getType())) {
            if (validateAndCorrectMinMax(
                    propertyName,
                    property.getMinItems(),
                    property.getMaxItems(),
                    "minItems",
                    "maxItems"
            )) {
                // Swap the values
                Integer temp = property.getMinItems();
                property.setMinItems(property.getMaxItems());
                property.setMaxItems(temp);
                corrected = true;
            }

            // Recursively validate items (array element type)
            if (property.getItems() != null) {
                if (validateAndCorrectProperty(propertyName + ".items", property.getItems())) {
                    corrected = true;
                }
            }
        }

        return corrected;
    }

    /**
     * Validates min/max constraint and logs if swap is needed.
     * <p>
     * Returns true if min &gt; max, indicating that the caller should swap the values.
     *
     * @param propertyName the property name (for logging)
     * @param min the minimum value (can be null)
     * @param max the maximum value (can be null)
     * @param minFieldName the name of the min field (e.g., "minItems")
     * @param maxFieldName the name of the max field (e.g., "maxItems")
     * @return true if min &gt; max and swap is needed, false otherwise
     */
    private boolean validateAndCorrectMinMax(
            String propertyName,
            Integer min,
            Integer max,
            String minFieldName,
            String maxFieldName
    ) {
        if (min == null || max == null) {
            return false; // Nothing to validate
        }

        if (min > max) {
            log.warn("🔧 Auto-correcting property '{}': {} ({}) > {} ({}) - swapping values",
                    propertyName, minFieldName, min, maxFieldName, max);
            return true; // Caller should swap
        }

        return false; // Valid constraint
    }

    /**
     * Validates minItems/maxItems in a raw Map (for YAML validation).
     * <p>
     * This is used during YAML file validation where we work with raw Map objects
     * instead of domain models.
     *
     * @param propertyName the property name (for logging)
     * @param propertyMap the property map from YAML
     * @return true if any corrections were made, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean validateAndCorrectPropertyMap(String propertyName, Map<String, Object> propertyMap) {
        if (propertyMap == null) {
            return false;
        }

        // Skip validation for $ref properties
        if (propertyMap.containsKey("$ref") || propertyMap.containsKey("ref")) {
            return false;
        }

        boolean corrected = false;

        // Check if this is an array type
        Object typeObj = propertyMap.get("type");
        if ("array".equals(typeObj)) {
            Object minItemsObj = propertyMap.get("minItems");
            Object maxItemsObj = propertyMap.get("maxItems");

            if (minItemsObj instanceof Integer && maxItemsObj instanceof Integer) {
                Integer minItems = (Integer) minItemsObj;
                Integer maxItems = (Integer) maxItemsObj;

                if (minItems > maxItems) {
                    log.warn("🔧 Auto-correcting property '{}': minItems ({}) > maxItems ({}) - swapping values",
                            propertyName, minItems, maxItems);
                    propertyMap.put("minItems", maxItems);
                    propertyMap.put("maxItems", minItems);
                    corrected = true;
                }
            }

            // Recursively validate items
            Object itemsObj = propertyMap.get("items");
            if (itemsObj instanceof Map) {
                if (validateAndCorrectPropertyMap(propertyName + ".items", (Map<String, Object>) itemsObj)) {
                    corrected = true;
                }
            }
        }

        return corrected;
    }

    /**
     * Validates all properties in a schema map (for YAML validation).
     * <p>
     * This is used during YAML file validation where we work with raw Map objects.
     *
     * @param schemaMap the schema map from YAML
     * @return true if any corrections were made, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean validateAndCorrectSchemaMap(Map<String, Object> schemaMap) {
        if (schemaMap == null) {
            return false;
        }

        // Skip validation for $ref schemas
        if (schemaMap.containsKey("$ref") || schemaMap.containsKey("ref")) {
            return false;
        }

        boolean corrected = false;

        // Validate properties if present
        Object propertiesObj = schemaMap.get("properties");
        if (propertiesObj instanceof Map) {
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Object propertyObj = entry.getValue();

                if (propertyObj instanceof Map) {
                    if (validateAndCorrectPropertyMap(propertyName, (Map<String, Object>) propertyObj)) {
                        corrected = true;
                    }
                }
            }
        }

        return corrected;
    }
}
