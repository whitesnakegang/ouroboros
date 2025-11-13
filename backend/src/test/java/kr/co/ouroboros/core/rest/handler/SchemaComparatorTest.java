package kr.co.ouroboros.core.rest.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * SchemaComparator의 flattenSchemas 메서드 단위 테스트
 */
public class SchemaComparatorTest {

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
     * 일반 필드만 있는 경우
     * Expect: 각 필드의 타입이 정확히 카운트됨
     */
    @Test
    public void 일반_필드만_있는_경우() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    age:
                      type: integer
                      format: int32
                    height:
                      type: number
                    isActive:
                      type: boolean
                    file:
                      type: string
                      format: binary
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        assertNotNull(result);
        assertTrue(result.containsKey("User"), "User 스키마가 있어야 합니다.");

        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> typeCounts = userTypeCnts.getTypeCounts();

        assertEquals(1, typeCounts.getOrDefault("name:string", 0), "name:string 1개");
        assertEquals(1, typeCounts.getOrDefault("age:integer", 0), "age:integer 1개");
        assertEquals(1, typeCounts.getOrDefault("height:number", 0), "height:number 1개");
        assertEquals(1, typeCounts.getOrDefault("isActive:boolean", 0), "isActive:boolean 1개");
        assertEquals(1, typeCounts.getOrDefault("file:binary", 0), "file:binary 1개");
    }

    /**
     * 객체 안에 객체가 있는 경우 ($ref 사용)
     * Expect: 하위 객체의 필드가 상위 객체에 포함됨 (prefix 없이)
     */
    @Test
    public void 객체_안에_객체가_있는_경우() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Address:
                  type: object
                  properties:
                    roadAddress:
                      type: string
                    num:
                      type: integer
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    address:
                      $ref: "#/components/schemas/Address"
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        // Address 스키마 확인
        SchemaComparator.TypeCnts addressTypeCnts = result.get("Address");
        assertNotNull(addressTypeCnts);
        Map<String, Integer> addressTypeCounts = addressTypeCnts.getTypeCounts();
        assertEquals(1, addressTypeCounts.getOrDefault("roadAddress:string", 0));
        assertEquals(1, addressTypeCounts.getOrDefault("num:integer", 0));

        // User 스키마 확인: name + Address의 필드들 (prefix 없이)
        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> userTypeCounts = userTypeCnts.getTypeCounts();
        
        assertEquals(1, userTypeCounts.getOrDefault("name:string", 0), "User의 직접 필드");
        assertEquals(1, userTypeCounts.getOrDefault("roadAddress:string", 0), "Address의 필드가 포함됨");
        assertEquals(1, userTypeCounts.getOrDefault("num:integer", 0), "Address의 필드가 포함됨");
    }

    /**
     * 객체 안에 객체 안에 객체가 있는 경우 (3단계 중첩)
     * Expect: 모든 하위 객체의 필드가 최상위 객체에 포함됨
     */
    @Test
    public void 객체_안에_객체_안에_객체가_있는_경우() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                City:
                  type: object
                  properties:
                    name:
                      type: string
                    code:
                      type: integer
                Address:
                  type: object
                  properties:
                    roadAddress:
                      type: string
                    city:
                      $ref: "#/components/schemas/City"
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    address:
                      $ref: "#/components/schemas/Address"
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        // User 스키마 확인: name + Address의 필드들 + City의 필드들 (모두 prefix 없이)
        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> userTypeCounts = userTypeCnts.getTypeCounts();
        
        // User의 name과 City의 name이 합쳐져서 2가 됨
        assertEquals(2, userTypeCounts.getOrDefault("name:string", 0), "User의 name + City의 name = 2개");
        assertEquals(1, userTypeCounts.getOrDefault("roadAddress:string", 0), "Address의 필드");
        assertEquals(1, userTypeCounts.getOrDefault("code:integer", 0), "City의 필드");
    }

    /**
     * 객체 안에 배열이 있는 경우 (기본 타입)
     * Expect: "propertyName:array.type" 형식으로 저장
     */
    @Test
    public void 객체_안에_배열이_있는_경우_기본타입() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    tags:
                      type: array
                      items:
                        type: string
                    scores:
                      type: array
                      items:
                        type: integer
                        format: int32
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> userTypeCounts = userTypeCnts.getTypeCounts();
        
        assertEquals(1, userTypeCounts.getOrDefault("name:string", 0), "일반 필드");
        assertEquals(1, userTypeCounts.getOrDefault("tags:array.string", 0), "배열 타입 (기본 타입)");
        assertEquals(1, userTypeCounts.getOrDefault("scores:array.integer", 0), "배열 타입 (기본 타입)");
    }

    /**
     * 객체 안에 배열이 있는 경우 ($ref 사용)
     * Expect: "propertyName:array.SchemaName" 형식으로 저장
     */
    @Test
    public void 객체_안에_배열이_있는_경우_스키마참조() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Product:
                  type: object
                  properties:
                    productName:
                      type: string
                    price:
                      type: integer
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    products:
                      type: array
                      items:
                        $ref: "#/components/schemas/Product"
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> userTypeCounts = userTypeCnts.getTypeCounts();
        
        assertEquals(1, userTypeCounts.getOrDefault("name:string", 0), "일반 필드");
        assertEquals(1, userTypeCounts.getOrDefault("products:array.Product", 0), "배열 타입 ($ref)");
        
        // Product 스키마도 확인
        SchemaComparator.TypeCnts productTypeCnts = result.get("Product");
        assertNotNull(productTypeCnts);
        Map<String, Integer> productTypeCounts = productTypeCnts.getTypeCounts();
        assertEquals(1, productTypeCounts.getOrDefault("productName:string", 0));
        assertEquals(1, productTypeCounts.getOrDefault("price:integer", 0));
    }

    /**
     * 복합 케이스: 일반 필드 + 객체 참조 + 배열 (기본 타입) + 배열 ($ref)
     */
    @Test
    public void 복합_케이스() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                Address:
                  type: object
                  properties:
                    roadAddress:
                      type: string
                    num:
                      type: integer
                Product:
                  type: object
                  properties:
                    productName:
                      type: string
                    price:
                      type: integer
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    age:
                      type: integer
                    address:
                      $ref: "#/components/schemas/Address"
                    tags:
                      type: array
                      items:
                        type: string
                    products:
                      type: array
                      items:
                        $ref: "#/components/schemas/Product"
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> userTypeCounts = userTypeCnts.getTypeCounts();
        
        // 일반 필드
        assertEquals(1, userTypeCounts.getOrDefault("name:string", 0));
        assertEquals(1, userTypeCounts.getOrDefault("age:integer", 0));
        
        // 객체 참조 (Address의 필드가 포함됨)
        assertEquals(1, userTypeCounts.getOrDefault("roadAddress:string", 0));
        assertEquals(1, userTypeCounts.getOrDefault("num:integer", 0));
        
        // 배열 (기본 타입)
        assertEquals(1, userTypeCounts.getOrDefault("tags:array.string", 0));
        
        // 배열 ($ref)
        assertEquals(1, userTypeCounts.getOrDefault("products:array.Product", 0));
    }

    /**
     * inline 객체 (properties 직접 정의)
     * Expect: 하위 객체의 필드가 상위 객체에 포함됨
     */
    @Test
    public void inline_객체_처리() throws Exception {
        String yamlContent = """
            openapi: 3.1.0
            info:
              title: Test API
              version: 1.0.0
            components:
              schemas:
                User:
                  type: object
                  properties:
                    name:
                      type: string
                    address:
                      type: object
                      properties:
                        roadAddress:
                          type: string
                        num:
                          type: integer
            """;

        Components components = parseComponentsFromYaml(yamlContent);
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(components);

        SchemaComparator.TypeCnts userTypeCnts = result.get("User");
        assertNotNull(userTypeCnts);
        Map<String, Integer> userTypeCounts = userTypeCnts.getTypeCounts();
        
        assertEquals(1, userTypeCounts.getOrDefault("name:string", 0), "User의 직접 필드");
        assertEquals(1, userTypeCounts.getOrDefault("roadAddress:string", 0), "inline 객체의 필드");
        assertEquals(1, userTypeCounts.getOrDefault("num:integer", 0), "inline 객체의 필드");
    }

    /**
     * null Components 처리
     */
    @Test
    public void null_Components_처리() {
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty(), "null Components는 빈 결과를 반환해야 합니다.");
    }

    /**
     * 빈 Components 처리
     */
    @Test
    public void 빈_Components_처리() {
        Components emptyComponents = new Components();
        emptyComponents.setSchemas(new java.util.HashMap<>());
        
        Map<String, SchemaComparator.TypeCnts> result = schemaComparator.flattenSchemas(emptyComponents);
        
        assertNotNull(result);
        assertTrue(result.isEmpty(), "빈 Components는 빈 결과를 반환해야 합니다.");
    }
}
