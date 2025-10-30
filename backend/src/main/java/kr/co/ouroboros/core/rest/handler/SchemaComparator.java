package kr.co.ouroboros.core.rest.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import kr.co.ouroboros.core.rest.common.dto.Components;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import org.springframework.stereotype.Component;

/**
 * REST API 스키마를 비교하는 컴포넌트.
 * 
 * 스캔된 문서와 파일 기반 문서의 components.schemas를 비교하여
 * 각 스키마별 일치 여부를 검사하고 결과를 Map으로 반환합니다.
 * 
 * @since 0.0.1
 */
@Component
public class SchemaComparator {

    /**
     * Compare the components.schemas of two OpenAPI Components and report per-schema match results.
     *
     * @param scannedComponents the scanned (baseline) document's Components to compare from
     * @param fileComponents the file-based document's Components to compare against
     * @return a map from schema name to `true` if the scanned and file schema match, `false` otherwise
     */
    public Map<String, Boolean> compareSchemas(Components scannedComponents, Components fileComponents) {
        Map<String, Boolean> schemaMatchResults = new HashMap<>();
        
        if (scannedComponents == null || fileComponents == null) {
            System.out.println("[SCHEMA COMPARISON] Components가 null입니다.");
            return schemaMatchResults;
        }
        
        Map<String, Schema> scannedSchemas = scannedComponents.getSchemas();
        Map<String, Schema> fileSchemas = fileComponents.getSchemas();
        
        if (scannedSchemas == null || fileSchemas == null) {
            System.out.println("[SCHEMA COMPARISON] Schemas가 null입니다.");
            return schemaMatchResults;
        }
        
        System.out.println("[SCHEMA COMPARISON] 스키마 비교 시작...");
        System.out.println("스캔된 스키마: " + scannedSchemas.keySet());
        System.out.println("파일 스키마: " + fileSchemas.keySet());
        
        // 스캔된 문서의 각 스키마에 대해 파일 문서와 비교
        for (Map.Entry<String, Schema> scannedEntry : scannedSchemas.entrySet()) {
            String schemaName = scannedEntry.getKey();
            Schema scannedSchema = scannedEntry.getValue();
            Schema fileSchema = fileSchemas.get(schemaName);
            
            if (fileSchema == null) {
                System.out.println(String.format("[SCHEMA MISSING] '%s': 파일 스펙에 해당 스키마가 없습니다.", schemaName));
                schemaMatchResults.put(schemaName, false);
                continue;
            }
            
            // 스키마 상세 비교
            boolean isMatch = compareSchemaDetails(scannedSchema, fileSchema, schemaName);
            schemaMatchResults.put(schemaName, isMatch);
            
            if (isMatch) {
                System.out.println(String.format("[SCHEMA MATCH] '%s': 스키마가 일치합니다.", schemaName));
            } else {
                System.out.println(String.format("[SCHEMA MISMATCH] '%s': 스키마가 일치하지 않습니다.", schemaName));
            }
        }
        
        // 파일에만 있는 스키마들 확인
        for (String fileSchemaName : fileSchemas.keySet()) {
            if (!scannedSchemas.containsKey(fileSchemaName)) {
                System.out.println(String.format("[SCHEMA EXTRA] '%s': 스캔된 문서에 없는 추가 스키마입니다.", fileSchemaName));
                schemaMatchResults.put(fileSchemaName, false);
            }
        }
        
        System.out.println("[SCHEMA COMPARISON] 스키마 비교 완료. 결과: " + schemaMatchResults);
        return schemaMatchResults;
    }
    
    /**
     * Recursively compares the detailed content of two Schema objects for equivalence.
     *
     * Compares type, `$ref`, format, properties, items (for arrays), required list, and additionalProperties.
     *
     * @param scannedSchema the scanned (reference) schema to compare from
     * @param fileSchema the file-based schema to compare against
     * @param schemaName the schema name used for logging/context
     * @return `true` if the two schemas are equivalent in type, `$ref`, format, properties, items, required, and additionalProperties; `false` otherwise
     */
    private boolean compareSchemaDetails(Schema scannedSchema, Schema fileSchema, String schemaName) {
        if (scannedSchema == null && fileSchema == null) {
            return true;
        }
        if (scannedSchema == null || fileSchema == null) {
            System.out.println(String.format("[SCHEMA NULL MISMATCH] '%s': 한쪽 스키마가 null입니다.", schemaName));
            return false;
        }
        
        // 기본 타입 비교
        if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
            System.out.println(String.format("[SCHEMA TYPE MISMATCH] '%s': 타입이 다릅니다. (스캔: %s, 파일: %s)", 
                schemaName, scannedSchema.getType(), fileSchema.getType()));
            return false;
        }
        
