package kr.co.ouroboros.core.rest.handler;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스키마 비교 결과를 담는 래퍼 클래스.
 * <p>
 * 파일 스펙과 스캔 스펙 각각에 대한 비교 결과를 포함합니다.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaComparisonResults {
    
    /**
     * 파일 스펙 기준의 스키마 비교 결과
     */
    private Map<String, SchemaComparisonResult> fileResults;
    
    /**
     * 스캔 스펙 기준의 스키마 비교 결과
     */
    private Map<String, SchemaComparisonResult> scanResults;
}



