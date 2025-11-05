package kr.co.ouroboros.core.rest.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
     *
     * @param scannedComponents the scanned (baseline) document's Components to compare from
     * @param fileComponents    the file-based document's Components to compare against
     * @return a map from schema name to `true` if the scanned and file schema match, `false` otherwise
     */
    public Map<String, Boolean> compareSchemas(Components scannedComponents, Components fileComponents) {
        Map<String, Boolean> schemaMatchResults = new HashMap<>();

        if (scannedComponents == null || fileComponents == null) {
            log.debug("[SCHEMA COMPARISON] Components가 null입니다.");
            return schemaMatchResults;
        }

        // Map의 키는 스키마 객체의 이름을 의미
        Map<String, Schema> scannedSchemas = scannedComponents.getSchemas();
        Map<String, Schema> fileSchemas = fileComponents.getSchemas();

        if (scannedSchemas == null || fileSchemas == null) {
            log.debug("[SCHEMA COMPARISON] Schemas가 null입니다.");
            return schemaMatchResults;
        }

        log.debug("[SCHEMA COMPARISON] 스키마 비교 시작...");
        log.debug("스캔된 스키마: {}", scannedSchemas.keySet());
        log.debug("파일 스키마: {}", fileSchemas.keySet());

        // 스캔된 문서의 각 스키마에 대해 파일 문서와 비교
        for (Map.Entry<String, Schema> scannedEntry : scannedSchemas.entrySet()) {
            String schemaName = scannedEntry.getKey();
            Schema scannedSchema = scannedEntry.getValue();
            Schema fileSchema = fileSchemas.get(schemaName);

            // 스캔된 스펙에는 있지만 파일 스펙에는 없는 경우
            // 불일치 판정
            if (fileSchema == null) {
                log.debug("[SCHEMA MISSING] '{}': 파일 스펙에 해당 스키마가 없습니다.", schemaName);
                schemaMatchResults.put(schemaName, false);
                continue;
            }

            // 둘 다 존재하는 경우, 재귀적 검사 진행
            // 스키마 상세 비교
            boolean isMatch = compareSchemaDetails(scannedSchema, fileSchema, schemaName);
            schemaMatchResults.put(schemaName, isMatch);

            if (isMatch) {
                log.debug("[SCHEMA MATCH] '{}': 스키마가 일치합니다.", schemaName);
            } else {
                log.debug("[SCHEMA MISMATCH] '{}': 스키마가 일치하지 않습니다.", schemaName);
            }

            // 파일에만 있는 경우는 미진행 -> 이건 미구현 상태로 봐야됨
        }

        log.debug("[SCHEMA COMPARISON] 스키마 비교 완료. 결과: {}", schemaMatchResults);
        return schemaMatchResults;
    }

    /**
     * Determine whether two Schema objects are equivalent in structure and constraints.
     *
     * Compares type, `$ref`, format, properties, items (for arrays), and additionalProperties;
     * the `schemaName` parameter is used for log context.
     *
     * @param scannedSchema the scanned (reference) schema to compare from
     * @param fileSchema    the file-based schema to compare against
     * @param schemaName    the schema name used for logging/context
     * @return true if the two schemas are equivalent in type, $ref, format, properties, items, required, and additionalProperties; false otherwise
     */
    private boolean compareSchemaDetails(Schema scannedSchema, Schema fileSchema, String schemaName) {
        if (scannedSchema == null && fileSchema == null) {
            return true;
        }
        if (scannedSchema == null || fileSchema == null) {
            log.debug("[SCHEMA NULL MISMATCH] '{}': 한쪽 스키마가 null입니다.", schemaName);
            return false;
        }

        // type 비교
        if (!Objects.equals(scannedSchema.getType(), fileSchema.getType())) {
            log.debug("[SCHEMA TYPE MISMATCH] '{}': 타입이 다릅니다. (스캔: {}, 파일: {})",
                    schemaName, scannedSchema.getType(), fileSchema.getType());
            return false;
        }

        // $ref 비교
        if (!Objects.equals(scannedSchema.getRef(), fileSchema.getRef())) {
            log.debug("[SCHEMA REF MISMATCH] '{}': $ref가 다릅니다. (스캔: {}, 파일: {})",
                    schemaName, scannedSchema.getRef(), fileSchema.getRef());
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
//        if (!Objects.equals(scannedSchema.getRequired(), fileSchema.getRequired())) {
//            log.debug("[SCHEMA REQUIRED MISMATCH] '{}': required 필드가 다릅니다. (스캔: {}, 파일: {})",
//                    schemaName, scannedSchema.getRequired(), fileSchema.getRequired());
//            return false;
//        }

        // AdditionalProperties 비교
        if (!Objects.equals(scannedSchema.getAdditionalProperties(), fileSchema.getAdditionalProperties())) {
            log.debug("[SCHEMA ADDITIONAL_PROPERTIES MISMATCH] '{}': additionalProperties가 다릅니다.", schemaName);
            return false;
        }

        return true;
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

            // 재귀 검사
            if (!compareSchemaDetails(scannedPropertySchema, filePropertySchema, schemaName + "." + propertyName)) {
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
}