        // $ref 비교
        if (!Objects.equals(scannedSchema.getRef(), fileSchema.getRef())) {
            System.out.println(String.format("[SCHEMA REF MISMATCH] '%s': $ref가 다릅니다. (스캔: %s, 파일: %s)", 
                schemaName, scannedSchema.getRef(), fileSchema.getRef()));
            return false;
        }
        
        // format 비교
        if (!Objects.equals(scannedSchema.getFormat(), fileSchema.getFormat())) {
            System.out.println(String.format("[SCHEMA FORMAT MISMATCH] '%s': format이 다릅니다. (스캔: %s, 파일: %s)", 
                schemaName, scannedSchema.getFormat(), fileSchema.getFormat()));
            return false;
        }
        
        // Properties 비교 (객체 타입인 경우)
        if (scannedSchema.getProperties() != null || fileSchema.getProperties() != null) {
            if (!compareSchemaProperties(scannedSchema.getProperties(), fileSchema.getProperties(), schemaName)) {
                return false;
            }
        }
        
        // Items 비교 (배열 타입인 경우)
        if (scannedSchema.getItems() != null || fileSchema.getItems() != null) {
            if (!compareSchemaDetails(scannedSchema.getItems(), fileSchema.getItems(), schemaName + ".items")) {
                return false;
            }
        }
        
        // Required 필드 비교
        if (!Objects.equals(scannedSchema.getRequired(), fileSchema.getRequired())) {
            System.out.println(String.format("[SCHEMA REQUIRED MISMATCH] '%s': required 필드가 다릅니다. (스캔: %s, 파일: %s)", 
                schemaName, scannedSchema.getRequired(), fileSchema.getRequired()));
            return false;
        }
        
        // AdditionalProperties 비교
        if (!Objects.equals(scannedSchema.getAdditionalProperties(), fileSchema.getAdditionalProperties())) {
            System.out.println(String.format("[SCHEMA ADDITIONAL_PROPERTIES MISMATCH] '%s': additionalProperties가 다릅니다.", schemaName));
            return false;
        }
        
        return true;
    }
    
    /**
     * Compare two schema property maps and determine whether they are equivalent.
     *
     * Compares keys and recursively compares each property's Schema; logs and returns false on any missing,
     * extra, or differing property schema. The schemaName is used only for logging context.
     *
     * @param scannedProperties the properties from the scanned document (baseline)
     * @param fileProperties the properties from the file-based document (to compare against)
     * @param schemaName schema name used for log messages and property path construction
     * @return {@code true} if both property maps contain the same keys and corresponding property schemas match, {@code false} otherwise
     */
    private boolean compareSchemaProperties(Map<String, Schema> scannedProperties, Map<String, Schema> fileProperties, String schemaName) {
        if (scannedProperties == null && fileProperties == null) {
            return true;
        }
        if (scannedProperties == null || fileProperties == null) {
            System.out.println(String.format("[PROPERTIES NULL MISMATCH] '%s': 한쪽 Properties가 null입니다.", schemaName));
            return false;
        }
        
        // 스캔된 문서의 각 Property에 대해 비교
        for (Map.Entry<String, Schema> scannedEntry : scannedProperties.entrySet()) {
            String propertyName = scannedEntry.getKey();
            Schema scannedPropertySchema = scannedEntry.getValue();
            Schema filePropertySchema = fileProperties.get(propertyName);
            
            if (filePropertySchema == null) {
                System.out.println(String.format("[PROPERTY MISSING] '%s.%s': Property가 파일 스펙에 없습니다.", 
                    schemaName, propertyName));
                return false;
            }
            
            if (!compareSchemaDetails(scannedPropertySchema, filePropertySchema, schemaName + "." + propertyName)) {
                return false;
            }
        }
        
        // 파일에만 있는 Property들 확인
        for (String filePropertyName : fileProperties.keySet()) {
            if (!scannedProperties.containsKey(filePropertyName)) {
                System.out.println(String.format("[PROPERTY EXTRA] '%s.%s': 스캔된 문서에 없는 추가 Property입니다.", 
                    schemaName, filePropertyName));
                return false;
            }
        }
        
        return true;
    }
}