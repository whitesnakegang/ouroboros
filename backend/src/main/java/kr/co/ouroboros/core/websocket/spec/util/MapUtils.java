package kr.co.ouroboros.core.websocket.spec.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Utility class for safely extracting values from Maps.
 * <p>
 * Provides type-safe extraction methods with automatic type validation and logging.
 * All methods return null if the value is not found or has an unexpected type.
 *
 * @since 0.1.0
 */
@Slf4j
public final class MapUtils {

    private MapUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Safely extracts a String value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the String value, or null if not found or not a String
     */
    public static String safeGetString(Map<String, Object> map, String key) {
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
    public static Integer safeGetInteger(Map<String, Object> map, String key) {
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
     * Safely extracts a Map value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the Map value, or null if not found or not a Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> safeGetMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value != null) {
            log.warn("Expected Map for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Safely extracts a List of Strings from a Map.
     * <p>
     * Validates that every element in the list is a String.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the list of strings, or null if not found, not a list, or contains non-String elements
     */
    @SuppressWarnings("unchecked")
    public static List<String> safeGetStringList(Map<String, Object> map, String key) {
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
     * Safely extracts a Boolean value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the Boolean value, or null if not found or not a Boolean
     */
    public static Boolean safeGetBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            log.warn("Expected Boolean for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Safely extracts a Long value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the Long value, or null if not found or not a Long
     */
    public static Long safeGetLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            log.warn("Expected Long for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Safely extracts a Double value from a Map.
     *
     * @param map the source map
     * @param key the key to look up
     * @return the Double value, or null if not found or not a Double
     */
    public static Double safeGetDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) {
            log.warn("Expected Double for key '{}' but got {}", key, value.getClass().getSimpleName());
        }
        return null;
    }
}
