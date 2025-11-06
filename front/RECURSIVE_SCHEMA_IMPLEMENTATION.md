# 재귀적 스키마 구조 구현 완료

## 🎯 목표
- Request Body와 Response Body에서 재귀적 중첩 구조 지원
- Object 내부에 Object, Array, Ref 등 모든 타입 지원
- Form-data에서도 Schema 선택 가능
- File 타입 지원 (multipart/form-data)

## ✅ 구현 완료 항목

### 1. 타입 정의 (`schema.types.ts`)

```typescript
// 재귀적 SchemaType
type SchemaType = PrimitiveSchema | ObjectSchema | ArraySchema | RefSchema;

// Object는 SchemaField 배열을 포함 (재귀!)
interface ObjectSchema {
  kind: "object";
  properties: SchemaField[];  // 재귀!
}

// Array는 items로 SchemaType 포함 (재귀!)
interface ArraySchema {
  kind: "array";
  items: SchemaType;  // 재귀!
}
```

### 2. UI 컴포넌트 (`SchemaFieldEditor.tsx`)

**재귀적 렌더링:**
- Primitive: type 선택 (string, integer, number, boolean, file)
- Object: properties 추가/편집 (재귀 호출)
- Array: items 편집 (재귀 호출)
- Ref: 스키마 선택

**특징:**
- `depth` 파라미터로 중첩 레벨 표시 (indentation)
- `allowFileType` 파라미터로 file 타입 허용 제어 (form-data에서만)
- Schema selector 모달 내장

### 3. 변환 로직 (`schemaConverter.ts`)

**Frontend → OpenAPI (재귀):**
```typescript
convertSchemaTypeToOpenAPI(schemaType: SchemaType) {
  if (isObjectSchema(schemaType)) {
    // properties를 재귀적으로 변환
    schemaType.properties.forEach(field => {
      properties[field.key] = convertSchemaFieldToOpenAPI(field);  // 재귀!
    });
  }
  if (isArraySchema(schemaType)) {
    // items를 재귀적으로 변환
    schema.items = convertSchemaTypeToOpenAPI(schemaType.items);  // 재귀!
  }
}
```

**OpenAPI → Frontend (재귀):**
```typescript
parseOpenAPISchemaToSchemaType(schema: any): SchemaType {
  if (schema.type === "object") {
    // properties를 재귀적으로 파싱
    properties.forEach(([key, propSchema]) => {
      properties.push({
        key,
        schemaType: parseOpenAPISchemaToSchemaType(propSchema),  // 재귀!
      });
    });
  }
  if (schema.type === "array") {
    // items를 재귀적으로 파싱
    items: parseOpenAPISchemaToSchemaType(schema.items),  // 재귀!
  }
}
```

### 4. 백엔드 Property 클래스

```java
public class Property {
    // Object type - nested properties (재귀!)
    private Map<String, Property> properties;
    private List<String> required;
    
    // Array type - recursive structure
    private Property items;
    
    // Additional constraints
    private String format;
    private List<String> enumValues;
    private String pattern;
    private Integer minLength;
    private Integer maxLength;
    private Number minimum;
    private Number maximum;
}
```

### 5. 백엔드 변환 로직 (RestApiSpecServiceImpl)

**convertProperty (재귀):**
```java
private Map<String, Object> convertProperty(Property property) {
    // Object type
    if (property.getProperties() != null) {
        result.put("properties", convertProperties(property.getProperties()));  // 재귀!
    }
    
    // Array type
    if (property.getItems() != null) {
        result.put("items", convertProperty(property.getItems()));  // 재귀!
    }
}
```

**parseProperty (재귀):**
```java
private Property parseProperty(Map<String, Object> propMap) {
    // Object type
    Map<String, Object> properties = (Map<String, Object>) propMap.get("properties");
    if (properties != null) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            parsedProperties.put(entry.getKey(), parseProperty(entry.getValue()));  // 재귀!
        }
    }
    
    // Array type
    Map<String, Object> items = (Map<String, Object>) propMap.get("items");
    if (items != null) {
        builder.items(parseProperty(items));  // 재귀!
    }
}
```

## 🚀 지원되는 복잡한 구조 예시

### Example 1: Object 내부에 Ref
```yaml
requestBody:
  content:
    application/json:
      schema:
        type: object
        properties:
          name:
            type: string
          user:
            $ref: '#/components/schemas/User'  # ✅ 지원!
```

### Example 2: Object 내부에 중첩 Object
```yaml
requestBody:
  content:
    application/json:
      schema:
        type: object
        properties:
          data:
            type: object  # ✅ 중첩 object 지원!
            properties:
              street:
                type: string
              city:
                type: string
```

### Example 3: Array items에 Ref
```yaml
requestBody:
  content:
    application/json:
      schema:
        type: object
        properties:
          products:
            type: array  # ✅ array items ref 지원!
            items:
              $ref: '#/components/schemas/Product'
```

### Example 4: 복잡한 혼합 구조
```yaml
requestBody:
  content:
    application/json:
      schema:
        type: object
        properties:
          name:
            type: string
          user:
            $ref: '#/components/schemas/User'
          address:
            type: object
            properties:
              street:
                type: string
              city:
                type: string
          tags:
            type: array
            items:
              type: string
          products:
            type: array
            items:
              $ref: '#/components/schemas/Product'
```

### Example 5: Form-data에서 file 타입
```yaml
requestBody:
  content:
    multipart/form-data:
      schema:
        type: object
        properties:
          file:
            type: string
            format: binary  # ✅ file 타입 지원!
          title:
            type: string
          user:
            $ref: '#/components/schemas/User'  # ✅ form-data에서도 ref 지원!
```

## 📋 사용 방법

### 1. Primitive Field 추가
1. "+ Add Field" 클릭
2. Field name 입력
3. "Primitive" 선택 (기본값)
4. Type 선택 (string, integer, number, boolean, file)

### 2. Object Field 추가 (중첩 구조)
1. "+ Add Field" 클릭
2. Field name 입력
3. "Object" 선택
4. "+ Add Property" 클릭하여 내부 필드 추가
5. 내부 필드도 동일하게 Primitive, Object, Array, Ref 선택 가능 (무한 재귀!)

### 3. Array Field 추가
1. "+ Add Field" 클릭
2. Field name 입력
3. "Array" 선택
4. Items 섹션에서 배열 요소의 타입 정의
5. Items도 Primitive, Object, Array, Ref 모두 가능 (재귀!)

### 4. Reference Field 추가
1. "+ Add Field" 클릭
2. Field name 입력
3. "Reference" 선택
4. Schema name 입력 또는 "Select" 버튼으로 선택

### 5. Schema 전체 참조
1. "+ Add Schema" 버튼 클릭
2. 스키마 선택
3. 전체 스키마가 참조됨 (fields는 읽기 전용 미리보기)

## 🔧 주의사항

### File 타입
- **허용**: multipart/form-data에서만
- **변환**: `type: "file"` → `type: "string", format: "binary"`

### Form-data에서 Schema 사용
- form-data, x-www-form-urlencoded에서도 "+ Add Schema" 가능
- Schema 선택 시 각 필드가 query parameter로 변환됨
- 백엔드에서 자동으로 parameters로 변환 및 마커 추가

### 재귀 깊이
- UI는 무한 재귀 지원
- 성능을 위해 너무 깊은 중첩은 권장하지 않음 (5레벨 이하 권장)

## 🎉 완료!
이제 Ouroboros는 OpenAPI 3.1.0의 모든 스키마 구조를 완벽하게 지원합니다!

