package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.loader.TestResourceLoader;
import org.junit.jupiter.api.Test;

public class SchemaComparisonTest {

    private final TestResourceLoader resourceLoader = new TestResourceLoader();

    /**
     * 스캔 스펙과 파일 스펙의 스키마가 완전히 일치하는 경우
     * Expect: 일치 판정
     */
    @Test
    public void 스키마가_완전히_일치하는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-complete-match-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-complete-match-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertTrue(results.get("User"), "완전히 일치하는 스키마는 true여야 합니다.");
    }

    /**
     * 파일 스펙에만 추가 속성이 있는 경우
     * Expect: 일치 판정 (파일 스펙이 더 상세함)
     */
    @Test
    public void 파일_스펙에만_추가_속성이_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-property-missing-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-property-missing-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertFalse(results.get("User"), "파일 스펙에 추가 필드가 있는 경우 불일치 판정");
    }
    
    /**
     * 스캔 스펙에만 추가 속성이 있는 경우
     * Expect: 일치 판정 (스캔 스펙이 더 상세함)
     */
    @Test
    public void 스캔_스펙에만_추가_속성이_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-scan-additional-property-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-scan-additional-property-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertTrue(results.get("User"), "스캔 스펙에 추가 속성이 있어도 일치해야 합니다.");
    }

    /**
     * 속성의 타입이 다른 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 속성_타입이_다른_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-type-different-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-type-different-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertFalse(results.get("User"), "속성 타입이 다르면 불일치여야 합니다.");
    }

    /**
     * 중첩 객체($ref)를 사용하는 스키마의 경우
     * Expect: 일치 판정
     */
    @Test
    public void 중첩_객체를_사용하는_스키마의_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-nested-object-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-nested-object-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertTrue(results.get("Address"), "Address 스키마는 일치해야 합니다.");
        assertTrue(results.get("User"), "User 스키마도 일치해야 합니다.");
    }

    /**
     * 배열 타입을 사용하는 스키마의 경우
     * Expect: 일치 판정
     */
    @Test
    public void 배열_타입을_사용하는_스키마의_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-array-type-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-array-type-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertTrue(results.get("User"), "배열 타입이 일치하면 일치 판정이어야 합니다.");
    }

    /**
     * 배열 아이템 타입이 다른 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 배열_아이템_타입이_다른_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-array-type-different-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-array-type-different-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        assertFalse(results.get("User"), "배열 아이템 타입이 다르면 불일치여야 합니다.");
    }

    /**
     * 여러 스키마 중 일부만 일치하는 경우
     * Expect: 각 스키마마다 개별 판정
     */
    @Test
    public void 여러_스키마_중_일부만_일치하는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-comparison-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-comparison-file.yaml");

        SchemaComparator schemaComparator = new SchemaComparator();
        Map<String, Boolean> results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        System.out.println("=== 스키마 비교 결과 ===");
        results.forEach((schemaName, isMatch) ->
                System.out.println(schemaName + ": " + (isMatch ? "일치" : "불일치"))
        );

        assertTrue(results.get("Address"), "Address는 일치해야 합니다.");
        assertTrue(results.get("User"), "User는 일치해야 합니다.");
        assertNull(results.get("Book"), "Book은 검사하지 않습니다.");
    }
}
