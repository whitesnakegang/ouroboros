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
     * 두 스펙의 components.schemas를 비교하여 일치 여부를 반환합니다.
     *
     * @param scannedComponents 스캔된 문서의 Components (기준)
     * @param fileComponents 파일 기반 문서의 Components (비교 대상)
     * @return 스키마명을 키로 하고 일치 여부를 값으로 하는 Map
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
     * 두 스키마의 상세 내용을 재귀적으로 비교합니다.
     *
     * @param scannedSchema 스캔된 스키마 (기준)
     * @param fileSchema 파일 기반 스키마 (비교 대상)
     * @param schemaName 스키마명 (로깅용)
     * @return 스키마가 일치하면 true, 그렇지 않으면 false
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
     * 스키마의 Properties를 비교합니다.
     *
     * @param scannedProperties 스캔된 Properties (기준)
     * @param fileProperties 파일 기반 Properties (비교 대상)
     * @param schemaName 스키마명 (로깅용)
     * @return Properties가 일치하면 true, 그렇지 않으면 false
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
