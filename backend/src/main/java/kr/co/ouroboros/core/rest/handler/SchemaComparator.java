package kr.co.ouroboros.core.rest.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import kr.co.ouroboros.core.rest.common.dto.Components;
import kr.co.ouroboros.core.rest.common.dto.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * REST API 스키마를 비교하는 컴포넌트.
 * <p>
 * 스캔된 문서와 파일 기반 문서의 components.schemas를 비교하여 각 스키마별 일치 여부를 검사하고 결과를 Map으로 반환합니다.
 *
 * @since 0.0.1
 */
@Slf4j
@Component
public class SchemaComparator {

    /**
     * 타입 카운트를 저장하는 클래스
     * 내부적으로 Map<String, Integer>를 가지며, 키는 "{필드명:타입}" 형식
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCnts {
        private Map<String, Integer> typeCounts = new HashMap<>();
    }

    /**
     * 스키마를 평탄화하여 타입별 개수를 구합니다.
     * <p>
     * 각 스키마에 대해 필드명과 타입을 조합한 키로 타입 개수를 카운트합니다.
     * format이 "binary"인 경우 타입으로 취급합니다.
     *
     * @param components Components 객체 (스키마들이 포함됨)
     * @return 스키마 이름 -> TypeCnts 맵
     */
    public Map<String, TypeCnts> flattenSchemas(Components components) {
        Map<String, TypeCnts> result = new HashMap<>();
        
        if (components == null || components.getSchemas() == null) {
            return result;
        }
        
        Map<String, Schema> schemas = components.getSchemas();
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            Schema schema = entry.getValue();
            
            TypeCnts typeCnts = flattenSchema(schemaName, schema, components, new HashSet<>());
            result.put(schemaName, typeCnts);
        }
        
