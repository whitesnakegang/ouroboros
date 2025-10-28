# Schema Management API Specification

Base URL: `/ouro/rest-specs/schemas`

## Overview
Manages reusable schema definitions in the OpenAPI `components/schemas` section. These schemas can be referenced by REST API specifications for request/response body definitions.

---

## Endpoints

### 1. Create Schema

**POST** `/ouro/rest-specs/schemas`

Creates a new reusable schema definition.

#### Request Body
```json
{
  "schemaName": "User",
  "type": "object",
  "title": "User Schema",
  "description": "A user entity",
  "properties": {
    "id": {
      "type": "string",
      "description": "User ID",
      "mockExpression": "{{$random.uuid}}"
    },
    "name": {
      "type": "string",
      "description": "User name",
      "mockExpression": "{{$name.fullName}}"
    },
    "email": {
      "type": "string",
      "description": "User email",
      "mockExpression": "{{$internet.email}}"
    }
  },
  "required": ["id", "name"],
  "orders": ["id", "name", "email"],
  "xmlName": "user"
}
```

#### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| schemaName | string | Yes | Schema identifier (used for $ref references) |
| type | string | No | Schema type (default: "object") |
| title | string | No | Human-readable title |
| description | string | No | Schema description |
| properties | object | No | Property definitions (key: property name, value: Property object) |
| required | string[] | No | List of required property names |
| orders | string[] | No | Custom field ordering (Ouroboros extension) |
| xmlName | string | No | XML root element name |

#### Property Object Structure
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| ref | string | No | **Schema reference** (e.g., "Address") - simply provide the schema name |
| type | string | No* | Property type (string, integer, number, boolean, array, object). *Required if not using ref |
| description | string | No | Property description |
| mockExpression | string | No | Mock data generation expression (Ouroboros extension) |
| items | Property | No | Item definition for array types (recursive, supports ref too) |
| minItems | integer | No | Minimum array length (array types only) |
| maxItems | integer | No | Maximum array length (array types only) |

**Note:** When using `ref`, all other fields except `description` are ignored (reference mode). The server automatically converts `ref` to OpenAPI's `$ref` format.

#### Response (Success - 200)
```json
{
  "status": 200,
  "data": {
    "schemaName": "User",
    "type": "object",
    "title": "User Schema",
    "description": "A user entity",
    "properties": {
      "id": {
        "type": "string",
        "description": "User ID",
        "mockExpression": "{{$random.uuid}}"
      },
      "name": {
        "type": "string",
        "description": "User name",
        "mockExpression": "{{$name.fullName}}"
      },
      "email": {
        "type": "string",
        "description": "User email",
        "mockExpression": "{{$internet.email}}"
      }
    },
    "required": ["id", "name"],
    "orders": ["id", "name", "email"]
  },
  "message": "Schema created successfully"
}
```

#### Response (Error - 400)
```json
{
  "status": 400,
  "message": "Failed to create schema",
  "error": {
    "code": "INVALID_REQUEST",
    "details": "Schema 'User' already exists"
  }
}
```

---

### 2. Get All Schemas

**GET** `/ouro/rest-specs/schemas`

Retrieves all schema definitions.

#### Response (Success - 200)
```json
{
  "status": 200,
  "data": [
    {
      "schemaName": "User",
      "type": "object",
      "title": "User Schema",
      "description": "A user entity",
      "properties": { ... },
      "required": ["id", "name"],
      "orders": ["id", "name", "email"]
    },
    {
      "schemaName": "Book",
      "type": "object",
      "title": "Book Schema",
      "description": "A book entity",
      "properties": { ... },
      "required": ["isbn", "title"]
    }
  ],
  "message": "Schemas retrieved successfully"
}
```

#### Response (Error - 404)
```json
{
  "status": 404,
  "message": "No schemas found",
  "error": {
    "code": "SCHEMA_NOT_FOUND",
    "details": "No schema definitions exist yet. Create your first schema to get started."
  }
}
```

---

### 3. Get Schema by Name

**GET** `/ouro/rest-specs/schemas/{schemaName}`

Retrieves a specific schema definition by name.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| schemaName | string | Schema identifier |

#### Response (Success - 200)
```json
{
  "status": 200,
  "data": {
    "schemaName": "User",
    "type": "object",
    "title": "User Schema",
    "description": "A user entity",
    "properties": {
      "id": {
        "type": "string",
        "description": "User ID",
        "mockExpression": "{{$random.uuid}}"
      }
    },
    "required": ["id", "name"]
  },
  "message": "Schema retrieved successfully"
}
```

#### Response (Error - 404)
```json
{
  "status": 404,
  "message": "Schema not found",
  "error": {
    "code": "SCHEMA_NOT_FOUND",
    "details": "Schema 'NonExistent' not found"
  }
}
```

---

### 4. Update Schema

**PUT** `/ouro/rest-specs/schemas/{schemaName}`

Updates an existing schema definition. Only provided fields will be updated.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| schemaName | string | Schema identifier |

