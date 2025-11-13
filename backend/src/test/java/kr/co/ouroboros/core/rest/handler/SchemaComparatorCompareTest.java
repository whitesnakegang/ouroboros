package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import kr.co.ouroboros.core.rest.handler.comparator.SchemaComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SchemaComparator의 compareFlattenedSchemas 메서드 단위 테스트
 */
public class SchemaComparatorCompareTest {

    private SchemaComparator schemaComparator;

    @BeforeEach
    public void setUp() {
        schemaComparator = new SchemaComparator();
    }

    /**
     * 완전히 일치하는 경우
     * Expect: 모든 스키마가 true 반환
     */
    @Test
    public void 완전히_일치하는_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("age:integer", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (동일한 내용)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("age:integer", 1);
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get("User"), "완전히 일치하는 스키마는 true여야 합니다.");
    }

    /**
     * 일부 키의 개수가 다른 경우
     * Expect: false 반환
     */
    @Test
    public void 타입_개수가_다른_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("age:integer", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (age:integer 개수가 다름)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("age:integer", 2); // 개수가 다름
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "타입 개수가 다르면 false여야 합니다.");
    }

    /**
     * baseSchemas에만 있는 스키마
     * Expect: false 반환
     */
    @Test
    public void base에만_있는_스키마() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas는 비어있음
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "targetSchemas에 없는 스키마는 false여야 합니다.");
    }

    /**
     * targetSchemas에만 있는 스키마
     * Expect: 결과에 포함되지 않음 (baseSchemas 기준으로만 순회)
     */
    @Test
    public void target에만_있는_스키마() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas에만 Product가 있음
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts productTypeCnts = new SchemaComparator.TypeCnts();
        productTypeCnts.getTypeCounts().put("productName:string", 1);
        targetSchemas.put("Product", productTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size(), "baseSchemas 기준으로만 결과가 생성되므로 User만 포함");
        assertTrue(results.containsKey("User"));
        assertFalse(results.containsKey("Product"), "targetSchemas에만 있는 스키마는 결과에 포함되지 않음");
    }

    /**
     * 키가 다른 경우 (base에는 있지만 target에는 없는 키, 또는 그 반대)
     * Expect: false 반환
     */
    @Test
    public void 키가_다른_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("age:integer", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (다른 키)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("height:number", 1); // age:integer 대신 height:number
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "키가 다르면 false여야 합니다.");
    }

    /**
     * 여러 스키마 중 일부만 일치하는 경우
     */
    @Test
    public void 여러_스키마_중_일부만_일치() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("age:integer", 1);
        baseSchemas.put("User", userTypeCnts);

        SchemaComparator.TypeCnts addressTypeCnts = new SchemaComparator.TypeCnts();
        addressTypeCnts.getTypeCounts().put("roadAddress:string", 1);
        baseSchemas.put("Address", addressTypeCnts);

        // targetSchemas 생성
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("age:integer", 2); // 개수가 다름
        targetSchemas.put("User", targetUserTypeCnts);

        SchemaComparator.TypeCnts targetAddressTypeCnts = new SchemaComparator.TypeCnts();
        targetAddressTypeCnts.getTypeCounts().put("roadAddress:string", 1);
        targetSchemas.put("Address", targetAddressTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertFalse(results.get("User"), "User는 타입 개수가 달라서 false");
        assertTrue(results.get("Address"), "Address는 완전히 일치해서 true");
    }

    /**
     * null baseSchemas 처리
     */
    @Test
    public void null_baseSchemas() {
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        targetSchemas.put("User", new SchemaComparator.TypeCnts());

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(null, targetSchemas);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "null baseSchemas는 빈 결과를 반환해야 합니다.");
    }

    /**
     * 빈 baseSchemas 처리
     */
    @Test
    public void 빈_baseSchemas() {
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        targetSchemas.put("User", new SchemaComparator.TypeCnts());

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "빈 baseSchemas는 빈 결과를 반환해야 합니다.");
    }

    /**
     * null targetSchemas 처리
     */
    @Test
    public void null_targetSchemas() {
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        baseSchemas.put("User", userTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "null targetSchemas는 모든 스키마가 false여야 합니다.");
    }

    /**
     * 복합 케이스: 배열 타입 포함
     */
    @Test
    public void 배열_타입_포함() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("tags:array.string", 1);
        userTypeCnts.getTypeCounts().put("products:array.Product", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (동일)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("tags:array.string", 1);
        targetUserTypeCnts.getTypeCounts().put("products:array.Product", 1);
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get("User"), "배열 타입까지 모두 일치하면 true여야 합니다.");
    }

    /**
     * 배열의 items 타입이 다른 경우 (기본 타입)
     * Expect: false 반환
     */
    @Test
    public void 배열_items_타입이_다른_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("tags:array.string", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (배열 items 타입이 다름)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("tags:array.integer", 1); // string 대신 integer
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "배열 items 타입이 다르면 false여야 합니다.");
    }

    /**
     * 배열의 items가 $ref인 경우 스키마 이름이 다른 경우
     * Expect: false 반환
     */
    @Test
    public void 배열_items_스키마_참조가_다른_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("products:array.Product", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (배열 items 스키마 참조가 다름)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("products:array.Order", 1); // Product 대신 Order
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "배열 items 스키마 참조가 다르면 false여야 합니다.");
    }

    /**
     * 배열 타입과 일반 타입이 섞여있는 경우 일부만 다른 경우
     * Expect: false 반환
     */
    @Test
    public void 배열_타입_일부만_다른_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("tags:array.string", 1);
        userTypeCnts.getTypeCounts().put("scores:array.integer", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (tags는 일치하지만 scores는 다름)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("tags:array.string", 1);
        targetUserTypeCnts.getTypeCounts().put("scores:array.number", 1); // integer 대신 number
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "배열 타입 중 하나라도 다르면 false여야 합니다.");
    }

    /**
     * 배열 타입이 base에만 있는 경우
     * Expect: false 반환
     */
    @Test
    public void 배열_타입이_base에만_있는_경우() {
        // baseSchemas 생성
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        userTypeCnts.getTypeCounts().put("tags:array.string", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (배열 타입이 없음)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "배열 타입이 base에만 있으면 false여야 합니다.");
    }

    /**
     * 배열 타입이 target에만 있는 경우
     * Expect: false 반환
     */
    @Test
    public void 배열_타입이_target에만_있는_경우() {
        // baseSchemas 생성 (배열 타입이 없음)
        Map<String, SchemaComparator.TypeCnts> baseSchemas = new HashMap<>();
        SchemaComparator.TypeCnts userTypeCnts = new SchemaComparator.TypeCnts();
        userTypeCnts.getTypeCounts().put("name:string", 1);
        baseSchemas.put("User", userTypeCnts);

        // targetSchemas 생성 (배열 타입이 있음)
        Map<String, SchemaComparator.TypeCnts> targetSchemas = new HashMap<>();
        SchemaComparator.TypeCnts targetUserTypeCnts = new SchemaComparator.TypeCnts();
        targetUserTypeCnts.getTypeCounts().put("name:string", 1);
        targetUserTypeCnts.getTypeCounts().put("tags:array.string", 1);
        targetSchemas.put("User", targetUserTypeCnts);

        Map<String, Boolean> results = schemaComparator.compareFlattenedSchemas(baseSchemas, targetSchemas);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get("User"), "배열 타입이 target에만 있으면 false여야 합니다.");
    }
}

