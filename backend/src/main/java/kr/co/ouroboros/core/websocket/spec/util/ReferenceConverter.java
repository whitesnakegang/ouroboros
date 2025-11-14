package kr.co.ouroboros.core.websocket.spec.util;

import java.util.*;

/**
 * Utility class for converting between AsyncAPI $ref format (YAML) and ref format (JSON API).
 * <p>
 * AsyncAPI specifications use $ref for references in YAML format, but JSON APIs typically
 * use ref for better JavaScript compatibility. This class provides bidirectional conversion
 * with recursive support for nested Maps and Lists.
 *
 * @since 0.1.0
 */
public final class ReferenceConverter {

    private ReferenceConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts "ref" fields to "$ref" fields in a Map (for YAML storage).
     * <p>
     * Recursively processes nested Maps and Lists. If a ref value doesn't start with "#",
     * it will be converted to a full AsyncAPI schema path (#/components/schemas/{name}).
     *
     * @param map the source map (may contain "ref" fields)
     * @return new map with "ref" converted to "$ref", or null if input is null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertRefToDollarRef(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Convert "ref" key to "$ref" with full path
            if ("ref".equals(key) && value instanceof String) {
                String refValue = (String) value;
                // Convert simple schema name to full AsyncAPI path
                if (!refValue.startsWith("#")) {
                    refValue = "#/components/schemas/" + refValue;
                }
                result.put("$ref", refValue);
            } else if (value instanceof Map) {
                // Recursively process nested maps
                result.put(key, convertRefToDollarRef((Map<String, Object>) value));
            } else if (value instanceof List) {
                // Recursively process lists
                result.put(key, convertRefToDollarRefInList((List<Object>) value));
            } else {
                // Keep other fields as-is
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Converts "$ref" fields to "ref" fields in a Map (for JSON API).
     * <p>
     * Recursively processes nested Maps and Lists.
     *
     * @param map the source map (may contain "$ref" fields)
     * @return new map with "$ref" converted to "ref", or null if input is null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertDollarRefToRef(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Convert "$ref" key to "ref"
            if ("$ref".equals(key)) {
                result.put("ref", value);
            } else if (value instanceof Map) {
                // Recursively process nested maps
                result.put(key, convertDollarRefToRef((Map<String, Object>) value));
            } else if (value instanceof List) {
                // Recursively process lists
                result.put(key, convertDollarRefToRefInList((List<Object>) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Recursively converts "ref" to "$ref" in a List.
     *
     * @param list the list to process
     * @return new list with conversions applied, or null if input is null
     */
    @SuppressWarnings("unchecked")
    private static List<Object> convertRefToDollarRefInList(List<Object> list) {
        if (list == null) {
            return null;
        }

        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(convertRefToDollarRef((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(convertRefToDollarRefInList((List<Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Recursively converts "$ref" to "ref" in a List.
     *
     * @param list the list to process
     * @return new list with conversions applied, or null if input is null
     */
    @SuppressWarnings("unchecked")
    private static List<Object> convertDollarRefToRefInList(List<Object> list) {
        if (list == null) {
            return null;
        }

        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(convertDollarRefToRef((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(convertDollarRefToRefInList((List<Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Updates all schema $ref references according to a rename map.
     * <p>
     * Recursively scans the object tree for $ref fields pointing to schemas
     * and updates them based on the provided rename mapping.
     *
     * @param obj the object to scan (can be Map, List, or primitive)
     * @param schemaRenameMap map of old schema names to new names
     */
    @SuppressWarnings("unchecked")
    public static void updateSchemaReferences(Object obj, Map<String, String> schemaRenameMap) {
        if (schemaRenameMap == null || schemaRenameMap.isEmpty()) {
            return;
        }

        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;

            // Check if this map has a $ref field
            if (map.containsKey("$ref")) {
                String ref = (String) map.get("$ref");
                if (ref != null && ref.startsWith("#/components/schemas/")) {
                    String schemaName = ref.substring("#/components/schemas/".length());
                    if (schemaRenameMap.containsKey(schemaName)) {
                        String newSchemaName = schemaRenameMap.get(schemaName);
                        map.put("$ref", "#/components/schemas/" + newSchemaName);
                    }
                }
            }

            // Recursively scan all values
            for (Object value : map.values()) {
                updateSchemaReferences(value, schemaRenameMap);
            }

        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            for (Object item : list) {
                updateSchemaReferences(item, schemaRenameMap);
            }
        }
    }
}