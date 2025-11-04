# 🐍 Ouroboros

<div align="center">

![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)
![React](https://img.shields.io/badge/React-19.1-61DAFB.svg)

**OpenAPI 3.1.0-based REST API Specification Management & Mock Server Library**

**English** | [한국어](./docs/ko/README.md)

[Getting Started](#-quick-start) • [Documentation](#-documentation) • [Contributing](./CONTRIBUTING.md) • [License](#-license)

</div>

---

## 📖 Table of Contents

- [Introduction](#-introduction)
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Usage](#-usage)
- [Documentation](#-documentation)
- [Contributing](#-contributing)
- [License](#-license)
- [Team](#-team)

---

## 🎯 Introduction

**Ouroboros** is a Spring Boot library that revolutionizes the REST API development lifecycle. Based on the OpenAPI 3.1.0 standard, it manages API specifications, automatically generates mock servers, and provides API validation and testing capabilities.

### Why Ouroboros?

- **Spec-First Development**: Write OpenAPI specs first, implement later
- **Ready-to-Use Mock Server**: Frontend development doesn't need to wait for backend
- **Automatic Validation**: Automatically verify consistency between implementation and specification
- **Developer-Friendly**: Intuitive web UI and RESTful API
- **Lightweight Library**: Simply add to existing Spring Boot applications

---

## ✨ Features

### 🔧 API Specification Management
- ✅ **Full OpenAPI 3.1.0 Support**: Compliant with the latest OpenAPI standard
- ✅ **CRUD Operations**: Create, Read, Update, Delete REST API specifications
- ✅ **Schema Reusability**: Reference schemas via `$ref` and eliminate duplication
- ✅ **YAML Import/Export**: Import and export external OpenAPI files
- ✅ **Duplicate Detection**: Automatic validation of path + method combinations
- ✅ **Version Management**: Track API progress status (mock, implementing, completed)

### 🎭 Automatic Mock Server
- ✅ **Immediately Available**: Mock APIs generated as soon as specs are written
- ✅ **Realistic Data**: Integrated with DataFaker for realistic mock data generation
- ✅ **Request Validation**: Automatic validation of parameters, headers, and body
- ✅ **Multiple Format Support**: JSON, XML, Form Data, etc.
- ✅ **Custom Mock Expressions**: Fine-grained control with `x-ouroboros-mock` field

### 🖥️ Web Interface
- ✅ **React-based Modern UI**: Intuitive and responsive web interface
- ✅ **Real-time Preview**: Instantly view API specification changes
- ✅ **Code Snippet Generation**: Various languages including cURL, JavaScript, Python
- ✅ **Markdown Export**: Automatic API documentation generation

### 🔍 Validation & QA
- ✅ **Spec Validation**: Verify OpenAPI standard compliance
- ✅ **Implementation Comparison**: Sync code and specs with `@ApiState` annotation
- ✅ **Automatic Enrichment**: Automatically add missing Ouroboros extension fields
- ✅ **Error Reporting**: Detailed validation error messages
- ✅ **Try Feature**: API execution tracking and analysis (📖 [Setup Guide](../../backend/src/main/java/kr/co/ouroboros/core/rest/tryit/OUROBOROS_TRY_SETUP.md))

---

## 🏗️ Architecture

### Overall Structure

```
┌──────────────────────────────────────────────────────────────┐
│                      User Application                         │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                  Spring Boot App                        │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │  │
│  │  │ Controllers  │  │   Services   │  │   Models    │  │  │
│  │  │  @ApiState   │  │              │  │             │  │  │
│  │  └──────────────┘  └──────────────┘  └─────────────┘  │  │
│  └────────────────────────────────────────────────────────┘  │
│                              │                                │
│                 ┌────────────▼────────────┐                  │
│                 │  Ouroboros Library      │                  │
│                 │  ┌──────────────────┐   │                  │
│                 │  │  Auto Config     │   │                  │
│                 │  ├──────────────────┤   │                  │
│                 │  │  Mock Filter     │◄──┼── Mock Requests │
│                 │  ├──────────────────┤   │                  │
│                 │  │  Spec Manager    │   │                  │
│                 │  ├──────────────────┤   │                  │
│                 │  │  YAML Parser     │   │                  │
│                 │  ├──────────────────┤   │                  │
│                 │  │  Validator       │   │                  │
│                 │  └──────────────────┘   │                  │
│                 └──────────┬──────────────┘                  │
│                            │                                  │
│                 ┌──────────▼──────────┐                      │
│                 │   ourorest.yml      │                      │
│                 │  (OpenAPI 3.1.0)    │                      │
│                 └─────────────────────┘                      │
└──────────────────────────────────────────────────────────────┘
```

### Core Components

#### Backend (Spring Boot Library)
- **`core/global`**: Auto-configuration, response format, exception handling
- **`core/rest/spec`**: API specification CRUD services
- **`core/rest/mock`**: Mock server filter and registry
- **`core/rest/validation`**: OpenAPI validation and enrichment
- **`ui/controller`**: REST API endpoints

#### Frontend (React + TypeScript)
- **`features/spec`**: API specification editor and viewer
- **`features/sidebar`**: Endpoint navigation
- **`services`**: Backend API communication
- **`store`**: Zustand state management

#### Data Storage
- **`ourorest.yml`**: Single OpenAPI file containing all API specifications
- **Location**: `{project}/src/main/resources/ouroboros/rest/ourorest.yml`

---

## 🚀 Quick Start

### Prerequisites
- ☕ Java 17 or higher
- 🍃 Spring Boot 3.x
- 📦 Gradle or Maven

### Installation

#### Gradle
```gradle
dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:0.1.0-SNAPSHOT'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

#### Maven
```xml
<dependency>
    <groupId>io.github.whitesnakegang</groupId>
    <artifactId>ouroboros</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Configuration (Optional)

`application.yml`:
```yaml
ouroboros:
  enabled: true  # default: true
  server:
    url: http://localhost:8080
    description: Local Development Server
```

### Getting Started

1. **Run Spring Boot Application**
   ```bash
   ./gradlew bootRun
   ```

2. **Access Web UI** 🖥️
   
   Open your browser and navigate to:
   ```
   http://localhost:8080/ouroboros/index.html
   ```
   
   The intuitive web interface allows you to:
   - ✅ Create and edit API specifications visually
   - ✅ Manage schemas with drag-and-drop
   - ✅ Preview API documentation in real-time
   - ✅ Import/Export OpenAPI YAML files
   - ✅ Generate code snippets (cURL, JavaScript, Python, etc.)

3. **Create Your First API Specification**
   
   Using the web UI:
   1. Click "New API" button
   2. Fill in the form (path, method, summary, etc.)
   3. Define request/response schemas
   4. Click "Save" - Your mock API is ready!

4. **Test Mock API Immediately**
   
   Your API is now available at the specified path:
   ```bash
   curl http://localhost:8080/api/users
   # Returns mock data automatically!
   ```

> 💡 **Pro Tip**: You can also use the REST API endpoints directly if you prefer programmatic access. See [API Documentation](./backend/docs/endpoints/README.md) for details.

---

## 📚 Usage

### Basic Workflow (Using Web UI)

#### Step 1: Define Reusable Schema
1. Navigate to **"Schemas"** tab in the web UI
2. Click **"New Schema"** button
3. Fill in the schema form:
   - **Name**: `User`
   - **Type**: `object`
   - Add properties:
     - `id` (string) - Mock: `{{random.uuid}}`
     - `name` (string) - Mock: `{{name.fullName}}`
     - `email` (string) - Mock: `{{internet.emailAddress}}`
   - Mark `id` and `name` as required
4. Click **"Save"**

#### Step 2: Create API Specification
1. Navigate to **"APIs"** tab
2. Click **"New API"** button
3. Fill in the API form:
   - **Path**: `/api/users`
   - **Method**: `POST`
   - **Summary**: `Create user`
   - **Request Body**: Reference `User` schema
   - **Response (201)**: Reference `User` schema
   - **Progress**: `mock`
4. Click **"Save"** - Your mock API is now live!

#### Step 3: Test Mock API
Your mock API is immediately available:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'

# Response (auto-generated):
{
  "id": "a3b5c7d9-1234-5678-90ab-cdef12345678",
  "name": "John Doe",
  "email": "john@example.com"
}
```

#### Step 4: Implement & Validate (Backend Developer)
Add `@ApiState` annotation to your controller:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    @ApiState(
        state = ApiState.State.IMPLEMENTING,
        owner = "backend-team",
        description = "User creation API implementation in progress"
    )
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Actual implementation...
        return ResponseEntity.status(201).body(savedUser);
    }
}
```

On application startup, Ouroboros automatically validates your implementation against the spec.

#### Step 5: Update Status
Once implementation is complete, update the status in the web UI:
1. Select your API in the list
2. Change **Progress** from `mock` to `completed`
3. Click **"Save"**

### Import External OpenAPI Files

1. Click **"Import"** button in the web UI
2. Select your OpenAPI YAML file (`.yml` or `.yaml`)
3. Click **"Upload"**

Ouroboros will automatically:
- ✅ Validate OpenAPI 3.1.0 compliance
- ✅ Handle duplicate APIs/schemas (auto-rename with `-import` suffix)
- ✅ Add Ouroboros extension fields
- ✅ Update all `$ref` references

> 📖 **For programmatic access**, see [REST API Documentation](./backend/docs/endpoints/README.md)

---

## 📖 Documentation

### API Documentation
- [Complete API Endpoints](./backend/docs/endpoints/README.md)
- [REST API Specification Management](./backend/docs/endpoints/01-create-rest-api-spec.md)
- [Schema Management](./backend/docs/endpoints/06-create-schema.md)
- [YAML Import](./backend/docs/endpoints/11-import-yaml.md)

### Developer Guide
- [Project Documentation](./backend/PROJECT_DOCUMENTATION.md)
- [GraphQL Design](./backend/docs/graphql/DESIGN.md)
- [Troubleshooting](./backend/docs/troubleshooting/README.md)
- [Try Feature Setup Guide](../../backend/src/main/java/kr/co/ouroboros/core/rest/tryit/OUROBOROS_TRY_SETUP.md)

### OpenAPI Extension Fields

Ouroboros adds the following custom fields to OpenAPI 3.1.0:

**Operation Level:**
- `x-ouroboros-id`: API specification unique identifier (UUID)
- `x-ouroboros-progress`: Development progress status (`mock` | `completed`)
- `x-ouroboros-tag`: Development tag (`none` | `implementing` | `bugfix`)
- `x-ouroboros-isvalid`: Validation status (boolean)

**Schema Level:**
- `x-ouroboros-mock`: DataFaker expression (e.g., `{{name.fullName}}`)
- `x-ouroboros-orders`: Field order array

---

## 🤝 Contributing

Ouroboros is an open-source project and welcomes your contributions!

### How to Contribute

1. **Check Issues**: Find issues to work on in [GitHub Issues](https://github.com/whitesnakegang/ouroboros/issues)
2. **Fork & Clone**: Fork the repository and clone locally
3. **Create Branch**: Create `feature/feature-name` or `fix/bug-name` branch
4. **Develop**: Write code and tests
5. **Commit**: Follow [commit conventions](./CONTRIBUTING.md#commit-message-rules)
6. **Pull Request**: Create PR to `develop` branch

See [Contributing Guide](./CONTRIBUTING.md) for details. [한국어 기여 가이드](./docs/ko/CONTRIBUTING.md)

### Code of Conduct

This project adheres to the [Code of Conduct](./CODE_OF_CONDUCT.md). By participating, you agree to uphold this code. [한국어 행동 강령](./docs/ko/CODE_OF_CONDUCT.md)

---

## 📄 License

This project is licensed under [Apache License 2.0](./LICENSE).

```
Copyright 2025 Whitesnakegang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 👥 Team

### Maintainers
- **Whitesnakegang** - *Project Founder & Maintainer* - [@whitesnakegang](https://github.com/whitesnakegang)

### Contributors
Thanks to all contributors to this project!

[Full Contributors List](https://github.com/whitesnakegang/ouroboros/graphs/contributors)

---

## 🔗 Links

- **GitHub**: https://github.com/whitesnakegang/ouroboros
- **Issues**: https://github.com/whitesnakegang/ouroboros/issues
- **Maven Central**: https://search.maven.org/artifact/io.github.whitesnakegang/ouroboros

---

## 📞 Support

Have questions or issues?

- 📝 [Create an Issue](https://github.com/whitesnakegang/ouroboros/issues/new)
- 💬 [Join Discussion](https://github.com/whitesnakegang/ouroboros/discussions)

---

<div align="center">

**Experience Better API Development with Ouroboros!**

⭐ If this project helped you, please give it a star!

Made with ❤️ by [Whitesnakegang](https://github.com/whitesnakegang)

</div>

