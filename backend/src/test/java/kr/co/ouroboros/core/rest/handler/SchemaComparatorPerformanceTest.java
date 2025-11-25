package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.common.dto.Components;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.handler.comparator.SchemaComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * SchemaComparator 성능 비교 테스트
 * <p>
 * Memoization 적용 전후의 성능 차이를 측정합니다.
 * 같은 스키마가 여러 번 참조되는 복잡한 케이스에서 성능 개선 효과를 확인할 수 있습니다.
 */
public class SchemaComparatorPerformanceTest {

    private SchemaComparator schemaComparator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Yaml yaml = new Yaml();

    @BeforeEach
    public void setUp() {
        schemaComparator = new SchemaComparator();
    }

    /**
     * YAML 문자열을 파싱하여 Components를 추출합니다.
     */
    private Components parseComponentsFromYaml(String yamlContent) throws Exception {
        Map<String, Object> yamlMap = yaml.load(yamlContent);
        String jsonContent = objectMapper.writeValueAsString(yamlMap);
        OuroRestApiSpec spec = objectMapper.readValue(jsonContent, OuroRestApiSpec.class);
        return spec.getComponents();
    }

    /**
     * 많은 $ref 참조가 있는 복잡한 스키마 생성
     * 같은 스키마가 여러 번 참조되는 경우를 시뮬레이션합니다.
     *
     * @param depth 중첩 깊이 (몇 단계까지 중첩할지)
     * @param breadth 각 레벨에서 참조할 스키마 개수
     * @return 생성된 YAML 문자열
     */
    private String createComplexSchemaWithManyRefs(int depth, int breadth) {
        StringBuilder yamlBuilder = new StringBuilder();
        yamlBuilder.append("openapi: 3.1.0\n");
        yamlBuilder.append("info:\n");
        yamlBuilder.append("  title: Performance Test API\n");
        yamlBuilder.append("  version: 1.0.0\n");
        yamlBuilder.append("components:\n");
        yamlBuilder.append("  schemas:\n");

        // 기본 스키마들 생성 (여러 번 참조될 스키마)
        for (int i = 0; i < breadth; i++) {
            yamlBuilder.append(String.format("    BaseSchema%d:\n", i));
            yamlBuilder.append("      type: object\n");
            yamlBuilder.append("      properties:\n");
            yamlBuilder.append(String.format("        field%d:\n", i));
            yamlBuilder.append("          type: string\n");
            yamlBuilder.append(String.format("        value%d:\n", i));
            yamlBuilder.append("          type: integer\n");
            yamlBuilder.append(String.format("        count%d:\n", i));
            yamlBuilder.append("          type: number\n");
            yamlBuilder.append(String.format("        active%d:\n", i));
            yamlBuilder.append("          type: boolean\n");
        }

        // 중첩된 스키마 생성 (여러 번 참조)
        for (int d = 0; d < depth; d++) {
            yamlBuilder.append(String.format("    NestedSchema%d:\n", d));
            yamlBuilder.append("      type: object\n");
            yamlBuilder.append("      properties:\n");
            
            // 각 중첩 스키마가 여러 BaseSchema를 참조
            for (int b = 0; b < breadth; b++) {
                yamlBuilder.append(String.format("        ref%d:\n", b));
                yamlBuilder.append(String.format("          $ref: \"#/components/schemas/BaseSchema%d\"\n", b % breadth));
            }
        }

        return yamlBuilder.toString();
    }

    /**
     * 기본 성능 측정 테스트
     * 복잡한 스키마에서 flattenSchemas의 실행 시간을 측정합니다.
     */
    @Test
    public void 기본_성능_측정_테스트() throws Exception {
        // 복잡한 스키마 생성 (깊이 5, 너비 10)
        String yamlContent = createComplexSchemaWithManyRefs(5, 10);
        Components components = parseComponentsFromYaml(yamlContent);
        
        // Components 검증
        assertNotNull(components, "Components가 null이 아니어야 합니다.");
        assertNotNull(components.getSchemas(), "Schemas가 null이 아니어야 합니다.");
        assertTrue(components.getSchemas().size() > 0, "Schemas가 비어있지 않아야 합니다.");

        // 워밍업 (JIT 컴파일 최적화)
        for (int i = 0; i < 3; i++) {
            schemaComparator.flattenSchemas(components);
        }

        // 실제 측정
        int iterations = 100;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            schemaComparator.flattenSchemas(components);
        }
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double averageTimeMs = totalTime / 1_000_000.0 / iterations;

