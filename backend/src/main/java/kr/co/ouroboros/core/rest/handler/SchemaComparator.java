package kr.co.ouroboros.core.rest.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import kr.co.ouroboros.core.rest.common.dto.Components;
import kr.co.ouroboros.core.rest.common.dto.Schema;
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
     * Compare the components.schemas of two OpenAPI Components and report per-schema match results.
     * <p>
     * 파일 스펙과 스캔 스펙 각각을 기준으로 모든 스키마를 비교하여 두 가지 결과 맵을 반환합니다.
     *
     * @param scannedComponents the scanned (baseline) document's Components to compare from
     * @param fileComponents    the file-based document's Components to compare against
     * @return SchemaComparisonResults containing file-based and scan-based comparison results
     */
    /**
     * 비교 결과와 타입 카운트를 함께 저장하는 캐시 구조
     */
    private static class ComparisonCache {
        // 비교 결과 캐시: "schemaName:baseSpec:targetSpec" 형식 (정렬되어 양방향 재사용 가능)
        private final Map<String, SchemaComparisonResult> comparisonCache = new HashMap<>();
        
        // 타입 카운트 캐시: "schemaName:specName" 형식 (스펙별로 구분)
        private final Map<String, Map<String, Integer>> typeCountCache = new HashMap<>();
        
        /**
         * 양방향 재사용을 위해 키를 정렬합니다.
         * "schemaName:file:scan"과 "schemaName:scan:file"을 같은 키로 처리합니다.
         */
        String makeComparisonKey(String schemaName, String baseSpec, String targetSpec) {
            // 스펙 이름을 정렬하여 양방향 재사용 가능하게 함
            String[] specs = {baseSpec, targetSpec};
            java.util.Arrays.sort(specs);
            return schemaName + ":" + specs[0] + ":" + specs[1];
        }
        
        /**
         * 타입 카운트 키 생성: "schemaName:specName" 형식
         */
        String makeTypeCountKey(String schemaName, String specName) {
            return schemaName + ":" + specName;
        }
        
        SchemaComparisonResult getComparison(String schemaName, String baseSpec, String targetSpec) {
            return comparisonCache.get(makeComparisonKey(schemaName, baseSpec, targetSpec));
        }
        
        void putComparison(String schemaName, String baseSpec, String targetSpec, SchemaComparisonResult result) {
            comparisonCache.put(makeComparisonKey(schemaName, baseSpec, targetSpec), result);
        }
        
        /**
         * 특정 스펙의 스키마 타입 카운트를 가져옵니다.
         * 파일 User와 스캔 User는 다를 수 있으므로 스펙별로 구분하여 저장/조회합니다.
         */
        Map<String, Integer> getTypeCounts(String schemaName, String specName) {
            return typeCountCache.get(makeTypeCountKey(schemaName, specName));
        }
        
        /**
         * 특정 스펙의 스키마 타입 카운트를 저장합니다.
         */
        void putTypeCounts(String schemaName, String specName, Map<String, Integer> typeCounts) {
            typeCountCache.put(makeTypeCountKey(schemaName, specName), new HashMap<>(typeCounts));
        }
        
    }

    public SchemaComparisonResults compareSchemas(Components scannedComponents, Components fileComponents) {
        // Map의 키는 스키마 객체의 이름을 의미
        Map<String, Schema> scannedSchemas = scannedComponents != null ? scannedComponents.getSchemas() : null;
        Map<String, Schema> fileSchemas = fileComponents != null ? fileComponents.getSchemas() : null;

        // 둘 다 null이거나 비어있을 때만 바로 return
        boolean scannedEmpty = scannedSchemas == null || scannedSchemas.isEmpty();
        boolean fileEmpty = fileSchemas == null || fileSchemas.isEmpty();
        
        if (scannedEmpty && fileEmpty) {
            log.debug("[SCHEMA COMPARISON] 스키마가 모두 없습니다.");
            return new SchemaComparisonResults(new HashMap<>(), new HashMap<>());
        }

        log.debug("[SCHEMA COMPARISON] 스키마 비교 시작...");
        log.debug("스캔된 스키마: {}", scannedSchemas != null ? scannedSchemas.keySet() : "없음");
        log.debug("파일 스키마: {}", fileSchemas != null ? fileSchemas.keySet() : "없음");

        // 공유 캐시 생성 (파일 기준 실행 시 스캔 스펙도 같이 검사하면서 결과 저장)
        ComparisonCache sharedCache = new ComparisonCache();

        // 파일 스펙 기준으로 모든 스키마 처리 (fileSchemas가 있으면 처리)
        Map<String, SchemaComparisonResult> fileResults = compareSchemasByBase(
                fileSchemas, fileComponents, scannedSchemas, scannedComponents, 
                "file", "scan", sharedCache);

        // 스캔 스펙 기준으로 모든 스키마 처리 (scannedSchemas가 있으면 처리)
        // 캐시된 결과가 있으면 재사용
        Map<String, SchemaComparisonResult> scanResults = compareSchemasByBase(
                scannedSchemas, scannedComponents, fileSchemas, fileComponents,
                "scan", "file", sharedCache);

        log.debug("[SCHEMA COMPARISON] 스키마 비교 완료. 파일 결과: {}, 스캔 결과: {}", 
                fileResults.size(), scanResults.size());
        
        return new SchemaComparisonResults(fileResults, scanResults);
    }

    /**
     * 기준 스펙의 모든 스키마를 대상 스펙과 비교합니다.
     * <p>
     * 한 번의 재귀 탐색으로 비교 결과와 타입 카운트를 모두 구하며, 캐시를 활용하여 중복 계산을 방지합니다.
     *
     * @param baseSchemas 기준 스펙의 스키마 맵
     * @param baseComponents 기준 스펙의 Components (타입 카운트 계산용)
     * @param targetSchemas 대상 스펙의 스키마 맵
     * @param targetComponents 대상 스펙의 Components (비교용)
     * @param baseSpecName 기준 스펙 이름 ("file" 또는 "scan")
     * @param targetSpecName 대상 스펙 이름 ("file" 또는 "scan")
     * @param sharedCache 공유 캐시 (파일 기준 실행 시 스캔 스펙도 같이 검사하면서 결과 저장)
     * @return 기준 스펙의 모든 스키마에 대한 비교 결과
     */
    private Map<String, SchemaComparisonResult> compareSchemasByBase(
            Map<String, Schema> baseSchemas, Components baseComponents,
            Map<String, Schema> targetSchemas, Components targetComponents,
            String baseSpecName, String targetSpecName, ComparisonCache sharedCache) {
        
        Map<String, SchemaComparisonResult> results = new HashMap<>();
        
        // baseComponents가 null이면 타입 카운트를 계산할 수 없으므로 빈 결과 반환
        if (baseComponents == null) {
            return results;
        }

        // 기준 스펙의 스키마가 없으면 빈 결과 반환
        if (baseSchemas == null || baseSchemas.isEmpty()) {
            return results;
        }

        // 기준 스펙의 각 스키마에 대해 대상 스펙과 비교
        for (Map.Entry<String, Schema> baseEntry : baseSchemas.entrySet()) {
            String schemaName = baseEntry.getKey();
            Schema baseSchema = baseEntry.getValue();
            Schema targetSchema = targetSchemas != null ? targetSchemas.get(schemaName) : null;

            // 캐시 확인 (이전에 계산된 결과가 있는지 확인)
            SchemaComparisonResult cachedResult = sharedCache.getComparison(schemaName, baseSpecName, targetSpecName);
            if (cachedResult != null) {
                log.debug("[CACHE HIT] '{}': 캐시에서 결과를 가져옵니다.", schemaName);
                results.put(schemaName, cachedResult);
                continue;
            }

            // 기준 스펙에는 있지만 대상 스펙에는 없는 경우
            if (targetSchema == null) {
                log.debug("[SCHEMA MISSING] '{}': 기준 스펙에는 있지만 대상 스펙에는 없습니다.", schemaName);
                // 기준 스키마만 타입 카운트 계산 (isSame = false)
                // 한 번의 재귀 탐색으로 타입 카운트 계산
                Map<String, Integer> baseTypeCounts = calculateTypeCounts(schemaName, baseSchema, baseComponents, baseSpecName, sharedCache);
                SchemaComparisonResult result = new SchemaComparisonResult(false, baseTypeCounts);
                results.put(schemaName, result);
                // 캐시에 저장
                sharedCache.putComparison(schemaName, baseSpecName, targetSpecName, result);
                continue;
            }

            // 둘 다 존재하는 경우, 한 번의 재귀 탐색으로 비교 결과와 타입 카운트를 모두 구함
            SchemaComparisonResult result = compareAndCountInOnePass(
                    schemaName, baseSchema, baseComponents, targetSchema, targetComponents, 
                    baseSpecName, targetSpecName, sharedCache);
            
            results.put(schemaName, result);
            
            // 캐시에 저장 (다음에 반대 방향으로 비교할 때 재사용)
            sharedCache.putComparison(schemaName, baseSpecName, targetSpecName, result);

            if (result.isSame()) {
                log.debug("[SCHEMA MATCH] '{}': 스키마가 일치합니다.", schemaName);
            } else {
                log.debug("[SCHEMA MISMATCH] '{}': 스키마가 일치하지 않습니다.", schemaName);
            }
        }

        return results;
    }

    /**
     * $ref 비교 없이 스키마의 실제 내용만 비교합니다.
     * <p>
     * properties, items, additionalProperties 등을 비교하지만 $ref는 비교하지 않습니다.
     *
     * @param scannedSchema the scanned schema to compare from
     * @param fileSchema    the file-based schema to compare against
     * @param schemaName    the schema name used for logging/context
     * @return true if the schemas' contents match (excluding $ref), false otherwise
     */
    private boolean compareSchemaDetailsWithoutRef(Schema scannedSchema, Schema fileSchema, String schemaName) {
        if (scannedSchema == null && fileSchema == null) {
            return true;
        }
        if (scannedSchema == null || fileSchema == null) {
            return false;
        }

        // type 비교
        if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
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
            if (!compareSchemaDetailsWithoutRef(scannedSchema.getItems(), fileSchema.getItems(), schemaName + ".items")) {
                return false;
            }
        }

        // AdditionalProperties 비교
        if (!Objects.equals(scannedSchema.getAdditionalProperties(), fileSchema.getAdditionalProperties())) {
            return false;
        }

        return true;
    }

    /**
     * Components에서 스키마 이름으로 스키마를 찾습니다.
     *
     * @param schemaName 찾을 스키마 이름
     * @param components Components 객체
     * @return 찾은 Schema 객체, 없으면 null
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
     * 비교 결과와 타입 카운트를 함께 저장하는 내부 클래스
     */
    private static class CompareAndCountResult {
        boolean isMatch;
        Map<String, Integer> typeCounts;
        
        CompareAndCountResult(boolean isMatch, Map<String, Integer> typeCounts) {
            this.isMatch = isMatch;
            this.typeCounts = typeCounts;
        }
    }

    /**
     * 한 번의 재귀 탐색으로 비교 결과와 타입 카운트를 모두 구합니다.
     * <p>
     * 스키마를 한 번만 재귀적으로 탐색하면서 비교 결과와 타입 카운트를 동시에 계산합니다.
     * 파일 기준으로 순회할 때, 파일 스키마와 스캔 스키마를 모두 탐색하므로 둘의 타입 카운트를 모두 계산합니다.
     *
     * @param schemaName 스키마 이름
     * @param baseSchema 기준 스펙의 스키마
     * @param baseComponents 기준 스펙의 Components
     * @param targetSchema 대상 스펙의 스키마
     * @param targetComponents 대상 스펙의 Components
     * @param baseSpecName 기준 스펙 이름
     * @param targetSpecName 대상 스펙 이름
     * @param sharedCache 공유 비교 결과 캐시
     * @return 비교 결과와 타입 카운트를 포함한 SchemaComparisonResult (기준 스펙의 타입 카운트)
     */
    private SchemaComparisonResult compareAndCountInOnePass(
            String schemaName, Schema baseSchema, Components baseComponents,
            Schema targetSchema, Components targetComponents,
            String baseSpecName, String targetSpecName, ComparisonCache sharedCache) {
        
        // 한 번의 재귀 탐색으로 비교 결과와 타입 카운트를 모두 구함
        // 기준 스펙과 대상 스펙의 타입 카운트를 모두 계산하여 캐시에 저장
        CompareAndCountResult result = compareAndCountRecursive(
                schemaName, baseSchema, baseComponents, targetSchema, targetComponents,
                baseSpecName, targetSpecName, sharedCache, new HashSet<>());
        
        return new SchemaComparisonResult(result.isMatch, result.typeCounts);
    }

    /**
     * 재귀적으로 스키마를 탐색하면서 비교 결과와 타입 카운트를 동시에 계산합니다.
     * <p>
     * 파일 기준으로 순회할 때, 파일 스키마와 스캔 스키마를 모두 탐색하므로 둘의 타입 카운트를 모두 계산하여 캐시에 저장합니다.
     *
     * @param schemaName 현재 스키마 이름 (경로)
     * @param baseSchema 기준 스펙의 스키마
     * @param baseComponents 기준 스펙의 Components
     * @param targetSchema 대상 스펙의 스키마
     * @param targetComponents 대상 스펙의 Components
     * @param baseSpecName 기준 스펙 이름
     * @param targetSpecName 대상 스펙 이름
     * @param sharedCache 공유 비교 결과 캐시
     * @param comparingVisited 비교 중인 스키마 추적 (순환 참조 방지)
     * @return 비교 결과와 타입 카운트 (기준 스펙의 타입 카운트)
     */
    private CompareAndCountResult compareAndCountRecursive(
            String schemaName, Schema baseSchema, Components baseComponents,
            Schema targetSchema, Components targetComponents,
            String baseSpecName, String targetSpecName, ComparisonCache sharedCache, Set<String> comparingVisited) {
        
        if (baseSchema == null && targetSchema == null) {
            return new CompareAndCountResult(true, new HashMap<>());
        }
        if (baseSchema == null || targetSchema == null) {
            // 타입 카운트는 baseSchema 기준으로 계산 (캐시 확인)
            Map<String, Integer> typeCounts;
            if (baseSchema != null && schemaName != null) {
                Map<String, Integer> cached = sharedCache.getTypeCounts(schemaName, baseSpecName);
                if (cached != null) {
                    typeCounts = new HashMap<>(cached);
                } else {
                    typeCounts = collectTypeCountsFromSchema(baseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
                }
            } else {
                typeCounts = new HashMap<>();
            }
            return new CompareAndCountResult(false, typeCounts);
        }

        // type 비교
        if (!Objects.equals(baseSchema.getType(), targetSchema.getType())) {
            // 타입 카운트는 baseSchema 기준으로 계산 (캐시 확인)
            Map<String, Integer> typeCounts;
            if (schemaName != null) {
                Map<String, Integer> cached = sharedCache.getTypeCounts(schemaName, baseSpecName);
                if (cached != null) {
                    typeCounts = new HashMap<>(cached);
                } else {
                    typeCounts = collectTypeCountsFromSchema(baseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
                }
            } else {
                typeCounts = collectTypeCountsFromSchema(baseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
            }
            return new CompareAndCountResult(false, typeCounts);
        }

        // $ref 비교 및 처리
        String baseRef = baseSchema.getRef();
        String targetRef = targetSchema.getRef();
        
        if (!Objects.equals(baseRef, targetRef)) {
            Map<String, Integer> typeCounts = collectTypeCountsFromSchema(baseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
            return new CompareAndCountResult(false, typeCounts);
        }
        
        // $ref가 같으면 참조된 스키마의 실제 내용도 비교
        if (baseRef != null && targetRef != null) {
            String referencedSchemaName = extractSchemaNameFromRef(baseRef);
            if (referencedSchemaName != null) {
                // 순환 참조 방지
                String visitKey = schemaName + "->" + referencedSchemaName;
                if (comparingVisited.contains(visitKey)) {
                    // 순환 참조는 일치로 간주하고 타입 카운트는 캐시에서 가져옴 (기준 스펙 기준)
                    Map<String, Integer> typeCounts = sharedCache.getTypeCounts(referencedSchemaName, baseSpecName);
                    if (typeCounts == null) {
                        typeCounts = new HashMap<>();
                    } else {
                        typeCounts = new HashMap<>(typeCounts);
                    }
                    return new CompareAndCountResult(true, typeCounts);
                }
                
                comparingVisited.add(visitKey);
                
                Schema referencedBaseSchema = getSchemaByName(referencedSchemaName, baseComponents);
                Schema referencedTargetSchema = getSchemaByName(referencedSchemaName, targetComponents);
                
                if (referencedBaseSchema == null || referencedTargetSchema == null) {
                    comparingVisited.remove(visitKey);
                    Map<String, Integer> typeCounts = referencedBaseSchema != null
                            ? collectTypeCountsFromSchema(referencedBaseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>())
                            : new HashMap<>();
                    return new CompareAndCountResult(false, typeCounts);
                }
                
                // 참조된 스키마 비교 (재귀 호출, $ref는 제외)
                // 파일 기준으로 순회할 때, 파일 스키마와 스캔 스키마를 모두 탐색하므로 둘의 타입 카운트를 모두 계산
                CompareAndCountResult refResult = compareAndCountRecursiveWithoutRef(
                        referencedSchemaName, referencedBaseSchema, baseComponents, referencedTargetSchema, targetComponents,
                        baseSpecName, targetSpecName, sharedCache, comparingVisited);
                
                comparingVisited.remove(visitKey);
                return refResult;
            }
        }

        // Properties 비교 및 타입 카운트 수집
        // 파일 기준으로 순회할 때, 파일 스키마와 스캔 스키마를 모두 탐색하므로 둘의 타입 카운트를 모두 계산
        Map<String, Integer> typeCounts = new HashMap<>();
        boolean propertiesMatch = true;
        
        if (baseSchema.getProperties() != null || targetSchema.getProperties() != null) {
            CompareAndCountResult propertiesResult = comparePropertiesAndCount(
                    baseSchema.getProperties(), baseComponents,
                    targetSchema.getProperties(), targetComponents,
                    schemaName, baseSpecName, targetSpecName, sharedCache, comparingVisited);
            propertiesMatch = propertiesResult.isMatch;
            mergeTypeCounts(typeCounts, propertiesResult.typeCounts);
        }

        // Items 비교 및 타입 카운트 수집
        boolean itemsMatch = true;
        if (baseSchema.getItems() != null || targetSchema.getItems() != null) {
            CompareAndCountResult itemsResult = compareAndCountRecursive(
                    schemaName + ".items", baseSchema.getItems(), baseComponents,
                    targetSchema.getItems(), targetComponents,
                    baseSpecName, targetSpecName, sharedCache, comparingVisited);
            itemsMatch = itemsResult.isMatch;
            mergeTypeCounts(typeCounts, itemsResult.typeCounts);
        }

        // AdditionalProperties 비교
        boolean additionalPropsMatch = Objects.equals(baseSchema.getAdditionalProperties(), targetSchema.getAdditionalProperties());

        // 기본 타입인 경우 타입 카운트 추가
        String type = baseSchema.getType();
        if (type != null && isActualPrimitiveType(type)) {
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        boolean isMatch = propertiesMatch && itemsMatch && additionalPropsMatch;
        return new CompareAndCountResult(isMatch, typeCounts);
    }

    /**
     * $ref 비교 없이 스키마의 실제 내용만 비교하면서 타입 카운트를 수집합니다.
     * <p>
     * $ref가 있으면 참조된 스키마의 타입 카운트를 가져와서 합산합니다.
     */
    private CompareAndCountResult compareAndCountRecursiveWithoutRef(
            String schemaName, Schema baseSchema, Components baseComponents,
            Schema targetSchema, Components targetComponents,
            String baseSpecName, String targetSpecName, ComparisonCache sharedCache, Set<String> comparingVisited) {
        
        if (baseSchema == null && targetSchema == null) {
            return new CompareAndCountResult(true, new HashMap<>());
        }
        if (baseSchema == null || targetSchema == null) {
            Map<String, Integer> typeCounts = baseSchema != null
                    ? collectTypeCountsFromSchema(baseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>())
                    : new HashMap<>();
            return new CompareAndCountResult(false, typeCounts);
        }

        // $ref가 있는 경우: 참조된 스키마의 타입 카운트를 가져와서 합산
        String baseRef = baseSchema.getRef();
        String targetRef = targetSchema.getRef();
        
        if (baseRef != null || targetRef != null) {
            // $ref가 있으면 참조된 스키마의 타입 카운트를 가져옴
            Map<String, Integer> typeCounts = new HashMap<>();
            boolean refMatch = Objects.equals(baseRef, targetRef);
            
            if (baseRef != null) {
                String referencedSchemaName = extractSchemaNameFromRef(baseRef);
                if (referencedSchemaName != null) {
                    Map<String, Integer> refTypeCounts = sharedCache.getTypeCounts(referencedSchemaName, baseSpecName);
                    if (refTypeCounts == null) {
                        // 캐시에 없으면 계산
                        Schema referencedSchema = getSchemaByName(referencedSchemaName, baseComponents);
                        if (referencedSchema != null) {
                            refTypeCounts = collectTypeCountsFromSchema(referencedSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
                        } else {
                            refTypeCounts = new HashMap<>();
                        }
                    }
                    mergeTypeCounts(typeCounts, refTypeCounts);
                }
            }
            
            if (targetRef != null && !Objects.equals(baseRef, targetRef)) {
                String referencedSchemaName = extractSchemaNameFromRef(targetRef);
                if (referencedSchemaName != null) {
                    Map<String, Integer> refTypeCounts = sharedCache.getTypeCounts(referencedSchemaName, targetSpecName);
                    if (refTypeCounts == null) {
                        // 캐시에 없으면 계산
                        Schema referencedSchema = getSchemaByName(referencedSchemaName, targetComponents);
                        if (referencedSchema != null) {
                            refTypeCounts = collectTypeCountsFromSchema(referencedSchema, targetComponents, targetSpecName, sharedCache, new HashSet<>());
                        } else {
                            refTypeCounts = new HashMap<>();
                        }
                    }
                    mergeTypeCounts(typeCounts, refTypeCounts);
                }
            }
            
            // $ref만 있고 properties가 없으면 여기서 반환
            if ((baseSchema.getProperties() == null || baseSchema.getProperties().isEmpty()) &&
                (targetSchema.getProperties() == null || targetSchema.getProperties().isEmpty()) &&
                baseSchema.getItems() == null && targetSchema.getItems() == null) {
                return new CompareAndCountResult(refMatch, typeCounts);
            }
            
            // $ref와 properties가 함께 있는 경우, properties의 타입 카운트도 합산
            // (일반적으로는 $ref가 있으면 properties가 없어야 하지만, 혹시 모를 경우를 대비)
        }

        // type 비교
        if (!Objects.equals(baseSchema.getType(), targetSchema.getType())) {
            Map<String, Integer> typeCounts = collectTypeCountsFromSchema(baseSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
            return new CompareAndCountResult(false, typeCounts);
        }

        Map<String, Integer> typeCounts = new HashMap<>();
        boolean propertiesMatch = true;
        
        if (baseSchema.getProperties() != null || targetSchema.getProperties() != null) {
            CompareAndCountResult propertiesResult = comparePropertiesAndCount(
                    baseSchema.getProperties(), baseComponents,
                    targetSchema.getProperties(), targetComponents,
                    schemaName, baseSpecName, targetSpecName, sharedCache, comparingVisited);
            propertiesMatch = propertiesResult.isMatch;
            mergeTypeCounts(typeCounts, propertiesResult.typeCounts);
        }

        boolean itemsMatch = true;
        if (baseSchema.getItems() != null || targetSchema.getItems() != null) {
            CompareAndCountResult itemsResult = compareAndCountRecursiveWithoutRef(
                    schemaName + ".items", baseSchema.getItems(), baseComponents,
                    targetSchema.getItems(), targetComponents,
                    baseSpecName, targetSpecName, sharedCache, comparingVisited);
            itemsMatch = itemsResult.isMatch;
            mergeTypeCounts(typeCounts, itemsResult.typeCounts);
        }

        boolean additionalPropsMatch = Objects.equals(baseSchema.getAdditionalProperties(), targetSchema.getAdditionalProperties());

        String type = baseSchema.getType();
        if (type != null && isActualPrimitiveType(type)) {
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        boolean isMatch = propertiesMatch && itemsMatch && additionalPropsMatch;
        return new CompareAndCountResult(isMatch, typeCounts);
    }

    /**
     * Properties를 비교하면서 타입 카운트를 수집합니다.
     */
    private CompareAndCountResult comparePropertiesAndCount(
            Map<String, Schema> baseProperties, Components baseComponents,
            Map<String, Schema> targetProperties, Components targetComponents,
            String schemaName, String baseSpecName, String targetSpecName, ComparisonCache sharedCache, Set<String> comparingVisited) {
        
        if (baseProperties == null && targetProperties == null) {
            return new CompareAndCountResult(true, new HashMap<>());
        }
        if (baseProperties == null || targetProperties == null) {
            Map<String, Integer> typeCounts = new HashMap<>();
            if (baseProperties != null) {
                for (Schema propSchema : baseProperties.values()) {
                    Map<String, Integer> propCounts = collectTypeCountsFromSchema(propSchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
                    mergeTypeCounts(typeCounts, propCounts);
                }
            }
            return new CompareAndCountResult(false, typeCounts);
        }

        Map<String, Integer> typeCounts = new HashMap<>();
        boolean allMatch = true;

        // baseProperties의 각 Property에 대해 비교
        for (Map.Entry<String, Schema> baseEntry : baseProperties.entrySet()) {
            String propertyName = baseEntry.getKey();
            Schema basePropertySchema = baseEntry.getValue();
            Schema targetPropertySchema = targetProperties.get(propertyName);

            if (targetPropertySchema == null) {
                allMatch = false;
                Map<String, Integer> propCounts = collectTypeCountsFromSchema(basePropertySchema, baseComponents, baseSpecName, sharedCache, new HashSet<>());
                mergeTypeCounts(typeCounts, propCounts);
                continue;
            }

            CompareAndCountResult propResult = compareAndCountRecursiveWithoutRef(
                    schemaName + "." + propertyName, basePropertySchema, baseComponents,
                    targetPropertySchema, targetComponents,
                    baseSpecName, targetSpecName, sharedCache, comparingVisited);
            
            if (!propResult.isMatch) {
                allMatch = false;
            }
            mergeTypeCounts(typeCounts, propResult.typeCounts);
        }

        // targetProperties에만 있는 Property 확인
        for (String targetPropertyName : targetProperties.keySet()) {
            if (!baseProperties.containsKey(targetPropertyName)) {
                allMatch = false;
                Schema targetPropertySchema = targetProperties.get(targetPropertyName);
                Map<String, Integer> propCounts = collectTypeCountsFromSchema(targetPropertySchema, targetComponents, targetSpecName, sharedCache, new HashSet<>());
                mergeTypeCounts(typeCounts, propCounts);
            }
        }

        return new CompareAndCountResult(allMatch, typeCounts);
    }

    /**
     * 스키마에서 타입 카운트만 수집합니다 (비교 없이).
     */
    private Map<String, Integer> collectTypeCountsFromSchema(
            Schema schema, Components components,
            String specName, ComparisonCache sharedCache,
            Set<String> visited) {
        
        Map<String, Integer> typeCounts = new HashMap<>();
        collectPrimitiveTypesFromSchema(schema, components, typeCounts, visited, specName, sharedCache);
        return typeCounts;
    }

    /**
         * Determine whether two schema property maps are equivalent.
         *
         * Compares the key sets and recursively compares each property's Schema; records a mismatch if any property is missing, extra, or differs.
         *
         * @param scannedProperties the properties from the scanned document (baseline)
         * @param fileProperties    the properties from the file-based document to compare against
         * @param schemaName        schema name used as context for property path construction (used in logging)
         * @return {@code true} if both property maps contain the same keys and corresponding property schemas match, {@code false} otherwise
         */
    private boolean compareSchemaProperties(Map<String, Schema> scannedProperties, Map<String, Schema> fileProperties, String schemaName) {
        if (scannedProperties == null && fileProperties == null) {
            return true;
        }
        if (scannedProperties == null || fileProperties == null) {
            log.debug("[PROPERTIES NULL MISMATCH] '{}': 한쪽 Properties가 null입니다.", schemaName);
            return false;
        }

        // 스캔된 문서의 각 Property에 대해 비교
        for (Map.Entry<String, Schema> scannedEntry : scannedProperties.entrySet()) {
            String propertyName = scannedEntry.getKey(); // 필드명을 의미
            Schema scannedPropertySchema = scannedEntry.getValue();
            Schema filePropertySchema = fileProperties.get(propertyName);

            // 필드명이 매칭되는게 없는 경우
            if (filePropertySchema == null) {
                log.debug("[PROPERTY MISSING] '{}.{}': Property가 파일 스펙에 없습니다.",
                        schemaName, propertyName);
                return false;
            }

            // 재귀 검사 (Components 정보는 전달하지 않음 - 현재 스키마의 properties 내부이므로)
            // properties 내부의 $ref는 이미 상위에서 처리됨
            if (!compareSchemaDetailsWithoutRef(scannedPropertySchema, filePropertySchema, schemaName + "." + propertyName)) {
                return false;
            }
        }

        // 파일에만 있는 Property들 확인
        for (String filePropertyName : fileProperties.keySet()) {
            if (!scannedProperties.containsKey(filePropertyName)) {
                log.debug("[PROPERTY EXTRA] '{}.{}': 스캔된 문서에 없는 추가 Property입니다.",
                        schemaName, filePropertyName);
                return false;
            }
        }

        return true;
    }

    /**
     * 스키마 이름에서 실제 스키마 이름을 추출합니다.
     *
     * @param ref 스키마 참조 주소 (예: "#/components/schemas/User")
     * @return 스키마 이름 (예: "User"), 또는 null if ref가 유효하지 않은 형식
     */
    private String extractSchemaNameFromRef(String ref) {
        if (ref == null || !ref.startsWith("#/components/schemas/")) {
            return null;
        }
        return ref.substring("#/components/schemas/".length());
    }

    /**
     * 스키마를 재귀적으로 탐색하여 기본형 타입만 카운트합니다.
     * <p>
     * object 타입이나 array 타입 자체는 카운트하지 않고, 내부의 기본형만 카운트합니다.
     * 중복 계산을 방지하기 위해 캐시를 사용합니다.
     *
     * @param schemaName 스키마 이름 (캐싱을 위해 필요)
     * @param schema 계산할 스키마
     * @param components Components 객체 (스키마 참조 해결용)
     * @param sharedCache 공유 비교 결과 캐시
     * @return 기본형 타입별 개수
     */
    private Map<String, Integer> calculateTypeCounts(String schemaName, Schema schema, Components components, String specName, ComparisonCache sharedCache) {
        // 캐시에 이미 계산된 결과가 있으면 사용
        if (schemaName != null) {
            Map<String, Integer> cached = sharedCache.getTypeCounts(schemaName, specName);
            if (cached != null) {
                log.debug("[TYPE COUNT] 캐시에서 '{}' 스키마의 타입 카운트를 가져옵니다 ({}).", schemaName, specName);
                return new HashMap<>(cached);
            }
        }
        
        Map<String, Integer> typeCounts = new HashMap<>();
        
        if (schema == null || components == null) {
            return typeCounts;
        }

        // 순환 참조 방지를 위한 visited Set
        Set<String> visited = new HashSet<>();
        
        // 스키마에서 타입 카운트 수집
        collectPrimitiveTypesFromSchema(schema, components, typeCounts, visited, specName, sharedCache);
        
        // 최상위 스키마의 경우 캐시에 저장
        if (schemaName != null) {
            sharedCache.putTypeCounts(schemaName, specName, typeCounts);
            log.debug("[TYPE COUNT] '{}' 스키마의 타입 카운트를 캐시에 저장했습니다 ({}): {}", schemaName, specName, typeCounts);
        }
        
        return typeCounts;
    }

    /**
     * Schema 객체를 재귀적으로 탐색하여 기본형 타입만 수집합니다.
     * <p>
     * object나 array 타입 자체는 카운트하지 않고, 내부의 기본형만 카운트합니다.
     *
     * @param schema 탐색할 Schema 객체
     * @param components Components 객체 (스키마 참조 해결용)
     * @param typeCounts 기본형 타입별 개수를 저장할 Map (누적)
     * @param visited 이미 방문한 스키마 이름 Set (순환 참조 방지)
     * @param specName 스펙 이름 (캐시 저장/조회용)
     * @param sharedCache 공유 비교 결과 캐시
     */
    private void collectPrimitiveTypesFromSchema(Schema schema, Components components, Map<String, Integer> typeCounts, 
                                                 Set<String> visited, String specName, ComparisonCache sharedCache) {
        if (schema == null) {
            return;
        }

        // $ref가 있는 경우: 참조된 스키마로 이동
        if (schema.getRef() != null) {
            String referencedSchemaName = extractSchemaNameFromRef(schema.getRef());
            if (referencedSchemaName != null) {
                // 캐시에 이미 계산된 결과가 있으면 사용
                Map<String, Integer> cached = sharedCache.getTypeCounts(referencedSchemaName, specName);
                if (cached != null) {
                    mergeTypeCounts(typeCounts, cached);
                    return;
                }
                
                // 캐시에 없으면 재귀적으로 계산
                collectPrimitiveTypesFromReferencedSchema(referencedSchemaName, components, typeCounts, visited, specName, sharedCache);
            }
            return;
        }

        // 기본 타입인 경우: 카운트 증가
        String type = schema.getType();
        if (type != null && isActualPrimitiveType(type)) {
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            log.debug("[TYPE COUNT] 기본 타입 발견: {} (현재 개수: {})", type, typeCounts.get(type));
        }

        // object 타입인 경우: properties 재귀 탐색 (object 자체는 카운트하지 않음)
        if ("object".equals(type) && schema.getProperties() != null) {
            for (Map.Entry<String, Schema> propertyEntry : schema.getProperties().entrySet()) {
                Schema propertySchema = propertyEntry.getValue();
                collectPrimitiveTypesFromSchema(propertySchema, components, typeCounts, visited, specName, sharedCache);
            }
        }

        // array 타입인 경우: items 재귀 탐색 (array 자체는 카운트하지 않음)
        if ("array".equals(type) && schema.getItems() != null) {
            collectPrimitiveTypesFromSchema(schema.getItems(), components, typeCounts, visited, specName, sharedCache);
        }
    }

    /**
     * 참조된 스키마를 재귀적으로 탐색하여 기본형 타입을 수집합니다.
     * <p>
     * 순환 참조를 방지하고, 계산 결과를 캐시에 저장합니다.
     *
     * @param schemaName 현재 탐색 중인 스키마 이름
     * @param components Components 객체
     * @param typeCounts 기본형 타입별 개수를 저장할 Map (누적)
     * @param visited 이미 방문한 스키마 이름 Set (순환 참조 방지)
     * @param specName 스펙 이름 (캐시 저장/조회용)
     * @param sharedCache 공유 비교 결과 캐시
     */
    private void collectPrimitiveTypesFromReferencedSchema(String schemaName, Components components, 
                                                           Map<String, Integer> typeCounts, Set<String> visited,
                                                           String specName, ComparisonCache sharedCache) {
        // 순환 참조 방지
        if (visited.contains(schemaName)) {
            log.debug("[TYPE COUNT] 순환 참조 감지: {}", schemaName);
            return;
        }

        // 캐시에 이미 계산된 결과가 있으면 사용
        Map<String, Integer> cached = sharedCache.getTypeCounts(schemaName, specName);
        if (cached != null) {
            log.debug("[TYPE COUNT] 캐시에서 '{}' 스키마의 타입 카운트를 가져옵니다 (참조 중, {}).", schemaName, specName);
            mergeTypeCounts(typeCounts, cached);
            return;
        }

        // 스키마 조회
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            return;
        }

        Schema schema = schemas.get(schemaName);
        if (schema == null) {
            log.debug("[TYPE COUNT] 스키마를 찾을 수 없습니다: {}", schemaName);
            return;
        }

        // 방문 표시
        visited.add(schemaName);

        // 현재 스키마의 타입 카운트를 계산
        Map<String, Integer> schemaTypeCounts = new HashMap<>();
        collectPrimitiveTypesFromSchema(schema, components, schemaTypeCounts, visited, specName, sharedCache);

        // 캐시에 저장 (다음에 같은 스키마를 참조할 때 재사용)
        sharedCache.putTypeCounts(schemaName, specName, schemaTypeCounts);
        log.debug("[TYPE COUNT] '{}' 스키마의 타입 카운트를 캐시에 저장했습니다 (참조 중, {}): {}", schemaName, specName, schemaTypeCounts);

        // 결과를 누적
        mergeTypeCounts(typeCounts, schemaTypeCounts);

        // 방문 해제 (다른 경로에서 접근 가능하도록)
        visited.remove(schemaName);
    }

    /**
     * 두 타입 카운트 맵을 병합합니다.
     *
     * @param target 누적할 대상 Map
     * @param source 병합할 소스 Map
     */
    private void mergeTypeCounts(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String type = entry.getKey();
            Integer count = entry.getValue();
            target.put(type, target.getOrDefault(type, 0) + count);
        }
    }

    /**
     * 주어진 타입이 실제 기본형 타입인지 확인합니다.
     * <p>
     * object와 array는 기본형이 아니므로 false를 반환합니다.
     *
     * @param type 확인할 타입
     * @return 실제 기본형 타입이면 true, 그렇지 않으면 false
     */
    private boolean isActualPrimitiveType(String type) {
        if (type == null) {
            return false;
        }
        // object와 array는 제외하고 기본형만 카운트
        return type.equals("string") || type.equals("integer") || type.equals("number") || type.equals("boolean");
    }
}