        return result;
    }

    /**
     * 단일 스키마를 평탄화하여 타입별 개수를 구합니다.
     *
     * @param schemaName 스키마 이름
     * @param schema 스키마 객체
     * @param components Components 객체 ($ref 해결용)
     * @param visited 방문한 스키마 추적 (순환 참조 방지)
     * @return TypeCnts 객체
     */
    private TypeCnts flattenSchema(String schemaName, Schema schema, Components components, Set<String> visited) {
        TypeCnts typeCnts = new TypeCnts();
        Map<String, Integer> typeCounts = new HashMap<>();
        
        if (schema == null) {
            typeCnts.setTypeCounts(typeCounts);
            return typeCnts;
        }
        
        // 순환 참조 방지
        if (visited.contains(schemaName)) {
            log.debug("[CIRCULAR REF] 순환 참조 감지: {}", schemaName);
            typeCnts.setTypeCounts(typeCounts);
            return typeCnts;
        }
        
        visited.add(schemaName);
        
        // $ref 처리
        if (schema.getRef() != null) {
            String referencedSchemaName = extractSchemaNameFromRef(schema.getRef());
            if (referencedSchemaName != null) {
                Schema referencedSchema = getSchemaByName(referencedSchemaName, components);
                if (referencedSchema != null) {
                    TypeCnts refTypeCnts = flattenSchema(referencedSchemaName, referencedSchema, components, visited);
                    mergeTypeCounts(typeCounts, refTypeCnts.getTypeCounts());
                }
            }
            visited.remove(schemaName);
            typeCnts.setTypeCounts(typeCounts);
            return typeCnts;
        }
        
        // Properties 처리
        if (schema.getProperties() != null) {
            for (Map.Entry<String, Schema> propertyEntry : schema.getProperties().entrySet()) {
                String propertyName = propertyEntry.getKey();
                Schema propertySchema = propertyEntry.getValue();
                
                collectTypeCountsFromProperty(propertyName, propertySchema, components, typeCounts, visited);
            }
        }
        
        // Items 처리 (배열 타입)
        if (schema.getItems() != null) {
            collectTypeCountsFromProperty("items", schema.getItems(), components, typeCounts, visited);
        }
        
        visited.remove(schemaName);
        typeCnts.setTypeCounts(typeCounts);
        return typeCnts;
    }

    /**
     * Property에서 타입 카운트를 수집합니다.
     *
     * @param propertyName 필드명
     * @param propertySchema Property 스키마
     * @param components Components 객체
     * @param typeCounts 타입 카운트를 누적할 Map
     * @param visited 방문한 스키마 추적
     */
    private void collectTypeCountsFromProperty(String propertyName, Schema propertySchema, 
                                                Components components, Map<String, Integer> typeCounts,
                                                Set<String> visited) {
        if (propertySchema == null) {
            return;
        }
        
        // $ref 처리
        if (propertySchema.getRef() != null) {
            String referencedSchemaName = extractSchemaNameFromRef(propertySchema.getRef());
            if (referencedSchemaName != null) {
                Schema referencedSchema = getSchemaByName(referencedSchemaName, components);
                if (referencedSchema != null) {
                    TypeCnts refTypeCnts = flattenSchema(referencedSchemaName, referencedSchema, components, visited);
                    // 참조된 스키마의 모든 타입 카운트를 prefix 없이 직접 병합 (하위 객체의 필드가 상위 객체에 포함됨)
                    for (Map.Entry<String, Integer> entry : refTypeCnts.getTypeCounts().entrySet()) {
                        String key = entry.getKey(); // prefix 없이 그대로 사용
                        typeCounts.put(key, typeCounts.getOrDefault(key, 0) + entry.getValue());
                    }
                }
            }
            return;
        }
        
        // 배열 타입 처리 (재귀 없이 바로 저장)
        String type = propertySchema.getType();
        if (type != null && "array".equals(type) && propertySchema.getItems() != null) {
            Schema itemsSchema = propertySchema.getItems();
            
            // items가 $ref인 경우: "propertyName:array.SchemaName"
            if (itemsSchema.getRef() != null) {
                String referencedSchemaName = extractSchemaNameFromRef(itemsSchema.getRef());
                if (referencedSchemaName != null) {
                    String key = propertyName + ":array." + referencedSchemaName;
                    typeCounts.put(key, typeCounts.getOrDefault(key, 0) + 1);
                }
            } 
            // items가 기본 타입인 경우: "propertyName:array.type"
            else if (itemsSchema.getType() != null && isPrimitiveType(itemsSchema.getType())) {
                String format = itemsSchema.getFormat();
                String typeValue = format != null && "binary".equals(format) ? "binary" : itemsSchema.getType();
                String key = propertyName + ":array." + typeValue;
                typeCounts.put(key, typeCounts.getOrDefault(key, 0) + 1);
            }
            // 배열 처리는 여기서 종료 (재귀 호출하지 않음)
            return;
        }
        
        // 타입 확인 (기본 타입만 카운트, object/array는 properties/items 처리)
        if (type != null && isPrimitiveType(type)) {
            String format = propertySchema.getFormat();
            String typeValue = format != null && "binary".equals(format) ? "binary" : type;
            String key = propertyName + ":" + typeValue;
            typeCounts.put(key, typeCounts.getOrDefault(key, 0) + 1);
        }
        
        // Properties가 있으면 재귀적으로 처리 (object 타입)
        // 하위 객체의 필드는 prefix 없이 직접 포함되도록 nestedPropertyName만 사용
        if (propertySchema.getProperties() != null) {
            for (Map.Entry<String, Schema> nestedPropertyEntry : propertySchema.getProperties().entrySet()) {
                String nestedPropertyName = nestedPropertyEntry.getKey();
                Schema nestedPropertySchema = nestedPropertyEntry.getValue();
                collectTypeCountsFromProperty(nestedPropertyName, nestedPropertySchema, 
                                            components, typeCounts, visited);
            }
        }
    }

    /**
     * $ref에서 스키마 이름을 추출합니다.
     *
     * @param ref $ref 값 (예: "#/components/schemas/User")
     * @return 스키마 이름 (예: "User"), 또는 null
     */
    private String extractSchemaNameFromRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        return ref.substring("#/components/schemas/".length());
    }

    /**
     * Components에서 스키마 이름으로 스키마를 찾습니다.
     *
     * @param schemaName 스키마 이름
     * @param components Components 객체
     * @return Schema 객체, 없으면 null
     */
    private Schema getSchemaByName(String schemaName, Components components) {
        if (schemaName == null || components == null) {
            return null;
        }
        
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            return null;
        }
        
        return schemas.get(schemaName);
    }

    /**
     * 두 타입 카운트 맵을 병합합니다.
     *
     * @param target 누적할 대상 Map
     * @param source 병합할 소스 Map
     */
    private void mergeTypeCounts(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String key = entry.getKey();
            Integer count = entry.getValue();
            target.put(key, target.getOrDefault(key, 0) + count);
        }
    }

    /**
     * 주어진 타입이 기본형 타입인지 확인합니다.
     * <p>
     * object와 array는 기본형이 아니므로 false를 반환합니다.
     * 이들은 properties나 items를 처리해야 합니다.
     *
     * @param type 확인할 타입
     * @return 기본형 타입(string, integer, number, boolean)이면 true
     */
    private boolean isPrimitiveType(String type) {
        if (type == null) {
            return false;
        }
        return type.equals("string") || type.equals("integer") || type.equals("number") || 
               type.equals("boolean");
    }

    /**
     * 평탄화된 스키마들을 비교하여 일치 여부를 판단합니다.
     * <p>
     * baseSchemas를 기준으로 순회하면서, 각 스키마에 대해:
     * - targetSchemas에 해당 스키마가 없으면 false
     * - 있으면 두 TypeCnts의 typeCounts를 비교하여 모든 키의 개수가 같으면 true, 하나라도 다르면 false
     *
     * @param baseSchemas 기준이 되는 평탄화된 스키마 맵
     * @param targetSchemas 비교 대상 평탄화된 스키마 맵
     * @return 스키마 이름 -> 일치 여부 (true/false) 맵
     */
    public Map<String, Boolean> compareFlattenedSchemas(
            Map<String, TypeCnts> baseSchemas,
            Map<String, TypeCnts> targetSchemas) {
        Map<String, Boolean> results = new HashMap<>();
        
        if (baseSchemas == null || baseSchemas.isEmpty()) {
            return results;
        }
        
        for (Map.Entry<String, TypeCnts> entry : baseSchemas.entrySet()) {
            String schemaName = entry.getKey();
            TypeCnts baseTypeCnts = entry.getValue();
            
            // targetSchemas에 해당 스키마가 없으면 false
            if (targetSchemas == null || !targetSchemas.containsKey(schemaName)) {
                results.put(schemaName, false);
                continue;
            }
            
            TypeCnts targetTypeCnts = targetSchemas.get(schemaName);
            
            // 두 TypeCnts의 typeCounts를 비교
            Map<String, Integer> baseTypeCounts = baseTypeCnts != null ? baseTypeCnts.getTypeCounts() : new HashMap<>();
            Map<String, Integer> targetTypeCounts = targetTypeCnts != null ? targetTypeCnts.getTypeCounts() : new HashMap<>();
            
            // 모든 키를 포함하는 Set 생성
            Set<String> allKeys = new HashSet<>(baseTypeCounts.keySet());
            allKeys.addAll(targetTypeCounts.keySet());
            
            // 모든 키에 대해 개수가 같은지 확인
            boolean isSame = true;
            for (String key : allKeys) {
                int baseCount = baseTypeCounts.getOrDefault(key, 0);
                int targetCount = targetTypeCounts.getOrDefault(key, 0);
                
                if (baseCount != targetCount) {
                    isSame = false;
                    break;
                }
            }
            
            results.put(schemaName, isSame);
        }
        
        return results;
    }
}
