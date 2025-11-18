package kr.co.ouroboros.core.rest.handler.comparator;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스키마 비교 결과를 담는 클래스.
 * <p>
 * 스키마 일치 여부와 기본형 타입별 개수 정보를 포함합니다.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaComparisonResult {
    
    /**
     * 스키마 내 기본형 타입별 개수
     * <p>
     * Key: 타입명 (string, integer, number, boolean 등)
     * Value: 해당 타입의 개수
     */
    private Map<String, Integer> typeCounts;
}



