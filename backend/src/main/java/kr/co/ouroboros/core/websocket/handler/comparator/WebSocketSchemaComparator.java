package kr.co.ouroboros.core.websocket.handler.comparator;

import kr.co.ouroboros.core.websocket.common.dto.Components;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.Schema;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class WebSocketSchemaComparator {

    /**
     * Compare schemas between a file-based WebSocket API spec and a scanned spec and report per-schema equality.
     *
     * Compares schema definitions from the provided fileSpec and scanSpec (scan spec keys are normalized to simple
     * class names). For each schema name present in either spec, records whether the two schemas are structurally equal.
     * If a schema is missing from either spec, it is considered unequal.
     *
     * @param fileSpec the WebSocket API specification loaded from the file (may be null)
     * @param scanSpec the WebSocket API specification obtained from scanning (may be null); keys are normalized before comparison
     * @return a map from schema name to `true` if the schema in fileSpec and scanSpec are equal, `false` otherwise
     */
    public Map<String, Boolean> compareSchemas(OuroWebSocketApiSpec fileSpec, OuroWebSocketApiSpec scanSpec) {
        Map<String, Boolean> result = new HashMap<>();

        Map<String, Schema> fileSchemas = extractSchemas(fileSpec);
        Map<String, Schema> scanSchemas = extractAndNormalizeSchemas(scanSpec);

        // 모든 스키마 이름을 수집 (File과 Scan 모두)
        Set<String> allSchemaNames = new HashSet<>();
        allSchemaNames.addAll(fileSchemas.keySet());
        allSchemaNames.addAll(scanSchemas.keySet());

        // 각 스키마 이름에 대해 비교
        for (String schemaName : allSchemaNames) {
            Schema fileSchema = fileSchemas.get(schemaName);
            Schema scanSchema = scanSchemas.get(schemaName);

            // File에만 존재하거나 Scan에만 존재하는 경우
            if (fileSchema == null || scanSchema == null) {
                result.put(schemaName, false);
                continue;
            }

            // 두 스키마를 재귀적으로 비교
            boolean isEqual = schemasEqual(fileSchema, scanSchema);
            result.put(schemaName, isEqual);
        }

        return result;
    }

    /**
     * Retrieve the components' schemas from the given WebSocket API specification.
     *
     * @param spec the OuroWebSocketApiSpec to extract schemas from; may be null
     * @return a map of schema name to Schema from spec.getComponents().getSchemas(), or an empty map if
     *         the spec, its components, or its schemas are null
     */
    private Map<String, Schema> extractSchemas(OuroWebSocketApiSpec spec) {
        if (spec == null) {
            return new HashMap<>();
        }

        Components components = spec.getComponents();
        if (components == null) {
            return new HashMap<>();
        }

        Map<String, Schema> schemas = components.getSchemas();
        return schemas != null ? schemas : new HashMap<>();
    }

    /**
     * Extracts schemas from the given WebSocket API spec and normalizes each schema's key to its simple class name.
     *
     * @param spec the WebSocket API specification to extract schemas from; may be null
     * @return a map whose keys are simple class names (substring after the last '.') corresponding to the original schema names
     *         (or `null` if an original name was null) and whose values are the original Schema instances; returns an empty map if the spec contains no schemas
     */
    private Map<String, Schema> extractAndNormalizeSchemas(OuroWebSocketApiSpec spec) {
        Map<String, Schema> originalSchemas = extractSchemas(spec);
        Map<String, Schema> normalizedSchemas = new HashMap<>();

        for (Map.Entry<String, Schema> entry : originalSchemas.entrySet()) {
            String originalKey = entry.getKey();
            String normalizedKey = extractSimpleClassName(originalKey);
            normalizedSchemas.put(normalizedKey, entry.getValue());
        }

        return normalizedSchemas;
    }

    /**
     * Extracts the simple class name from a fully-qualified class name.
     *
     * @param fullClassName the fully-qualified class name (may be null)
     * @return the substring after the last '.' if present, the original string if it contains no '.', or {@code null} if {@code fullClassName} is {@code null}
     */
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return null;
        }
        int lastDotIndex = fullClassName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fullClassName;
        }
        return fullClassName.substring(lastDotIndex + 1);
    }

    /**
     * Determines whether two schema reference strings refer to the same schema after normalization.
     *
     * Normalization will canonicalize component schema references (for example converting
     * "#/components/schemas/com.example.Foo" to "#/components/schemas/Foo"). Null references are treated as unequal.
     *
     * @param fileRef the reference string from the file specification, or null
     * @param scanRef the reference string from the scanned specification, or null
     * @return `true` if the normalized references are equal, `false` otherwise
     */
    private boolean refsEqual(String fileRef, String scanRef) {
        if (fileRef == scanRef) {
            return true;
        }
        if (fileRef == null || scanRef == null) {
            return false;
        }

        String normalizedFileRef = normalizeRef(fileRef);
        String normalizedScanRef = normalizeRef(scanRef);

        return normalizedFileRef.equals(normalizedScanRef);
    }

    /**
     * Normalize a component schema reference to use the canonical components/schemas prefix with a simple class name.
     *
     * If `ref` is null, returns null. If `ref` does not start with "#/components/schemas/", returns `ref` unchanged.
     * When `ref` uses the components schemas prefix, the substring after the prefix is reduced to its simple class name
     * (text after the last dot) and returned prefixed with "#/components/schemas/".
     *
     * @param ref the reference string to normalize, may be a full component reference or any other string
     * @return the normalized component schema reference, the original `ref` if not a component schema reference, or null if `ref` is null
     */
    private String normalizeRef(String ref) {
        if (ref == null) {
            return null;
        }

        final String prefix = "#/components/schemas/";
        if (!ref.startsWith(prefix)) {
            return ref;
        }

        String schemaName = ref.substring(prefix.length());
        String simpleSchemaName = extractSimpleClassName(schemaName);
        return prefix + simpleSchemaName;
    }

    /**
     * Determine whether two Schema objects represent the same schema structure.
     *
     * Compares title, type, format, `$ref` (after normalization), enum values, required fields,
     * items (recursively), and properties (recursively).
     *
     * @param fileSchema the schema from the file specification to compare
     * @param scanSchema the schema from the scanned specification to compare
     * @return `true` if the two schemas are equivalent across the compared fields, `false` otherwise
     */
    private boolean schemasEqual(Schema fileSchema, Schema scanSchema) {
        if (fileSchema == scanSchema) {
            return true;
        }
        if (fileSchema == null || scanSchema == null) {
            return false;
        }

        // title 비교
        if (!Objects.equals(fileSchema.getTitle(), scanSchema.getTitle())) {
            return false;
        }

        // type 비교
        if (!Objects.equals(fileSchema.getType(), scanSchema.getType())) {
            return false;
        }

        // format 비교
        if (!Objects.equals(fileSchema.getFormat(), scanSchema.getFormat())) {
            return false;
        }

        // $ref 비교 (패키지 경로 정규화 후 비교)
        if (!refsEqual(fileSchema.getRef(), scanSchema.getRef())) {
            return false;
        }

        // enum 값 비교
        if (!Objects.equals(fileSchema.getEnumValues(), scanSchema.getEnumValues())) {
            return false;
        }

        // required 필드 비교
        if (!Objects.equals(fileSchema.getRequired(), scanSchema.getRequired())) {
            return false;
        }

        // items 비교 (재귀)
        if (!schemasEqual(fileSchema.getItems(), scanSchema.getItems())) {
            return false;
        }

        // properties 비교 (재귀)
        return propertiesEqual(fileSchema.getProperties(), scanSchema.getProperties());
    }

    /**
     * Compare two property maps for deep structural equality of their Schema values.
     *
     * The maps are considered equal only if they are the same object, or if both are non-null,
     * have the same size, contain identical key sets, and each corresponding Schema is equal
     * according to {@code schemasEqual}.
     *
     * @param fileProp property name to Schema map from the file specification
     * @param scanProp property name to Schema map from the scanned specification
     * @return `true` if both maps have identical keys and corresponding Schema values are deeply equal, `false` otherwise
     */
    private boolean propertiesEqual(Map<String, Schema> fileProp, Map<String, Schema> scanProp) {
        if (fileProp == scanProp) {
            return true;
        }
        if (fileProp == null || scanProp == null) {
            return false;
        }

        // 키 개수 비교
        if (fileProp.size() != scanProp.size()) {
            return false;
        }

        // 모든 키가 동일한지 확인
        if (!fileProp.keySet().equals(scanProp.keySet())) {
            return false;
        }

        // 각 property를 재귀적으로 비교
        for (String key : fileProp.keySet()) {
            Schema schema1 = fileProp.get(key);
            Schema schema2 = scanProp.get(key);

            if (!schemasEqual(schema1, schema2)) {
                return false;
            }
        }

        return true;
    }
}