#### Request Body
```json
{
  "type": "object",
  "title": "Updated User Schema",
  "description": "An updated user entity with additional fields",
  "properties": {
    "id": {
      "type": "string",
      "description": "User ID",
      "mockExpression": "{{$random.uuid}}"
    },
    "name": {
      "type": "string",
      "description": "User name",
      "mockExpression": "{{$name.fullName}}"
    },
    "email": {
      "type": "string",
      "description": "User email",
      "mockExpression": "{{$internet.email}}"
    },
    "age": {
      "type": "integer",
      "description": "User age",
      "mockExpression": "{{$number.numberBetween(18,80)}}"
    }
  },
  "required": ["id", "name", "email"],
  "orders": ["id", "name", "email", "age"]
}
```

#### Request Fields (All Optional)
| Field | Type | Description |
|-------|------|-------------|
| type | string | Schema type |
| title | string | Human-readable title |
| description | string | Schema description |
| properties | object | Property definitions |
| required | string[] | List of required property names |
| orders | string[] | Custom field ordering |
| xmlName | string | XML root element name |

#### Response (Success - 200)
```json
{
  "status": 200,
  "data": {
    "schemaName": "User",
    "type": "object",
    "title": "Updated User Schema",
    "description": "An updated user entity with additional fields",
    "properties": { ... },
    "required": ["id", "name", "email"],
    "orders": ["id", "name", "email", "age"]
  },
  "message": "Schema updated successfully"
}
```

#### Response (Error - 404)
```json
{
  "status": 404,
  "message": "Schema not found",
  "error": {
    "code": "SCHEMA_NOT_FOUND",
    "details": "Schema 'NonExistent' not found"
  }
}
```

---

### 5. Delete Schema

**DELETE** `/ouro/rest-specs/schemas/{schemaName}`

Deletes a schema definition.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| schemaName | string | Schema identifier |

#### Response (Success - 200)
```json
{
  "status": 200,
  "message": "Schema deleted successfully"
}
```

#### Response (Error - 404)
```json
{
  "status": 404,
  "message": "Schema not found",
  "error": {
    "code": "SCHEMA_NOT_FOUND",
    "details": "Schema 'NonExistent' not found"
  }
}
```

---

## Common Error Responses

### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Failed to [operation] schema",
  "error": {
    "code": "INTERNAL_ERROR",
    "details": "An internal error occurred while [operation] the schema"
  }
}
```

---

## Array Type Example

Schema with array property:

```json
{
  "schemaName": "Book",
  "type": "object",
  "title": "Book Schema",
  "description": "A book entity",
  "properties": {
    "isbn": {
      "type": "string",
      "description": "Book ISBN",
      "mockExpression": "{{$number.digits(13)}}"
    },
    "title": {
      "type": "string",
      "description": "Book title",
      "mockExpression": "{{$book.title}}"
    },
    "tags": {
      "type": "array",
      "description": "Book tags",
      "items": {
        "type": "string"
      },
      "minItems": 1,
      "maxItems": 5
    }
  },
  "required": ["isbn", "title"],
  "orders": ["isbn", "title", "tags"]
}
```

---

## Nested Schema References

### Creating Nested Schemas

Schemas can reference other schemas using the simplified `ref` field. This enables complex nested data structures:

```json
{
  "schemaName": "UserWithAddress",
  "type": "object",
  "title": "User with Address",
  "description": "A user entity with nested address",
  "properties": {
    "id": {
      "type": "string",
      "description": "User ID",
      "mockExpression": "{{$random.uuid}}"
    },
    "name": {
      "type": "string",
      "description": "User name",
      "mockExpression": "{{$name.fullName}}"
    },
    "address": {
      "ref": "Address",
      "description": "User's address (references Address schema)"
    },
    "previousAddresses": {
      "type": "array",
      "description": "Previous addresses",
      "items": {
        "ref": "Address"
      },
      "minItems": 0,
      "maxItems": 5
    }
  },
  "required": ["id", "name"],
  "orders": ["id", "name", "address", "previousAddresses"]
}
```

**Important:** Make sure to create the referenced schema (e.g., "Address") before creating schemas that reference it.

---

## Schema Reference in REST API Specs

Use the simplified `ref` field to reference schemas:

```json
{
  "path": "/api/users",
  "method": "POST",
  "requestBody": {
    "required": true,
    "content": {
      "application/json": {
        "schema": {
          "ref": "User"
        }
      }
    }
  },
  "responses": {
    "201": {
      "description": "User created successfully",
      "content": {
        "application/json": {
          "schema": {
            "ref": "User"
          }
        }
      }
    }
  }
}
```

### How It Works

- **Input:** Send `"ref": "User"` (just the schema name)
- **Processing:** Server automatically converts to `"$ref": "#/components/schemas/User"`
- **Storage:** Saved as standard OpenAPI format in `ourorest.yml`
- **Response:** Returns simplified `"ref": "User"` format

---

## Notes

- Schema names must be unique
- Null fields are excluded from JSON responses
- Array type properties must include `items` definition
- Mock expressions follow Faker.js syntax
- Custom field ordering (`orders`) is an Ouroboros extension
- XML name mapping is optional and used for XML serialization
- **Schema references:** Use `"ref": "SchemaName"` to reference other schemas (no need for verbose `$ref` format)
- **Nested schema support:** Properties can reference other schemas using `ref` field
- **Recursive structures:** Supported through schema references (be careful with circular references)
