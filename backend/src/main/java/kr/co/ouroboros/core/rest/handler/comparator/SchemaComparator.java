package kr.co.ouroboros.core.rest.handler.comparator;

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

@Slf4j
@Component
public class SchemaComparator {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCnts {
        private Map<String, Integer> typeCounts = new HashMap<>();
    }

    /**
     * Compute flattened type-count representations for each schema defined in the provided Components.
     *
     * @param components the OpenAPI Components object containing schema definitions; may be null
     *                   or contain no schemas
     * @return a map from schema name to its flattened TypeCnts; empty if {@code components} is null
     *         or contains no schemas
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
     * Computes flattened type counts for the given schema by traversing its properties, array items, and any referenced schemas while preventing infinite recursion from circular references.
     *
     * @param schemaName the name identifying the schema within components; used for circular-reference detection
     * @param schema the schema to analyze; may be null
     * @param components the Components container used to resolve `$ref` references; may be null
     * @param visited a set of schema names already visited in the current traversal to detect and avoid circular references
     * @return a TypeCnts containing a map of flattened type-count entries; the map will be empty if the schema is null or references cannot be resolved
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
     * Accumulates flattened type counts for a property (including nested properties, arrays, and $ref targets)
     * into the provided map.
     *
     * Processes the property's schema as follows:
     * - If the schema is a $ref, merges the referenced schema's flattened counts directly into `typeCounts`.
     * - If the schema is an array, records a single count for the array element type or referenced schema using the key
     *   `propertyName:array.{TypeOrSchemaName}`.
     * - If the schema is a primitive type, increments the count for `propertyName:{type}` (uses `binary` when format is `binary`).
     * - If the schema has nested properties (object), recursively processes each nested property and merges their counts
     *   without adding a parent prefix.
     *
     * @param propertyName the name of the property being processed; used as the key prefix for recorded types
     * @param propertySchema the OpenAPI Schema object describing the property
     * @param components the Components container used to resolve schema $ref references
     * @param typeCounts the accumulating map of type keys to counts that will be updated in-place
     * @param visited set of schema names already visited to detect and avoid circular references during resolution
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
     * Extracts the schema name from a Components schema reference.
     *
     * @param ref the JSON Reference string, expected to start with "#/components/schemas/{SchemaName}"
     * @return the schema name portion when `ref` starts with "#/components/schemas/", `null` otherwise
     */
    private String extractSchemaNameFromRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        return ref.substring("#/components/schemas/".length());
    }

    /**
     * Retrieve a schema definition from the provided Components by its schema name.
     *
     * @param schemaName the key name of the schema within the Components' schemas map
     * @param components the Components container to search for the schema
     * @return the Schema associated with the given name, or `null` if the name, components, or schema map is null or the name is not present
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
     * Merge counts from the source map into the target map by summing values for matching keys.
     *
     * @param target the map that will be updated; for each key present in {@code source} its value is added to the existing value in {@code target} (or inserted if absent)
     * @param source the map supplying counts to add into {@code target}
     */
    private void mergeTypeCounts(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String key = entry.getKey();
            Integer count = entry.getValue();
            target.put(key, target.getOrDefault(key, 0) + count);
        }
    }

    /**
     * Determines whether the given type name represents a primitive OpenAPI/Swagger type.
     *
     * @param type the type name to check (for example, "string", "integer", "number", or "boolean")
     * @return `true` if the type is one of "string", "integer", "number", or "boolean", `false` otherwise
     */
    private boolean isPrimitiveType(String type) {
        if (type == null) {
            return false;
        }
        return type.equals("string") || type.equals("integer") || type.equals("number") || 
               type.equals("boolean");
    }

    /**
     * Compare flattened type-count representations of schemas from a base set against a target set.
     *
     * For each schema present in {@code baseSchemas}, determines whether the corresponding {@code TypeCnts}
     * in {@code targetSchemas} has identical {@code typeCounts}. If a schema from {@code baseSchemas} is
     * missing in {@code targetSchemas}, it is considered not equal.
     *
     * @param baseSchemas   map of schema names to their flattened type counts (base reference)
     * @param targetSchemas map of schema names to their flattened type counts (target to compare), may be {@code null}
     * @return              a map from each schema name in {@code baseSchemas} to `true` if the target has the same
     *                      type-counts for that schema, `false` otherwise
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
