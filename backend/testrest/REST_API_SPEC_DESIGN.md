# REST API Specification CRUD Design

Base URL: `/ouro/rest-specs`

## Overview
Manages REST API endpoint specifications with support for schema references. API specs can reference schemas from `components/schemas` or define schemas inline.

---

## Schema Reference Support

### Two Schema Definition Methods

#### 1. Schema Reference (Recommended)

Use the simple `ref` field to reference schemas:

```json
{
  "schema": {
    "ref": "User"
  }
}
```

**How it works:**
- **Input:** Send `"ref": "User"` (just the schema name)
- **Processing:** Server auto-converts to OpenAPI's `$ref` format
- **Storage:** Saved as standard OpenAPI in YAML
- **Response:** Returns simplified `"ref": "User"` format

**Advantages of Schema References:**
- Reusability across multiple API specs
- Centralized schema management
- Easier maintenance and updates
- Single source of truth
- Clean and simple syntax

#### 2. Inline Schema Definition
Defines schema directly in the API spec:

```json
{
  "schema": {
    "type": "object",
    "properties": {
      "id": { "type": "string" },
      "name": { "type": "string" }
    },
    "required": ["id", "name"]
  }
}
```

**Use cases:**
- Quick prototyping
- Simple one-off schemas
- Unique response structures

**Note:** Inline schemas also support nested schema references using the simplified `ref` format!

---

## Endpoints

### 1. Create REST API Specification

**POST** `/ouro/rest-specs`

Creates a new REST API endpoint specification.

#### Example 1: Using Schema Reference (Recommended)

```json
{
  "path": "/api/users",
  "method": "POST",
  "summary": "Create a new user",
  "description": "Creates a new user in the system",
  "tags": ["Users"],
  "requestBody": {
    "description": "User object to be created",
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
    },
    "400": {
      "description": "Invalid input"
    }
  },
  "progress": "mock",
  "tag": "implementing"
}
```

**Note:** Simply use the schema name - no need for verbose `$ref` paths!

#### Example 2: Using Inline Schema

