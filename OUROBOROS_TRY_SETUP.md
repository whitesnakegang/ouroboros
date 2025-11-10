# Ouroboros SDK Try Feature Setup Guide

This guide explains how to use the Try feature of the Ouroboros SDK. The Try feature tracks and analyzes API execution traces.

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [Basic Configuration](#2-basic-configuration)
3. [Using the Try Feature](#3-using-the-try-feature)
4. [Advanced Configuration](#4-advanced-configuration)
5. [Troubleshooting](#5-troubleshooting)
6. [Frequently Asked Questions (QnA)](#6-frequently-asked-questions-qna)

---

## 1. Quick Start

### Try Feature Overview

The Try feature allows you to track and analyze API execution traces by simply adding a header to your requests. **By default, traces are stored in memory**, so you can start using it immediately without any additional setup.

### Minimum Setup (In-Memory Storage)

1. **Add Dependency**: Add Ouroboros SDK to `build.gradle`
2. **Basic Configuration**: Add basic Ouroboros settings to `application.properties`
3. **Run**: Start your application
4. **Use**: Add `X-Ouroboros-Try: on` header to your API requests

That's it! The Try feature works immediately with in-memory storage.

> **Note**: Traces stored in memory are lost when the application restarts. For persistent storage, see [Tempo Integration](#tempo-integration-optional) in the Advanced Configuration section.

---

## 2. Basic Configuration

### 2.1. Dependency Configuration

#### build.gradle

Add the Ouroboros SDK dependency to your project's `build.gradle` file.

```gradle
dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:1.0.1'
}
```

> **Note**: Please verify and update the Ouroboros SDK version to the latest version.

### 2.2. Application Configuration

#### application.properties

Add the following configuration to the `src/main/resources/application.properties` file.

```properties
# Enable Ouroboros SDK
ouroboros.enabled=true
ouroboros.server.url=https://your-api-server.com
ouroboros.server.description=Your API Server Description
```

> **Note**: Change `ouroboros.server.url` and `ouroboros.server.description` to your actual project's API server information.

#### Trace Storage (Default: In-Memory)

> **💡 Default Behavior**: By default, Ouroboros uses **in-memory trace storage**. You don't need any additional configuration. Traces are stored in memory and are immediately available, but will be lost when the application restarts.

No configuration needed! The Try feature works immediately with in-memory storage.

#### Logging Configuration (Optional)

```properties
# Output Ouroboros SDK debug logs to console
logging.level.kr.co.ouroboros=DEBUG
```

---

## 3. Using the Try Feature

### 3.1. Making Try Requests

To use the Try feature, add the `X-Ouroboros-Try: on` header to your API requests.

#### Example: cURL

```bash
curl -X GET "http://localhost:8080/api/your-endpoint" \
  -H "X-Ouroboros-Try: on"
```

> **Note**: Change `localhost:8080` to your actual application server address and port.

#### Example: JavaScript (Fetch API)

```javascript
fetch('http://localhost:8080/api/your-endpoint', {
  headers: {
    'X-Ouroboros-Try': 'on'
  }
})
.then(response => response.json())
.then(data => {
  console.log('Try ID:', data._ouroborosTryId);
});
```

> **Note**: Change to your actual API endpoint URL.

### 3.2. Querying Try Results

After making a Try request, you can query the results using the generated `tryId`.

```bash
GET /ouro/tries/{tryId}
```

#### Response Example

```json
{
  "tryId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed",
  "createdAt": "2024-01-01T00:00:00Z",
  "analyzedAt": "2024-01-01T00:00:01Z",
  "spans": [...],
  "issues": [...],
  "spanCount": 10
}
```

### 3.3. Regular Requests (Without Try)

Regular requests without the Try header will work normally, and no Try-related information will be included.

```bash
curl -X GET "http://localhost:8080/api/your-endpoint"
```

### 3.4. How the Try Feature Works

1. **Request Detection**: Detects requests containing the `X-Ouroboros-Try: on` header
2. **Try ID Generation**: Generates a unique Try ID (UUID)
3. **Trace Collection**: Collects all traces during request execution through OpenTelemetry
4. **Trace Storage**: 
   - **Default (In-memory)**: Stores traces in memory (immediately available, lost on restart)
   - **Tempo (Optional)**: Sends and stores collected trace data to Tempo (persistent storage)
5. **Result Query**: Queries analysis results through the `/ouro/tries/{tryId}` endpoint

---

## 4. Advanced Configuration

### 4.1. Method Tracing (Optional)

> **Note**: Internal method tracing is **disabled by default**. If you need to trace internal method calls in the Try feature, you must enable this configuration.

> **⚠️ Required Settings**: To use method tracing, you must configure both Ouroboros method tracing and Micrometer sampling settings.

```properties
# Enable Method Tracing
ouroboros.method-tracing.enabled=true
ouroboros.method-tracing.allowed-packages=your.package.name

# Micrometer Tracing (Required for Method Tracing)
# Set sampling probability to 1.0 to capture all traces
management.tracing.sampling.probability=1.0
```

> **Note**: 
> - Specify the package paths to apply tracing. Examples: `com.example.yourproject`, `your.package.name`, etc.
> - `management.tracing.sampling.probability=1.0` is **required** to capture all method traces. Without this setting, method traces may not be captured.

### 4.2. Tempo Integration (Optional)

> **💡 When to Use Tempo**: Use Tempo if you need:
> - Persistent trace storage (traces survive application restarts)
> - Advanced trace analysis capabilities
> - Sharing traces across multiple application instances
> 
> For most use cases, **in-memory storage is sufficient** and requires no setup.

If you need persistent trace storage or want to use **Grafana Tempo** for advanced trace analysis, you can optionally configure Tempo integration.

#### Enable Tempo in Application Configuration

```properties
# Enable Tempo integration (Ouroboros SDK uses TraceQL)
# When not set or set to false, in-memory storage is used by default
ouroboros.tempo.enabled=true
ouroboros.tempo.base-url=http://${TEMPO_HOST:localhost}:${TEMPO_UI_PORT:3200}
```

#### OpenTelemetry Exporter Configuration

> **Note**: This configuration is **only required** when using Tempo integration (`ouroboros.tempo.enabled=true`).

```properties
# OpenTelemetry Exporter (App -> Tempo)
# Using HTTP protocol (port 4318)
management.tracing.enabled=true
management.otlp.tracing.endpoint=http://${TEMPO_HOST:localhost}:${TEMPO_HTTP_PORT:4318}/v1/traces

# Micrometer Tracing
management.tracing.sampling.probability=1.0
```

> **Note**: `${TEMPO_HOST:localhost}` and `${TEMPO_HTTP_PORT:4318}` reference environment variables set in the `.env` file.

#### Tempo Server Setup

##### File Structure

```
.
├─ docker-compose.yml      # Tempo container configuration
├─ tempo.yaml              # Tempo configuration file
├─ .env                    # Docker ↔ App synchronization variables (required)
├─ tempo-data/             # Local trace data storage folder (auto-generated)
└─ src/main/resources/application.properties  # Spring Boot app configuration
```

##### Create .env File

Create a `.env` file in the project root (where `docker-compose.yml` is located).

```bash
# ====== Docker ↔ App Synchronization Variables ======
# Tempo server address (Docker container name or localhost)
TEMPO_HOST=localhost

# Tempo ports (Docker Compose ↔ Spring Boot synchronization)
TEMPO_HTTP_PORT=4318  # OTLP HTTP (trace data transmission)
TEMPO_UI_PORT=3200    # Tempo UI / Query API
```

> **Note**: These variables are required for port/host synchronization between Docker Compose and Spring Boot.  
> For local execution → `TEMPO_HOST=localhost`  
> For execution within Docker network → `TEMPO_HOST=tempo`

##### Docker Compose Configuration

Create a `docker-compose.yml` file in the project root.

```yaml
services:
  tempo:
    image: grafana/tempo:2.6.1
    container_name: tempo
    ports:
      - "${TEMPO_HTTP_PORT:-4318}:4318"  # OTLP HTTP (trace data reception)
      - "${TEMPO_UI_PORT:-3200}:3200"    # Tempo UI / Query API
    command: ["-config.file=/etc/tempo/tempo.yaml"]
    volumes:
      - ./tempo.yaml:/etc/tempo/tempo.yaml:ro
      - ./tempo-data:/var/tempo
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:3200/ready"]
      interval: 10s
      timeout: 3s
      retries: 10
    networks:
      - tempo-net

networks:
  tempo-net:
    driver: bridge
```

> **Note**: Only HTTP protocol is used, so gRPC port (4317) is not required.

##### Tempo Configuration File

Create a `tempo.yaml` file in the project root:

```yaml
server:
  http_listen_port: 3200

storage:
  trace:
    backend: local
    local:
      path: /var/tempo/traces

distributor:
  receivers:
    otlp:
      protocols:
        http:
          endpoint: 0.0.0.0:4318
```

> **Note**: Only HTTP protocol is used, so gRPC protocol configuration has been removed.

##### Start Tempo

```bash
# Start Tempo container
docker compose up -d

# Verify execution
docker ps
docker logs tempo -f
```

#### Gitignore Configuration (For Tempo)

Add the following items to your project's `.gitignore` file:

```gitignore
# ===== Ouroboros SDK Try Feature Related =====

# Tempo data directory
tempo-data/

# Environment variable files
.env
.env.local
.env.*.local

# Docker Compose override
docker-compose.override.yml
```

> **Important Notes**:
> - `tempo.yaml`: This file is a project configuration file and **should be included** in Git.
> - `docker-compose.yml`: This is a Docker Compose configuration file and **should be included** in Git.
> - `tempo-data/`: The Tempo data directory **should not be included** in Git.
> - `.env`: Environment variable files **should not be included** in Git.

---

## 5. Troubleshooting

### Try Feature Not Working

1. **Check Dependencies**: Verify that Ouroboros SDK is properly added to `build.gradle`
2. **Check Configuration**: Verify `ouroboros.enabled=true` setting in `application.properties`
3. **Check Header**: Verify that the request includes the `X-Ouroboros-Try: on` header exactly
4. **Check Logging**: Check detailed logs with `logging.level.kr.co.ouroboros=DEBUG` setting

### Method Tracing Not Working

1. **Check Method Tracing Configuration**: Verify `ouroboros.method-tracing.enabled=true` is set
2. **Check Allowed Packages**: Verify `ouroboros.method-tracing.allowed-packages` is correctly configured with your package paths
3. **Check Micrometer Sampling**: Verify `management.tracing.sampling.probability=1.0` is set (required for method tracing)
4. **Check Package Names**: Ensure the classes you want to trace are in the allowed packages
5. **Check Logging**: Enable debug logging to see if method traces are being created

### Tempo Integration Not Working

1. **Check Tempo Execution**: Verify that Tempo is running normally with Docker Compose
   ```bash
   docker ps
   docker logs tempo -f
   ```
2. **Check Ports**: Verify that `TEMPO_HTTP_PORT` in `.env` file matches `management.otlp.tracing.endpoint` in `application.properties`
   - HTTP port should be 4318
   - Verify that `application.properties` uses `http://` protocol
3. **Check Environment Variables**: Verify that the `.env` file exists in the project root and is in the correct format
   - Verify that `TEMPO_HTTP_PORT=4318` is set
   - Verify that `TEMPO_UI_PORT=3200` is set
4. **Check Network**: Verify network connection from application to Tempo server
   - For local execution: `TEMPO_HOST=localhost`
   - For Docker network execution: `TEMPO_HOST=tempo`
5. **Access Tempo UI**: Verify that Tempo UI is displayed correctly at [http://localhost:3200](http://localhost:3200)
6. **Check Protocol**: Only HTTP protocol is used, so verify that there are no gRPC-related settings

---

## 6. Frequently Asked Questions (QnA)

### In-Memory vs Tempo Storage

**Q. What's the difference between in-memory and Tempo storage?**  
A. 
- **In-memory (default)**: No setup required, traces are immediately available, but lost on application restart
- **Tempo (optional)**: Requires Docker setup, provides persistent storage, traces survive application restarts

### When to Use Tempo

**Q. When should I use Tempo?**  
A. Use Tempo if you need:
- Persistent trace storage (traces survive application restarts)
- Advanced trace analysis across multiple requests
- Sharing traces across multiple application instances
- Long-term trace retention

For most development and testing scenarios, **in-memory storage is sufficient**.

### Trace Data Storage Location

**Q. Where is trace data stored?**  
A. By default, traces are stored in memory. When Tempo is enabled, traces are stored in the `./tempo-data/` folder.  
   For production environments with Tempo, modify `tempo.yaml` to switch to object storage like S3 or MinIO.

### Changing OTLP Port

**Q. Can I change the OTLP port?**  
A. It's possible but not recommended. 4317 (gRPC) and 4318 (HTTP) are OpenTelemetry standard ports.

### Changing Tempo UI Port

**Q. I want to change the Tempo UI port.**  
A. Change `TEMPO_UI_PORT=3300` in the `.env` file, then restart with `docker compose down && docker compose up -d`.

### Method Tracing Configuration

**Q. Method traces are not being captured. What should I check?**  
A. Ensure the following settings are configured:
1. `ouroboros.method-tracing.enabled=true`
2. `ouroboros.method-tracing.allowed-packages=your.package.name` (with your actual package)
3. `management.tracing.sampling.probability=1.0` (required - without this, method traces won't be captured)

**Q. Do I need `management.tracing.sampling.probability=1.0` for basic Try feature?**  
A. No. This setting is only required when using Method Tracing. The basic Try feature works without it.

---

## References

- [Ouroboros SDK Official Documentation](https://github.com/whitesnakegang/ouroboros)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Grafana Tempo Documentation](https://grafana.com/docs/tempo/latest/)

---

## 🚀 Quick Start Summary

### Option 1: Quick Start with In-Memory Storage (Default)

1. **Add Dependency**: Add Ouroboros SDK to `build.gradle`
2. **Application Configuration**: Add basic Ouroboros settings to `application.properties`
3. **Run**: Start your application
4. **Try Request**: Make API requests with `X-Ouroboros-Try: on` header
5. **Query Results**: Check analysis results via `/ouro/tries/{tryId}`

> **Note**: Traces are stored in memory and will be lost when the application restarts.

### Option 2: With Tempo Integration (For Persistent Storage)

1. **Add Dependency**: Add Ouroboros SDK to `build.gradle`
2. **Set Environment Variables**: Create `.env` file
3. **Docker Configuration**: Create `docker-compose.yml`, `tempo.yaml`
4. **Application Configuration**: Add Tempo integration settings to `application.properties` (set `ouroboros.tempo.enabled=true`)
5. **Gitignore Configuration**: Add `tempo-data/`, `.env` to `.gitignore`
6. **Run**: Start Tempo with `docker compose up -d`, then run the application
7. **Try Request**: Make API requests with `X-Ouroboros-Try: on` header
8. **Query Results**: Check analysis results via `/ouro/tries/{tryId}`
