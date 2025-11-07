package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.loader.TestResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaComparisonTest {

    private SchemaComparator schemaComparator;
    private final TestResourceLoader resourceLoader = new TestResourceLoader();

    @BeforeEach
    public void setUp() {
        schemaComparator = new SchemaComparator();
    }

    /**
     * 스캔 스펙과 파일 스펙의 스키마가 완전히 일치하는 경우
     * Expect: 일치 판정
     */
    @Test
    public void 스키마가_완전히_일치하는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-complete-match-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-complete-match-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult);
        assertTrue(scanResult.isSame(), "완전히 일치하는 스키마는 true여야 합니다.");
        
        SchemaComparisonResult fileResult = results.getFileResults().get("User");
        assertNotNull(fileResult);
        assertTrue(fileResult.isSame(), "완전히 일치하는 스키마는 true여야 합니다.");
    }

    /**
     * 파일 스펙에만 추가 속성이 있는 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 파일_스펙에만_추가_속성이_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-property-missing-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-property-missing-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult);
        assertFalse(scanResult.isSame(), "파일 스펙에 추가 필드가 있는 경우 불일치 판정");
    }
    
    /**
     * 스캔 스펙에만 추가 속성이 있는 경우
     * Expect: 일치 판정 (스캔 스펙이 더 상세함)
     */
    @Test
    public void 스캔_스펙에만_추가_속성이_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-scan-additional-property-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-scan-additional-property-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult);
        assertFalse(scanResult.isSame(), "스캔 스펙에 추가 속성이 있으면 불일치 판정");
    }

    /**
     * 속성의 타입이 다른 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 속성_타입이_다른_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-type-different-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-type-different-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult);
        assertFalse(scanResult.isSame(), "속성 타입이 다르면 불일치여야 합니다.");
    }

    /**
     * 중첩 객체($ref)를 사용하는 스키마의 경우
     * Expect: 일치 판정
     */
    @Test
    public void 중첩_객체를_사용하는_스키마의_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-nested-object-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-nested-object-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult addressResult = results.getScanResults().get("Address");
        assertNotNull(addressResult);
        assertTrue(addressResult.isSame(), "Address 스키마는 일치해야 합니다.");
        
        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        assertTrue(userResult.isSame(), "User 스키마도 일치해야 합니다.");
    }

    /**
     * 배열 타입을 사용하는 스키마의 경우
     * Expect: 일치 판정
     */
    @Test
    public void 배열_타입을_사용하는_스키마의_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-array-type-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-array-type-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        assertTrue(userResult.isSame(), "배열 타입이 일치하면 일치 판정이어야 합니다.");
    }

    /**
     * 배열 아이템 타입이 다른 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 배열_아이템_타입이_다른_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-array-type-different-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-array-type-different-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        assertFalse(userResult.isSame(), "배열 아이템 타입이 다르면 불일치여야 합니다.");
    }

    /**
     * 여러 스키마 중 일부만 일치하는 경우
     * Expect: 각 스키마마다 개별 판정
     */
    @Test
    public void 여러_스키마_중_일부만_일치하는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-comparison-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-comparison-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), fileSpec.getComponents());

        System.out.println("=== 스캔 스펙 기준 스키마 비교 결과 ===");
        results.getScanResults().forEach((schemaName, result) ->
                System.out.println(schemaName + ": " + (result.isSame() ? "일치" : "불일치"))
        );

        System.out.println("=== 파일 스펙 기준 스키마 비교 결과 ===");
        results.getFileResults().forEach((schemaName, result) ->
                System.out.println(schemaName + ": " + (result.isSame() ? "일치" : "불일치"))
        );

        // 스캔 스펙 기준 결과 확인
        SchemaComparisonResult addressScanResult = results.getScanResults().get("Address");
        if (addressScanResult != null) {
            assertTrue(addressScanResult.isSame(), "Address는 일치해야 합니다.");
        }
        
        SchemaComparisonResult userScanResult = results.getScanResults().get("User");
        if (userScanResult != null) {
            assertTrue(userScanResult.isSame(), "User는 일치해야 합니다.");
        }
        
        // 파일 스펙에만 있는 Book 확인
        SchemaComparisonResult bookFileResult = results.getFileResults().get("Book");
        if (bookFileResult != null) {
            assertFalse(bookFileResult.isSame(), "파일 스펙에만 있는 Book은 isSame = false여야 합니다.");
        }
    }
}
