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
