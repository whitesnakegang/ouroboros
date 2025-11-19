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
- [User Interface](#️-user-interface)
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

> 🎬 **New to Ouroboros?** Check out our [User Guide](./docs/USER_GUIDE.md) with animated GIFs showing key workflows!

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
  - ⚠️ **Only methods with `@ApiState` are scanned and validated** - methods without this annotation are excluded from validation
- ✅ **Automatic Enrichment**: Automatically add missing Ouroboros extension fields
- ✅ **Error Reporting**: Detailed validation error messages
- ✅ **Try Feature**: API execution tracking and analysis with **in-memory storage by default** (📖 [Setup Guide](./OUROBOROS_TRY_SETUP.md))
  - **Default**: In-memory trace storage (no setup required)

### 🌐 WebSocket/STOMP API Management
- ✅ **AsyncAPI 3.0.0 Support**: Full support for WebSocket/STOMP API specifications
- ✅ **Channel Management**: Create and manage STOMP destinations (channels)
- ✅ **Operation Management**: Define send/receive operations with reply configurations
- ✅ **Message Components**: Reusable message definitions for WebSocket communications
- ✅ **Schema Management**: Shared schemas for WebSocket message payloads
- ✅ **Code Scanning**: Automatic code scanning via Springwolf (optional)
- ✅ **WebSocket Try**: Performance tracing for WebSocket/STOMP messages

> **⚠️ Important for WebSocket Code Scanning**:
> - **Channel address must include prefix**: When writing WebSocket specifications, the channel address must include the application destination prefix. For example, if your `@MessageMapping` is `/chat/send` and your prefix is `/app`, write the full address as `/app/chat/send` in the specification.
> - **Annotations required**: Only methods annotated with `@MessageMapping` and `@SendTo` are scanned. Methods without these annotations will not be included in code scanning.

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
- **`core/rest/spec`**: REST API specification CRUD services
- **`core/rest/mock`**: Mock server filter and registry
- **`core/rest/validation`**: OpenAPI validation and enrichment
- **`core/rest/tryit`**: Internal method call tracing for try requests
- **`core/websocket/spec`**: WebSocket/STOMP API specification CRUD services
- **`core/websocket/handler`**: WebSocket protocol handlers (with/without Springwolf)
- **`core/websocket/tryit`**: WebSocket message tracing for try requests
- **`ui/rest/controller`**: REST API endpoints
- **`ui/websocket/controller`**: WebSocket API endpoints

#### Frontend (React + TypeScript)
- **`features/spec`**: API specification editor and viewer
- **`features/sidebar`**: Endpoint navigation
- **`services`**: Backend API communication
- **`store`**: Zustand state management

#### Data Storage
- **`ourorest.yml`**: Single OpenAPI file containing all REST API specifications
  - **Location**: `{project}/src/main/resources/ouroboros/rest/ourorest.yml`
- **`ourowebsocket.yml`**: Single AsyncAPI file containing all WebSocket/STOMP API specifications
  - **Location**: `{project}/src/main/resources/ouroboros/websocket/ourowebsocket.yml`

---

## 🚀 Quick Start

### Prerequisites
- ☕ Java 17 or higher
- 🍃 Spring Boot 3.x
- 📦 Gradle or Maven

### Installation

> **⚠️ Version Warning**: Do not use version 1.0.2 as it causes errors. Please use version 1.0.4.

#### Gradle
```gradle
dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:1.0.4'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // Optional: For WebSocket code scanning and spec comparison
    // Only add if you need automatic code scanning for WebSocket APIs
    implementation 'io.github.springwolf:springwolf-stomp:1.17.0'
    runtimeOnly 'io.github.springwolf:springwolf-ui:1.17.0'
}
```

#### Maven
```xml
<dependency>
    <groupId>io.github.whitesnakegang</groupId>
    <artifactId>ouroboros</artifactId>
    <version>1.0.4</version>
</dependency>

<!-- Optional: For WebSocket code scanning and spec comparison -->
<!-- Only add if you need automatic code scanning for WebSocket APIs -->
<dependency>
    <groupId>io.github.springwolf</groupId>
    <artifactId>springwolf-stomp</artifactId>
    <version>1.17.0</version>
</dependency>
<dependency>
    <groupId>io.github.springwolf</groupId>
    <artifactId>springwolf-ui</artifactId>
    <version>1.17.0</version>
    <scope>runtime</scope>
</dependency>
```

> **⚠️ Important**: 
> - If you rely on Lombok annotations, make sure your build includes <code>annotationProcessor 'org.projectlombok:lombok'</code>. Without it, <code>@ApiState</code> metadata is not generated and automatic scanning will be skipped.
> - **`@ApiState` annotation is required for code scanning and validation**: Only controller methods annotated with `@ApiState` are included in code scanning. Methods without this annotation will **not be scanned** and **will not be validated** against the specification. If you want Ouroboros to track and validate your API implementation, you must add `@ApiState` to all controller methods.

### Configuration (Optional)

> **Method Tracing**: Internal method tracing is **disabled by default**. If you need internal method tracing in the Try feature, you must add the `method-tracing` configuration.

`application.yml`:
```yaml
ouroboros:
  enabled: true  # default: true
  server:
    url: http://localhost:8080
    description: Local Development Server
  # Method Tracing configuration (required for internal method tracing in Try feature)
  # Internal method tracing is disabled by default
  method-tracing:
    enabled: true
    allowed-packages: your.package.name  # Specify package paths to trace

# WebSocket Configuration (Optional)
# If you only want to write WebSocket specs without code scanning/comparison:
springwolf:
  enabled: false  # Disable Springwolf (spec writing only)

# If you want WebSocket code scanning, spec comparison, and testing:
springwolf:
  enabled: true
  docket:
    info:
      title: WebSocket API
      version: 1.0.0
      description: WebSocket API Description
    servers:
      websocket:
        host: localhost:8080
        protocol: ws
        description: WebSocket Server
    base-package: com.yourpackage  # Package to scan for @MessageMapping annotations
```

### Getting Started

1. **Run Spring Boot Application**
   ```bash
   ./gradlew bootRun
   ```

2. **Access Web UI** 🖥️
   
   Open your browser and navigate to:
   ```
   http://localhost:8080/ouroboros/
   ```
   
   The intuitive web interface allows you to:
   - ✅ Create and edit API specifications visually
   - ✅ Manage schemas with drag-and-drop
   - ✅ Preview API documentation in real-time
   - ✅ Import/Export OpenAPI YAML files
   - ✅ Generate code snippets (cURL, JavaScript, Python, etc.)

   > 🎬 **Learn the workflows**: See our [User Guide](./docs/USER_GUIDE.md) with step-by-step animated GIFs!

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

## 🖥️ User Interface

Ouroboros provides an intuitive web-based interface for managing API specifications. All operations can be performed through the GUI without writing code.

### Overview

The complete GUI allows you to write and review specifications through a visual interface. The interface is divided into three main areas: the sidebar for navigation, the main content area for viewing and editing specifications, and the action panels for testing and validation.

![Complete GUI](./docs/images/scrennshots/complete-gui.png)

**Key Areas**:
- **Left Sidebar**: Navigate through all API endpoints, schemas, and WebSocket operations
- **Main Content Area**: View and edit API specifications, schemas, and messages
- **Action Panels**: Test APIs, view validation results, and analyze performance

### Sidebar with Status Badges

The sidebar displays all API endpoints with status badges, allowing you to quickly identify development status at a glance. You can filter endpoints by status and easily navigate to any API specification.

![Sidebar with Badges](./docs/images/scrennshots/sidebar-badges.png)

**Status Badges**:
- 🟢 **Completed**: API is fully implemented and tested
- 🟡 **Implementing**: API is currently being developed
- 🔴 **Mock**: API exists only as a specification (not yet implemented)
- 🟠 **Bugfix**: API is under bug fixing

**Features**:
- Click on any endpoint to view its details
- Filter endpoints by status using the status filter buttons
- Group endpoints by tags or paths for better organization
- Quick access to create new APIs, schemas, or operations

### API Detail Page

The detail page provides comprehensive information about each API specification, including request/response schemas, parameters, and metadata. All information is organized in tabs for easy navigation.

![API Detail Page](./docs/images/scrennshots/api-detail-page.png)

**Features**:
- **Overview Tab**: View complete API specification including path, method, summary, description, and tags
- **Request Tab**: Configure request parameters, headers, query parameters, and request body schemas
- **Response Tab**: Define response schemas for different status codes (200, 201, 400, 404, etc.)
- **Test Tab**: Execute API tests and view responses
- **Validation Tab**: Check validation status and discrepancies between spec and implementation
- **Code Snippets**: Generate code examples in various languages (cURL, JavaScript, Python, etc.)
- **Export**: Export API documentation in Markdown or OpenAPI YAML format

**Quick Actions**:
- Edit API details directly in the page
- Reference reusable schemas for request/response bodies
- Set development progress and tags
- View validation status and apply changes

### Specification Editor

Create and edit API specifications through an intuitive form-based editor. The editor provides a step-by-step workflow to define all aspects of your API.

![Specification Editor](./docs/images/scrennshots/spec-editor.png)

**Editor Sections**:
- **Basic Information**: Define path, HTTP method (GET, POST, PUT, DELETE, etc.), summary, and description
- **Request Configuration**: 
  - Add path parameters, query parameters, and headers
  - Define request body schema (reference existing schemas or create inline)
  - Set content types (application/json, application/xml, etc.)
- **Response Configuration**:
  - Add response definitions for each status code
  - Define response headers and body schemas
  - Set response content types
- **Metadata**: Set development progress (mock/completed), tags (none/implementing/bugfix), and validation status

**Capabilities**:
- Reference reusable schemas using `{"ref": "SchemaName"}`
- Auto-complete for schema names and field paths
- Real-time validation of OpenAPI 3.1.0 compliance
- Preview generated OpenAPI specification

### Validation Screen

The validation screen shows discrepancies between your specification and actual implementation, allowing you to review and apply changes. This helps maintain consistency between your API documentation and actual code.

![Validation Screen](./docs/images/scrennshots/validation-screen.png)

**Validation Types**:
- **Request Validation**: Compare request parameters, headers, and body schemas
- **Response Validation**: Compare response status codes, headers, and body schemas
- **Endpoint Validation**: Check if path and method match between spec and implementation
- **Both**: When both request and response differ from the specification

**Features**:
- **Visual Diff Display**: See exactly what differs between spec and implementation
- **One-Click Sync**: Apply changes from code to specification with a single click
- **Validation Status Badge**: Each endpoint shows its validation status (Valid/Invalid/Diff detected)
- **Detailed Reports**: View comprehensive validation reports for all endpoints
- **Filter by Status**: Filter endpoints by validation status for quick review

**Workflow**:
1. View validation results after code scanning
2. Review discrepancies highlighted in the interface
3. Apply changes to synchronize spec with code
4. Track validation status for each endpoint

### Testing Screen

The testing screen allows you to test APIs and view both mock and actual responses, along with method-level performance tracking. Configure authentication, set request parameters, and analyze performance all in one place.

![Testing Setting](./docs/images/scrennshots/testing-setting.png)

**Test Configuration**:
- **Request Setup**: Configure path parameters, query parameters, headers, and request body
- **Authentication**: Set up Bearer tokens, API keys, or custom headers for authenticated requests
- **Environment**: Switch between different environments (development, staging, production)

![Test Response](./docs/images/scrennshots/test-response.png)

**Response Viewing**:
- **Mock Response**: Test against mock data generated from specifications (useful for frontend development)
- **Actual Response**: Test against real backend implementation
- **Side-by-Side Comparison**: Compare mock vs actual responses to verify implementation correctness
- **Response Details**: View status code, headers, and formatted response body (JSON, XML, etc.)

![Method Tracing](./docs/images/scrennshots/method-tracing-simple.png)

**Method Performance Tracking**:
- **Simple View**: See total request execution time and key method timings at a glance
- **Detailed View**: Dive deep into method-level execution times and call hierarchies

![Method Tracing Detail](./docs/images/scrennshots/method-tracing-detail.png)

**Performance Analysis**:
- **Execution Timeline**: Visual timeline showing when each method was called
- **Method Hierarchy**: Tree view showing method call relationships
- **Performance Metrics**: 
  - Total request execution time
  - Individual method execution times
  - Database query durations (if applicable)
  - External API call times (if applicable)
- **Bottleneck Identification**: Automatically highlight slow methods and potential performance issues
- **N+1 Detection**: Identify N+1 query problems in database operations

**Features**:
- **TEST Tab**: Navigate to the TEST tab to view detailed method traces
- **Request History**: View and replay previous test requests
- **Export Results**: Export test results and performance metrics

> 📖 **For step-by-step workflows with animated GIFs, see [User Guide](./docs/USER_GUIDE.md)**

---

## 📚 Usage

### Basic Workflow (Using Web UI)

#### Step 1: Define Reusable Schema
1. Navigate to **"Schemas"** tab in the web UI
2. Fill in the schema form:
   - **Name**: `User`
   - **Type**: `object`
   - Add properties:
     - `id` (string) - Mock: `{{random.uuid}}`
     - `name` (string) - Mock: `{{name.fullName}}`
     - `email` (string) - Mock: `{{internet.emailAddress}}`
   - Mark `id` and `name` as required
3. Click **"Save"**

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
    )
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Actual implementation...
        return ResponseEntity.status(201).body(savedUser);
    }
}
```

> **⚠️ Critical**: **Only methods with `@ApiState` annotation are scanned and validated**. If you don't add `@ApiState` to a controller method:
> - ❌ The method will **not be included** in code scanning
> - ❌ The method will **not be validated** against the specification
> - ❌ Specification-implementation comparison will **not work** for that endpoint
> 
> To enable automatic validation, you **must** add `@ApiState` to all controller methods you want to track.

On application startup, Ouroboros automatically validates your implementation against the spec for all methods annotated with `@ApiState`.

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

### Official Site
- [https://ouroboros.co.kr](https://ouroboros.co.kr) — 최신 가이드와 배포 문서를 확인할 수 있습니다.

### API Documentation
- [Complete API Endpoints](./backend/docs/endpoints/README.md)
- [REST API Specification Management](./backend/docs/endpoints/01-create-rest-api-spec.md)
- [Schema Management](./backend/docs/endpoints/06-create-schema.md)
- [WebSocket Operation Management](./backend/docs/endpoints/12-create-websocket-operation.md)
- [WebSocket Schema & Message Management](./backend/docs/endpoints/README.md#websocket-스키마-관리)
- [YAML Import](./backend/docs/endpoints/11-import-yaml.md)

### User Guides
- 🎬 **[User Guide with Animated Workflows](./docs/USER_GUIDE.md)** - Step-by-step workflows with GIFs showing how to:
  - Create and use schemas in REST APIs
  - Set up authentication for testing
  - View method-level performance results
  - Create WebSocket/STOMP specifications
- [한국어 사용자 가이드](./docs/ko/USER_GUIDE.md) - GIF가 포함된 단계별 워크플로우

### Developer Guide
- [Project Documentation](./backend/PROJECT_DOCUMENTATION.md)
- [GraphQL Design](./backend/docs/graphql/DESIGN.md)
- [Troubleshooting](./backend/docs/troubleshooting/README.md)
- [Try Feature Setup Guide](./OUROBOROS_TRY_SETUP.md)

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

