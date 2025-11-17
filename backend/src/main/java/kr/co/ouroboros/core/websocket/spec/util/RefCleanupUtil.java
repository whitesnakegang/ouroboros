package kr.co.ouroboros.core.websocket.spec.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for cleaning up package names from references in WebSocket specifications.
 * <p>
 * Provides methods to remove package prefixes and full paths from $ref values,
 * returning only simple class names for cleaner API responses.
 *
 * @since 0.1.0
 */
public class RefCleanupUtil {

    /**
     * Recursively removes package names from all $ref values in a map.
     * <p>
     * Converts references like "#/components/messages/com.example.Message" to "Message".
     * Creates a deep copy to avoid modifying the original map.
     *
     * @param map the map to process
     * @return a new map with cleaned $ref values
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> cleanupPackageNamesInRefs(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("$ref".equals(key) && value instanceof String) {
                // Clean package name from $ref value
                String ref = (String) value;
                cleaned.put(key, cleanRefValue(ref));
            } else if (value instanceof Map) {
                // Recursively clean nested maps
                cleaned.put(key, cleanupPackageNamesInRefs((Map<String, Object>) value));
            } else if (value instanceof List) {
                // Recursively clean lists
                List<Object> list = (List<Object>) value;
                List<Object> cleanedList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        cleanedList.add(cleanupPackageNamesInRefs((Map<String, Object>) item));
                    } else {
                        cleanedList.add(item);
                    }
                }
                cleaned.put(key, cleanedList);
            } else {
                cleaned.put(key, value);
            }
        }
        return cleaned;
    }

    /**
     * Cleans a single $ref value by removing prefix and package names, returning only the class name.
     * <p>
     * Examples:
     * <ul>
     *   <li>"#/components/schemas/com.example.User" -> "User"</li>
     *   <li>"#/components/messages/com.example.Message" -> "Message"</li>
     *   <li>"#/channels/channelName/messages/com.example.Msg" -> "Msg"</li>
     * </ul>
     *
     * @param ref the $ref value to clean
     * @return simple class name without prefix or package
     */
    public static String cleanRefValue(String ref) {
        if (ref == null || !ref.contains("/")) {
            return ref;
        }

        // Get the last part after the final slash
        int lastSlashIndex = ref.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return ref;
        }

        String name = ref.substring(lastSlashIndex + 1);

        // Extract class name from fully qualified name (removes package if present)
        return extractClassNameFromFullName(name);
    }

    /**
     * Extracts the simple class name from a package-qualified name.
     * <p>
     * Example: "com.c102.ourotest.dto.User" -> "User"
     *
     * @param fullName the package-qualified name
     * @return the simple class name, or the original string if no '.' is present
     */
    public static String extractClassNameFromFullName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return fullName;
        }

        int lastDotIndex = fullName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fullName;
        }

        return fullName.substring(lastDotIndex + 1);
    }
}