```json
{
  "path": "/api/users/{id}",
  "method": "GET",
  "summary": "Get user by ID",
  "description": "Retrieves a single user by their ID",
  "tags": ["Users"],
  "parameters": [
    {
      "name": "id",
      "in": "path",
      "description": "User ID",
      "required": true,
      "schema": {
        "type": "string"
      }
    }
  ],
  "responses": {
    "200": {
      "description": "User found",
      "content": {
        "application/json": {
          "schema": {
            "type": "object",
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
              }
            },
            "required": ["id", "name"]
          }
        }
      }
    },
    "404": {
      "description": "User not found"
    }
  },
  "progress": "completed",
  "tag": "none"
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string | No | Unique identifier (UUID generated if not provided) |
| path | string | Yes | API endpoint path (e.g., "/api/users/{id}") |
| method | string | Yes | HTTP method (GET, POST, PUT, DELETE, PATCH) |
| summary | string | No | Brief description of the endpoint |
| description | string | No | Detailed description |
| deprecated | boolean | No | Whether this endpoint is deprecated (default: false) |
| tags | string[] | No | Grouping tags for organization |
| parameters | Parameter[] | No | Query/path/header parameters |
| requestBody | RequestBody | No | Request body specification |
| responses | object | No | Response definitions (key: status code, value: ApiResponse) |
| security | SecurityRequirement[] | No | Security requirements |
| progress | string | No | Development progress ("mock" or "completed", default: "mock") |
| tag | string | No | Development tag ("none", "implementing", "bugfix", default: "none") |
| isValid | boolean | No | Validation flag (default: true) |

#### Parameter Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Parameter name |
| in | string | Yes | Parameter location ("path", "query", "header", "cookie") |
| description | string | No | Parameter description |
| required | boolean | No | Whether parameter is required |
| schema | Schema | No | Parameter schema (usually simple types) |

#### RequestBody Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| description | string | No | Request body description |
| required | boolean | No | Whether request body is required |
| content | object | Yes | Content type mapping (key: media type, value: MediaType) |

#### MediaType Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| schema | Schema | Yes | Schema definition (can use $ref or inline) |

#### Schema Object (Two Modes)

**Reference Mode:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| $ref | string | Yes | Reference path (e.g., "#/components/schemas/User") |

**Inline Mode:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| type | string | Yes | Schema type |
| title | string | No | Schema title |
| description | string | No | Schema description |
| properties | object | No | Property definitions |
| required | string[] | No | Required properties |
| orders | string[] | No | Field ordering (Ouroboros extension) |
| xmlName | string | No | XML root element name |

#### ApiResponse Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| description | string | Yes | Response description |
| content | object | No | Content type mapping |
| headers | object | No | Response headers |

---

### 2. Get All REST API Specifications

**GET** `/ouro/rest-specs`

Retrieves all REST API specifications.

#### Query Parameters (Future Enhancement)
| Parameter | Type | Description |
|-----------|------|-------------|
| tag | string | Filter by tag (Users, Books, etc.) |
| method | string | Filter by HTTP method |
| progress | string | Filter by progress (mock, completed) |

#### Response Example
```json
{
  "status": 200,
  "data": [
    {
      "id": "64e9b87f-6ae6-438d-a558-bb6fab8548f0",
      "path": "/api/users",
      "method": "POST",
      "summary": "Create a new user",
      "tags": ["Users"],
      "progress": "mock",
      "tag": "implementing"
    },
    {
      "id": "7f8a9c0d-1b2e-4f3a-9d8e-5c6b7a8d9e0f",
      "path": "/api/users/{id}",
      "method": "GET",
      "summary": "Get user by ID",
      "tags": ["Users"],
      "progress": "completed",
      "tag": "none"
    }
  ],
  "message": "REST API specifications retrieved successfully"
}
```

---

### 3. Get REST API Specification by ID

**GET** `/ouro/rest-specs/{id}`

Retrieves a specific REST API specification.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| id | string | Specification UUID |

#### Response Example
```json
{
  "status": 200,
  "data": {
    "id": "64e9b87f-6ae6-438d-a558-bb6fab8548f0",
    "path": "/api/users",
    "method": "POST",
    "summary": "Create a new user",
    "description": "Creates a new user in the system",
    "tags": ["Users"],
    "requestBody": {
      "description": "User object to be created",
      "required": true,
      "content": {
        "application/json": {
          "schema": {
            "$ref": "#/components/schemas/User"
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
              "$ref": "#/components/schemas/User"
            }
          }
        }
      }
    },
    "progress": "mock",
    "tag": "implementing",
    "isValid": true
  },
  "message": "REST API specification retrieved successfully"
}
```

---

### 4. Update REST API Specification

**PUT** `/ouro/rest-specs/{id}`

Updates an existing REST API specification.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| id | string | Specification UUID |

#### Request Body
Same structure as Create, but all fields are optional. Only provided fields will be updated.

---

### 5. Delete REST API Specification

**DELETE** `/ouro/rest-specs/{id}`

Deletes a REST API specification.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| id | string | Specification UUID |

#### Response
```json
{
  "status": 200,
  "message": "REST API specification deleted successfully"
}
```

---

## Workflow Example

### Complete Workflow: Creating API with Schema Reference

1. **Create reusable schema**
```bash
POST /ouro/rest-specs/schemas
{
  "schemaName": "User",
  "type": "object",
  "properties": {
    "id": { "type": "string" },
    "name": { "type": "string" },
    "email": { "type": "string" }
  },
  "required": ["id", "name"]
}
```

2. **Create API spec referencing the schema**
```bash
POST /ouro/rest-specs
{
  "path": "/api/users",
  "method": "POST",
  "summary": "Create user",
  "requestBody": {
    "required": true,
    "content": {
      "application/json": {
        "schema": {
          "$ref": "#/components/schemas/User"
        }
      }
    }
  },
  "responses": {
    "201": {
      "description": "User created",
      "content": {
        "application/json": {
          "schema": {
            "$ref": "#/components/schemas/User"
          }
        }
      }
    }
  }
}
```

3. **View generated OpenAPI YAML**
The system automatically generates:
```yaml
openapi: 3.1.0
paths:
  /api/users:
    post:
      summary: Create user
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/User'
      responses:
        '201':
          description: User created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        email:
          type: string
      required:
        - id
        - name
```

---

## Error Responses

### 400 Bad Request
```json
{
  "status": 400,
  "message": "Failed to create REST API specification",
  "error": {
    "code": "INVALID_REQUEST",
    "details": "Path and method are required"
  }
}
```

### 404 Not Found
```json
{
  "status": 404,
  "message": "REST API specification not found",
  "error": {
    "code": "SPEC_NOT_FOUND",
    "details": "Specification with ID 'xxx' not found"
  }
}
```

### 409 Conflict
```json
{
  "status": 409,
  "message": "Failed to create REST API specification",
  "error": {
    "code": "DUPLICATE_API",
    "details": "API specification already exists for POST /api/users"
  }
}
```

### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Failed to create REST API specification",
  "error": {
    "code": "INTERNAL_ERROR",
    "details": "An internal error occurred"
  }
}
```

---

## Notes

- **Schema Reference Format:** `#/components/schemas/{schemaName}`
- **Duplicate Detection:** Based on path + method combination
- **UUID Generation:** Automatic if not provided
- **Progress Values:** "mock" (default) or "completed"
- **Tag Values:** "none" (default), "implementing", or "bugfix"
- **Null Fields:** Excluded from JSON responses via `@JsonInclude(NON_NULL)`
- **OpenAPI Compliance:** Fully compliant with OpenAPI 3.1.0 specification