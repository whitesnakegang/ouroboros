package kr.co.ouroboros.core.global.spec;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for validating API specification fields.
 * <p>
 * Provides validation methods for common specification constraints
 * such as Korean character detection in paths and entrypoints,
 * and normalization methods for tags.
 *
 * @since 0.1.0
 */
public class SpecValidationUtil {

    /**
     * Validates that a path or entrypoint does not contain Korean characters.
     * <p>
     * Throws {@link IllegalArgumentException} if Korean characters are detected.
     *
     * @param value the value to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value contains Korean characters
     */
    public static void validateNoKorean(String value, String fieldName) {
        if (value != null && containsKorean(value)) {
            throw new IllegalArgumentException(
                    fieldName + " cannot contain Korean characters: " + value
            );
        }
    }

    /**
     * Validates that a name is a simple class name, not a package-qualified name.
     * <p>
     * Throws {@link IllegalArgumentException} if the name contains a dot (.) character,
     * indicating it is a fully qualified package name.
     *
     * @param value the value to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws IllegalArgumentException if the value contains a dot (package separator)
     */
    public static void validateSimpleClassName(String value, String fieldName) {
        if (value != null && value.contains(".")) {
            throw new IllegalArgumentException(
                    fieldName + " must be a simple class name, not a package-qualified name: " + value
            );
        }
    }

    /**
     * Validates that all names in a list are simple class names.
     * <p>
     * Throws {@link IllegalArgumentException} if any name contains a dot (.) character.
     *
     * @param values the list of values to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws IllegalArgumentException if any value contains a dot (package separator)
     */
    public static void validateSimpleClassNames(List<String> values, String fieldName) {
        if (values != null) {
            for (String value : values) {
                validateSimpleClassName(value, fieldName);
            }
        }
    }

    /**
     * Normalizes REST API tags to uppercase.
     * <p>
     * Converts all tag strings in the list to uppercase.
     *
     * @param tags the list of tag strings to normalize
     * @return normalized list with uppercase tags, or null if input is null
     */
    public static List<String> normalizeRestTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .map(tag -> tag != null ? tag.toUpperCase() : null)
                .collect(Collectors.toList());
    }

    /**
     * Normalizes WebSocket/AsyncAPI tags to uppercase.
     * <p>
     * Converts all tag name fields in the list to uppercase.
     * Each tag is a Map with a "name" field.
     *
     * @param tags the list of tag maps to normalize
     * @return normalized list with uppercase tag names, or null if input is null
     */
    public static List<Map<String, String>> normalizeWebSocketTags(List<Map<String, String>> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .map(tag -> {
                    if (tag != null && tag.containsKey("name")) {
                        Map<String, String> normalizedTag = new java.util.LinkedHashMap<>(tag);
                        String name = normalizedTag.get("name");
                        if (name != null) {
                            normalizedTag.put("name", name.toUpperCase());
                        }
                        return normalizedTag;
                    }
                    return tag;
                })
                .collect(Collectors.toList());
    }

    /**
     * Checks if a string contains Korean characters.
     * <p>
     * Detects:
     * <ul>
     *   <li>Complete Korean characters (가-힣): U+AC00 to U+D7A3</li>
     *   <li>Hangul Jamo (초성/중성/종성): U+1100 to U+11FF</li>
     *   <li>Hangul Compatibility Jamo (호환 자모): U+3130 to U+318F</li>
     * </ul>
     *
     * @param str the string to check
     * @return true if Korean characters are found, false otherwise
     */
    private static boolean containsKorean(String str) {
        return str.chars().anyMatch(ch ->
                (ch >= 0xAC00 && ch <= 0xD7A3) ||  // 가-힣 (Complete Korean characters)
                (ch >= 0x1100 && ch <= 0x11FF) ||  // Hangul Jamo (초성/중성/종성)
                (ch >= 0x3130 && ch <= 0x318F)     // Hangul Compatibility Jamo (호환 자모)
        );
    }
}