        System.out.println("\n=== 기본 성능 측정 결과 ===");
        System.out.printf("스키마 구성: 깊이 5, 너비 10%n");
        System.out.printf("실제 스키마 개수: %d개%n", components.getSchemas().size());
        System.out.printf("실행 횟수: %d회%n", iterations);
        System.out.printf("총 실행 시간: %,d ns (%.2f ms)%n", totalTime, totalTime / 1_000_000.0);
        System.out.printf("평균 실행 시간: %.2f ms%n", averageTimeMs);
        System.out.println("===========================\n");

        // 결과 검증
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);
        assertNotNull(result, "결과가 null이 아니어야 합니다.");
        assertTrue(result.size() > 0, "결과가 있어야 합니다.");
    }

    /**
     * 대규모 스키마 성능 테스트
     * 더 많은 스키마와 참조로 성능을 측정합니다.
     */
    @Test
    public void 대규모_스키마_성능_테스트() throws Exception {
        // 더 큰 스키마로 테스트 (깊이 10, 너비 20)
        String yamlContent = createComplexSchemaWithManyRefs(10, 20);
        Components components = parseComponentsFromYaml(yamlContent);
        
        // Components 검증
        assertNotNull(components, "Components가 null이 아니어야 합니다.");
        assertNotNull(components.getSchemas(), "Schemas가 null이 아니어야 합니다.");
        assertTrue(components.getSchemas().size() > 0, "Schemas가 비어있지 않아야 합니다.");

        // 워밍업
        for (int i = 0; i < 3; i++) {
            schemaComparator.flattenSchemas(components);
        }

        // 측정
        int iterations = 1000;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            schemaComparator.flattenSchemas(components);
        }
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double averageTimeMs = totalTime / 1_000_000.0 / iterations;

        System.out.println("\n=== 대규모 스키마 성능 측정 결과 ===");
        System.out.printf("스키마 구성: 깊이 10, 너비 20%n");
        System.out.printf("실제 스키마 개수: %d개%n", components.getSchemas().size());
        System.out.printf("실행 횟수: %d회%n", iterations);
        System.out.printf("총 실행 시간: %,d ns (%.2f ms)%n", totalTime, totalTime / 1_000_000.0);
        System.out.printf("평균 실행 시간: %.2f ms%n", averageTimeMs);
        System.out.println("====================================\n");

        // 결과 검증
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);
        assertNotNull(result, "결과가 null이 아니어야 합니다.");
        assertTrue(result.size() > 0, "결과가 있어야 합니다.");
    }

    /**
     * 중복 참조가 많은 경우 성능 테스트
     * 같은 스키마가 여러 번 참조되는 경우의 성능을 측정합니다.
     * Memoization의 효과를 확인하기 좋은 테스트입니다.
     */
    @Test
    public void 중복_참조_많은_경우_성능_테스트() throws Exception {
        // 같은 스키마를 여러 번 참조하는 케이스
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Duplicate Reference Test
              version: 1.0.0
            components:
              schemas:
                CommonSchema:
                  type: object
                  properties:
                    id:
                      type: integer
                    name:
                      type: string
                    description:
                      type: string
                    value:
                      type: number
                    active:
                      type: boolean
                Schema1:
                  type: object
                  properties:
                    common1:
                      $ref: "#/components/schemas/CommonSchema"
                    common2:
                      $ref: "#/components/schemas/CommonSchema"
                    common3:
                      $ref: "#/components/schemas/CommonSchema"
                Schema2:
                  type: object
                  properties:
                    common1:
                      $ref: "#/components/schemas/CommonSchema"
                    common2:
                      $ref: "#/components/schemas/CommonSchema"
                    common3:
                      $ref: "#/components/schemas/CommonSchema"
                    common4:
                      $ref: "#/components/schemas/CommonSchema"
                Schema3:
                  type: object
                  properties:
                    common1:
                      $ref: "#/components/schemas/CommonSchema"
                    common2:
                      $ref: "#/components/schemas/CommonSchema"
                    common3:
                      $ref: "#/components/schemas/CommonSchema"
                    common4:
                      $ref: "#/components/schemas/CommonSchema"
                    common5:
                      $ref: "#/components/schemas/CommonSchema"
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        
        // Components 검증
        assertNotNull(components, "Components가 null이 아니어야 합니다.");
        assertNotNull(components.getSchemas(), "Schemas가 null이 아니어야 합니다.");

        // 워밍업
        for (int i = 0; i < 3; i++) {
            schemaComparator.flattenSchemas(components);
        }

        // 측정
        int iterations = 200;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            schemaComparator.flattenSchemas(components);
        }
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double averageTimeMs = totalTime / 1_000_000.0 / iterations;

        System.out.println("\n=== 중복 참조 많은 경우 성능 측정 결과 ===");
        System.out.printf("CommonSchema가 총 12번 참조됨 (Schema1: 3번, Schema2: 4번, Schema3: 5번)%n");
        System.out.printf("실행 횟수: %d회%n", iterations);
        System.out.printf("총 실행 시간: %,d ns (%.2f ms)%n", totalTime, totalTime / 1_000_000.0);
        System.out.printf("평균 실행 시간: %.2f ms%n", averageTimeMs);
        System.out.println("==========================================\n");

        // 결과 검증
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);
        assertNotNull(result, "결과가 null이 아니어야 합니다.");
        assertTrue(result.size() >= 4, "최소 4개의 스키마가 있어야 합니다.");
        
        // CommonSchema가 여러 번 참조되었는지 확인
        SchemaComparator.TypeCnts commonResult = result.get("CommonSchema");
        assertNotNull(commonResult, "CommonSchema 결과가 있어야 합니다.");
    }

    /**
     * 깊은 중첩 구조 성능 테스트
     * 깊게 중첩된 스키마 구조에서의 성능을 측정합니다.
     */
    @Test
    public void 깊은_중첩_구조_성능_테스트() throws Exception {
        // 깊게 중첩된 구조 생성
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Deep Nesting Test
              version: 1.0.0
            components:
              schemas:
                Level5:
                  type: object
                  properties:
                    field:
                      type: string
                Level4:
                  type: object
                  properties:
                    level5:
                      $ref: "#/components/schemas/Level5"
                Level3:
                  type: object
                  properties:
                    level4:
                      $ref: "#/components/schemas/Level4"
                Level2:
                  type: object
                  properties:
                    level3:
                      $ref: "#/components/schemas/Level3"
                Level1:
                  type: object
                  properties:
                    level2:
                      $ref: "#/components/schemas/Level2"
                    level2_2:
                      $ref: "#/components/schemas/Level2"
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        
        // Components 검증
        assertNotNull(components, "Components가 null이 아니어야 합니다.");
        assertNotNull(components.getSchemas(), "Schemas가 null이 아니어야 합니다.");

        // 워밍업
        for (int i = 0; i < 3; i++) {
            schemaComparator.flattenSchemas(components);
        }

        // 측정
        int iterations = 200;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            schemaComparator.flattenSchemas(components);
        }
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double averageTimeMs = totalTime / 1_000_000.0 / iterations;

        System.out.println("\n=== 깊은 중첩 구조 성능 측정 결과 ===");
        System.out.printf("5단계 깊이의 중첩 구조%n");
        System.out.printf("실행 횟수: %d회%n", iterations);
        System.out.printf("총 실행 시간: %,d ns (%.2f ms)%n", totalTime, totalTime / 1_000_000.0);
        System.out.printf("평균 실행 시간: %.2f ms%n", averageTimeMs);
        System.out.println("=====================================\n");

        // 결과 검증
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);
        assertNotNull(result, "결과가 null이 아니어야 합니다.");
        assertTrue(result.size() >= 5, "최소 5개의 스키마가 있어야 합니다.");
    }
}

