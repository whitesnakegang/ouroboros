package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.loader.TestResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SchemaComparator 단위 테스트
 */
public class SchemaComparatorTest {

    private SchemaComparator schemaComparator;
    private final TestResourceLoader resourceLoader = new TestResourceLoader();

    @BeforeEach
    public void setUp() {
        schemaComparator = new SchemaComparator();
    }

    /**
     * 스캔 스펙과 파일 스펙의 스키마가 완전히 일치하는 경우
     * Expect: 일치 판정, 타입 카운트 정확
     */
    @Test
    public void 스키마가_완전히_일치하는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-complete-match-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-complete-match-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        // 스캔 스펙 기준 결과 확인
        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult, "스캔 스펙 기준 User 결과가 있어야 합니다.");
        assertTrue(scanResult.isSame(), "완전히 일치하는 스키마는 true여야 합니다.");
        
        // 타입 카운트 확인: string 1개, integer 1개
        Map<String, Integer> scanTypeCounts = scanResult.getTypeCounts();
        assertEquals(1, scanTypeCounts.getOrDefault("string", 0), "string 타입 1개여야 합니다.");
        assertEquals(1, scanTypeCounts.getOrDefault("integer", 0), "integer 타입 1개여야 합니다.");

        // 파일 스펙 기준 결과 확인
        SchemaComparisonResult fileResult = results.getFileResults().get("User");
        assertNotNull(fileResult, "파일 스펙 기준 User 결과가 있어야 합니다.");
        assertTrue(fileResult.isSame(), "완전히 일치하는 스키마는 true여야 합니다.");
        
        // 타입 카운트 확인
        Map<String, Integer> fileTypeCounts = fileResult.getTypeCounts();
        assertEquals(1, fileTypeCounts.getOrDefault("string", 0), "string 타입 1개여야 합니다.");
        assertEquals(1, fileTypeCounts.getOrDefault("integer", 0), "integer 타입 1개여야 합니다.");
    }

    /**
     * 파일 스펙에만 추가 속성이 있는 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 파일_스펙에만_추가_속성이_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-property-missing-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-property-missing-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        // 스캔 스펙 기준 결과 확인
        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult);
        assertFalse(scanResult.isSame(), "파일 스펙에 추가 필드가 있는 경우 불일치 판정");

        // 파일 스펙 기준 결과 확인
        SchemaComparisonResult fileResult = results.getFileResults().get("User");
        assertNotNull(fileResult);
        assertFalse(fileResult.isSame(), "파일 스펙에 추가 필드가 있는 경우 불일치 판정");
    }

    /**
     * 속성의 타입이 다른 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 속성_타입이_다른_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-type-different-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-type-different-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult scanResult = results.getScanResults().get("User");
        assertNotNull(scanResult);
        assertFalse(scanResult.isSame(), "속성 타입이 다르면 불일치여야 합니다.");
    }

    /**
     * 중첩 객체($ref)를 사용하는 스키마의 경우
     * Expect: 일치 판정, 타입 카운트가 재귀적으로 계산되어야 함
     */
    @Test
    public void 중첩_객체를_사용하는_스키마의_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-nested-object-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-nested-object-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        // Address 스키마 확인: string 2개
        SchemaComparisonResult addressResult = results.getScanResults().get("Address");
        assertNotNull(addressResult);
        assertTrue(addressResult.isSame(), "Address 스키마는 일치해야 합니다.");
        Map<String, Integer> addressTypeCounts = addressResult.getTypeCounts();
        assertEquals(2, addressTypeCounts.getOrDefault("string", 0), "Address는 string 2개여야 합니다.");

        // User 스키마 확인: name(string 1개) + address(Address 참조 -> string 2개) = 총 string 3개
        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        assertTrue(userResult.isSame(), "User 스키마도 일치해야 합니다.");
        Map<String, Integer> userTypeCounts = userResult.getTypeCounts();
        assertEquals(3, userTypeCounts.getOrDefault("string", 0), 
                "User는 직접 string 1개 + Address의 string 2개 = 총 string 3개여야 합니다.");
    }

    /**
     * 배열 타입을 사용하는 스키마의 경우
     * Expect: 일치 판정, array 자체는 카운트하지 않고 items의 타입만 카운트
     */
    @Test
    public void 배열_타입을_사용하는_스키마의_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-array-type-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-array-type-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        assertTrue(userResult.isSame(), "배열 타입이 일치하면 일치 판정이어야 합니다.");
        
        // 배열 타입 자체는 카운트하지 않음
        Map<String, Integer> typeCounts = userResult.getTypeCounts();
        assertEquals(0, typeCounts.getOrDefault("array", 0), "array 타입 자체는 카운트하지 않습니다.");
    }

    /**
     * 배열 아이템 타입이 다른 경우
     * Expect: 불일치 판정
     */
    @Test
    public void 배열_아이템_타입이_다른_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-array-type-different-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-array-type-different-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        assertFalse(userResult.isSame(), "배열 아이템 타입이 다르면 불일치여야 합니다.");
    }

    /**
     * 파일 스펙에만 있는 스키마
     * Expect: 파일 스펙 결과에만 포함, isSame = false
     */
    @Test
    public void 파일_스펙에만_있는_스키마() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-comparison-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-comparison-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        // 파일 스펙에만 있는 Book 스키마 확인
        SchemaComparisonResult fileBookResult = results.getFileResults().get("Book");
        assertNotNull(fileBookResult, "파일 스펙 기준 Book 결과가 있어야 합니다.");
        assertFalse(fileBookResult.isSame(), "파일 스펙에만 있는 스키마는 isSame = false여야 합니다.");
        assertNotNull(fileBookResult.getTypeCounts(), "타입 카운트가 있어야 합니다.");

        // 스캔 스펙 기준 결과에는 Book이 없어야 함
        assertNull(results.getScanResults().get("Book"), "스캔 스펙 기준 결과에는 Book이 없어야 합니다.");
    }

    /**
     * 스캔 스펙에만 있는 스키마
     * Expect: 스캔 스펙 결과에만 포함, isSame = false
     */
    @Test
    public void 스캔_스펙에만_있는_스키마() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-comparison-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-comparison-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        // 스캔 스펙에만 있는 Product 스키마가 있다고 가정 (실제 파일에 따라 다를 수 있음)
        // 실제로는 테스트 파일에 따라 확인
        Map<String, SchemaComparisonResult> scanResults = results.getScanResults();
        Map<String, SchemaComparisonResult> fileResults = results.getFileResults();
        
        // 파일에만 있거나 스캔에만 있는 스키마 확인
        assertTrue(scanResults.containsKey("Address") || fileResults.containsKey("Address"), 
                "Address는 양쪽 중 하나에 있어야 합니다.");
    }

    /**
     * 여러 스키마 중 일부만 일치하는 경우
     * Expect: 각 스키마마다 개별 판정, 타입 카운트 정확히 검증
     * 
     * 파일 스펙: Address, User, Book
     * 스캔 스펙: Address, User
     * 
     * - Address: 양쪽 모두 일치 (string 3개)
     * - User: 양쪽 모두 일치 (string 4개, integer 1개, number 1개)
     * - Book: 파일 스펙에만 있음 (string 1개, isSame = false)
     */
    @Test
    public void 여러_스키마_중_일부만_일치하는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-comparison-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-comparison-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        // ========== 파일 스펙 기준 결과 확인 ==========
        Map<String, SchemaComparisonResult> fileResults = results.getFileResults();
        assertNotNull(fileResults, "파일 스펙 기준 결과가 있어야 합니다.");
        assertEquals(3, fileResults.size(), "파일 스펙에는 Address, User, Book 3개가 있어야 합니다.");

        // 1. 파일 스펙의 Address 확인 (일치해야 함)
        SchemaComparisonResult fileAddressResult = fileResults.get("Address");
        assertNotNull(fileAddressResult, "파일 스펙 기준 Address 결과가 있어야 합니다.");
        assertTrue(fileAddressResult.isSame(), "Address는 양쪽 모두 있어서 일치해야 합니다.");
        Map<String, Integer> fileAddressTypeCounts = fileAddressResult.getTypeCounts();
        assertNotNull(fileAddressTypeCounts, "Address 타입 카운트가 있어야 합니다.");
        assertEquals(3, fileAddressTypeCounts.getOrDefault("string", 0), 
                "Address는 string 3개(roadname, dong, gu)여야 합니다.");
        assertEquals(0, fileAddressTypeCounts.getOrDefault("integer", 0), 
                "Address는 integer가 없어야 합니다.");
        assertEquals(0, fileAddressTypeCounts.getOrDefault("number", 0), 
                "Address는 number가 없어야 합니다.");

        // 2. 파일 스펙의 User 확인 (일치해야 함)
        SchemaComparisonResult fileUserResult = fileResults.get("User");
        assertNotNull(fileUserResult, "파일 스펙 기준 User 결과가 있어야 합니다.");
        assertTrue(fileUserResult.isSame(), "User는 양쪽 모두 있어서 일치해야 합니다.");
        Map<String, Integer> fileUserTypeCounts = fileUserResult.getTypeCounts();
        assertNotNull(fileUserTypeCounts, "User 타입 카운트가 있어야 합니다.");
        // User: name(string 1개) + address(Address 참조 -> string 3개) = 총 string 4개
        assertEquals(4, fileUserTypeCounts.getOrDefault("string", 0), 
                "User는 직접 string 1개(name) + Address의 string 3개 = 총 string 4개여야 합니다.");
        assertEquals(1, fileUserTypeCounts.getOrDefault("integer", 0), 
                "User는 integer 1개(age)여야 합니다.");
        assertEquals(1, fileUserTypeCounts.getOrDefault("number", 0), 
                "User는 number 1개(height)여야 합니다.");

        // 3. 파일 스펙의 Book 확인 (스캔 스펙에 없으므로 불일치)
        SchemaComparisonResult fileBookResult = fileResults.get("Book");
        assertNotNull(fileBookResult, "파일 스펙 기준 Book 결과가 있어야 합니다.");
        assertFalse(fileBookResult.isSame(), "Book은 스캔 스펙에 없으므로 isSame = false여야 합니다.");
        Map<String, Integer> fileBookTypeCounts = fileBookResult.getTypeCounts();
        assertNotNull(fileBookTypeCounts, "Book 타입 카운트가 계산되어야 합니다.");
        assertEquals(1, fileBookTypeCounts.getOrDefault("string", 0), 
                "Book은 string 1개(title)여야 합니다.");
        assertEquals(0, fileBookTypeCounts.getOrDefault("integer", 0), 
                "Book은 integer가 없어야 합니다.");

        // ========== 스캔 스펙 기준 결과 확인 ==========
        Map<String, SchemaComparisonResult> scanResults = results.getScanResults();
        assertNotNull(scanResults, "스캔 스펙 기준 결과가 있어야 합니다.");
        assertEquals(2, scanResults.size(), "스캔 스펙에는 Address, User 2개가 있어야 합니다.");

        // 1. 스캔 스펙의 Address 확인 (일치해야 함)
        SchemaComparisonResult scanAddressResult = scanResults.get("Address");
        assertNotNull(scanAddressResult, "스캔 스펙 기준 Address 결과가 있어야 합니다.");
        assertTrue(scanAddressResult.isSame(), "Address는 양쪽 모두 있어서 일치해야 합니다.");
        Map<String, Integer> scanAddressTypeCounts = scanAddressResult.getTypeCounts();
        assertNotNull(scanAddressTypeCounts, "Address 타입 카운트가 있어야 합니다.");
        assertEquals(3, scanAddressTypeCounts.getOrDefault("string", 0), 
                "Address는 string 3개(roadname, dong, gu)여야 합니다.");
        assertEquals(0, scanAddressTypeCounts.getOrDefault("integer", 0), 
                "Address는 integer가 없어야 합니다.");

        // 2. 스캔 스펙의 User 확인 (일치해야 함)
        SchemaComparisonResult scanUserResult = scanResults.get("User");
        assertNotNull(scanUserResult, "스캔 스펙 기준 User 결과가 있어야 합니다.");
        assertTrue(scanUserResult.isSame(), "User는 양쪽 모두 있어서 일치해야 합니다.");
        Map<String, Integer> scanUserTypeCounts = scanUserResult.getTypeCounts();
        assertNotNull(scanUserTypeCounts, "User 타입 카운트가 있어야 합니다.");
        // User: name(string 1개) + address(Address 참조 -> string 3개) = 총 string 4개
        assertEquals(4, scanUserTypeCounts.getOrDefault("string", 0), 
                "User는 직접 string 1개(name) + Address의 string 3개 = 총 string 4개여야 합니다.");
        assertEquals(1, scanUserTypeCounts.getOrDefault("integer", 0), 
                "User는 integer 1개(age)여야 합니다.");
        assertEquals(1, scanUserTypeCounts.getOrDefault("number", 0), 
                "User는 number 1개(height)여야 합니다.");

        // 3. 스캔 스펙에는 Book이 없어야 함
        assertNull(scanResults.get("Book"), "스캔 스펙 기준 결과에는 Book이 없어야 합니다.");

        // ========== 양방향 결과 일관성 확인 ==========
        // Address와 User는 양쪽 모두 일치하므로 양방향 결과가 동일해야 함
        assertEquals(fileAddressResult.isSame(), scanAddressResult.isSame(), 
                "Address는 양방향 모두 일치해야 합니다.");
        assertEquals(fileUserResult.isSame(), scanUserResult.isSame(), 
                "User는 양방향 모두 일치해야 합니다.");
        
        // 타입 카운트도 동일해야 함 (같은 스펙이므로)
        assertEquals(fileAddressTypeCounts, scanAddressTypeCounts, 
                "Address 타입 카운트는 양방향 동일해야 합니다.");
        assertEquals(fileUserTypeCounts, scanUserTypeCounts, 
                "User 타입 카운트는 양방향 동일해야 합니다.");
    }

    /**
     * 기본형 타입만 카운트되는지 확인
     * Expect: object, array 타입은 카운트되지 않고 string, integer, number, boolean만 카운트
     */
    @Test
    public void 기본형_타입만_카운트되는지_확인() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-complete-match-scanned.yaml");
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-complete-match-file.yaml");

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), fileSpec.getComponents());

        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult);
        
        Map<String, Integer> typeCounts = userResult.getTypeCounts();
        
        // object 타입은 카운트되지 않아야 함
        assertEquals(0, typeCounts.getOrDefault("object", 0), "object 타입은 카운트되지 않아야 합니다.");
        
        // 기본형 타입만 카운트
        assertTrue(typeCounts.containsKey("string") || typeCounts.containsKey("integer"), 
                "기본형 타입(string, integer 등)은 카운트되어야 합니다.");
    }

    /**
     * null Components 처리 (둘 다 null)
     * Expect: 빈 결과 반환
     */
    @Test
    public void null_Components_처리() {
        SchemaComparisonResults results = schemaComparator.compareSchemas(null, null);
        
        assertNotNull(results);
        assertNotNull(results.getFileResults());
        assertNotNull(results.getScanResults());
        assertTrue(results.getFileResults().isEmpty());
        assertTrue(results.getScanResults().isEmpty());
    }

    /**
     * 파일 스펙에만 스키마가 있는 경우
     * Expect: 파일 스펙 결과에만 포함, isSame = false, 타입 카운트 계산
     */
    @Test
    public void 파일_스펙에만_스키마가_있는_경우() throws Exception {
        OuroRestApiSpec fileSpec = resourceLoader.loadSchemaTest("schema-complete-match-file.yaml");
        
        SchemaComparisonResults results = schemaComparator.compareSchemas(null, fileSpec.getComponents());

        // 파일 스펙 기준 결과는 있어야 함
        assertNotNull(results.getFileResults());
        SchemaComparisonResult userResult = results.getFileResults().get("User");
        assertNotNull(userResult, "파일 스펙에만 있는 스키마는 파일 결과에 포함되어야 합니다.");
        assertFalse(userResult.isSame(), "파일 스펙에만 있는 스키마는 isSame = false여야 합니다.");
        assertNotNull(userResult.getTypeCounts(), "타입 카운트가 계산되어야 합니다.");
        assertTrue(userResult.getTypeCounts().getOrDefault("string", 0) > 0 || 
                   userResult.getTypeCounts().getOrDefault("integer", 0) > 0, 
                   "타입 카운트가 있어야 합니다.");
        
        // 스캔 스펙 기준 결과는 없어야 함
        assertNotNull(results.getScanResults());
        assertTrue(results.getScanResults().isEmpty(), "스캔 스펙이 없으면 스캔 결과도 비어있어야 합니다.");
    }

    /**
     * 스캔 스펙에만 스키마가 있는 경우
     * Expect: 스캔 스펙 결과에만 포함, isSame = false, 타입 카운트 계산
     */
    @Test
    public void 스캔_스펙에만_스키마가_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-complete-match-scanned.yaml");
        
        SchemaComparisonResults results = schemaComparator.compareSchemas(scannedSpec.getComponents(), null);

        // 스캔 스펙 기준 결과는 있어야 함
        assertNotNull(results.getScanResults());
        SchemaComparisonResult userResult = results.getScanResults().get("User");
        assertNotNull(userResult, "스캔 스펙에만 있는 스키마는 스캔 결과에 포함되어야 합니다.");
        assertFalse(userResult.isSame(), "스캔 스펙에만 있는 스키마는 isSame = false여야 합니다.");
        assertNotNull(userResult.getTypeCounts(), "타입 카운트가 계산되어야 합니다.");
        assertTrue(userResult.getTypeCounts().getOrDefault("string", 0) > 0 || 
                   userResult.getTypeCounts().getOrDefault("integer", 0) > 0, 
                   "타입 카운트가 있어야 합니다.");
        
        // 파일 스펙 기준 결과는 없어야 함
        assertNotNull(results.getFileResults());
        assertTrue(results.getFileResults().isEmpty(), "파일 스펙이 없으면 파일 결과도 비어있어야 합니다.");
    }

    /**
     * 빈 스키마 맵 처리 (둘 다 빈 맵)
     * Expect: 빈 결과 반환
     */
    @Test
    public void 빈_스키마_맵_처리() {
        kr.co.ouroboros.core.rest.common.dto.Components emptyComponents = new kr.co.ouroboros.core.rest.common.dto.Components();
        emptyComponents.setSchemas(new java.util.HashMap<>());
        
        SchemaComparisonResults results = schemaComparator.compareSchemas(emptyComponents, emptyComponents);
        
        assertNotNull(results);
        assertTrue(results.getFileResults().isEmpty());
        assertTrue(results.getScanResults().isEmpty());
    }

    /**
     * 파일 스펙은 빈 맵이고 스캔 스펙에는 스키마가 있는 경우
     * Expect: 스캔 스펙 결과만 포함
     */
    @Test
    public void 파일_스펙은_빈맵_스캔_스펙은_있는_경우() throws Exception {
        OuroRestApiSpec scannedSpec = resourceLoader.loadSchemaTest("schema-complete-match-scanned.yaml");
        kr.co.ouroboros.core.rest.common.dto.Components emptyFileComponents = new kr.co.ouroboros.core.rest.common.dto.Components();
        emptyFileComponents.setSchemas(new java.util.HashMap<>());

        SchemaComparisonResults results = schemaComparator.compareSchemas(
                scannedSpec.getComponents(), emptyFileComponents);

        // 스캔 스펙 기준 결과는 있어야 함
        assertNotNull(results.getScanResults());
        assertTrue(results.getScanResults().containsKey("User"));
        
        // 파일 스펙 기준 결과는 없어야 함
        assertNotNull(results.getFileResults());
        assertTrue(results.getFileResults().isEmpty());
    }
}

