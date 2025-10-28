# Backend RequestBody 구조 요약

## RequestBody 구조

```typescript
interface RequestBody {
  description: string; // 선택
  required: boolean; // 선택 (기본: false)
  content: Map<string, MediaType>; // content-type별 스키마
}
```

## MediaType 구조

```typescript
interface MediaType {
  schema: Schema;
}
```

## Schema 구조

```typescript
interface Schema {
  type: string; // "object" | "string" | "array"
  title: string; // 선택
  description: string; // 선택
  properties: Map<string, Property>; // object 타입일 때만
  required: string[]; // 필수 필드 목록
  orders: string[]; // 필드 순서
  xmlName: string; // XML 전용
}
```

## Property 구조

```typescript
interface Property {
  type: string; // "string" | "integer" | "number" | "boolean" | "object" | "array"
  description: string; // 선택
  mockExpression: string; // 선택
  items: Property; // array 타입일 때, 요소의 스키마
  minItems: number; // array 최소 길이
  maxItems: number; // array 최대 길이
}
```

## 프론트엔드에서 입력해야 할 값들

### Content-Type 선택

- application/json
- application/xml
- text/plain
- application/x-www-form-urlencoded

### Schema 정의 (object 타입일 때)

표 형식:

- Property Name (필드명)
- Type (string | integer | number | boolean | object | array)
- Description (선택)
- Mock Expression (선택)
- Required 체크박스 (선택)
- Orders 순서 (선택)

### Array 타입

- items의 Property 정의 (재귀)
- minItems, maxItems

---

**결론**: raw 입력보다는 schema definition을 표 형식으로 받는 것이 더 적합합니다.
