# Ouroboros SDK Try Feature Setup Guide

This guide explains how to set up the Try feature of the Ouroboros SDK.

## Table of Contents

1. [Dependency Configuration](#1-dependency-configuration)
2. [Application Configuration](#2-application-configuration)
3. [Tempo Integration Configuration](#3-tempo-integration-configuration)
4. [Using the Try Feature](#4-using-the-try-feature)
5. [Gitignore Configuration](#5-gitignore-configuration)
6. [Frequently Asked Questions (QnA)](#6-frequently-asked-questions-qna)

---

## 1. Dependency Configuration

### build.gradle

Add the Ouroboros SDK dependency to your project's `build.gradle` file.

```gradle
dependencies {
    implementation 'io.github.whitesnakegang:ouroboros:1.0.0'
}
```

> **Note**: Please verify and update the Ouroboros SDK version to the latest version.

---

## 2. Application Configuration

### application.properties

Add the following configuration to the `src/main/resources/application.properties` file.

#### Basic Ouroboros Configuration

```properties
# Enable Ouroboros SDK
ouroboros.enabled=true
ouroboros.server.url=https://your-api-server.com
ouroboros.server.description=Your API Server Description
```

> **Note**: Change `ouroboros.server.url` and `ouroboros.server.description` to your actual project's API server information.

#### Tempo Integration Configuration

```properties
# Enable Tempo integration (Ouroboros SDK uses QueryQL)
ouroboros.tempo.enabled=true
ouroboros.tempo.base-url=http://${TEMPO_HOST:localhost}:${TEMPO_UI_PORT:3200}
```

#### Method Tracing Configuration

```properties
# Enable Method Tracing
ouroboros.method-tracing.enabled=true
ouroboros.method-tracing.allowed-packages=your.package.name
```

> **Note**: Specify the package paths to apply tracing. Examples: `com.example.yourproject`, `your.package.name`, etc.

#### OpenTelemetry Exporter Configuration

```properties
# OpenTelemetry Exporter (App -> Tempo)
# Using HTTP protocol (port 4318)
management.tracing.enabled=true
management.otlp.tracing.endpoint=http://${TEMPO_HOST:localhost}:${TEMPO_HTTP_PORT:4318}/v1/traces

# Micrometer Tracing
management.tracing.sampling.probability=1.0
```

> **Note**: `${TEMPO_HOST:localhost}` and `${TEMPO_HTTP_PORT:4318}` reference environment variables set in the `.env` file.  
> The Try feature sends trace data to Tempo using the HTTP protocol.

#### Logging Configuration

```properties
# Output Ouroboros SDK debug logs to console
logging.level.kr.co.ouroboros=DEBUG
```

---

## 3. Tempo Integration Configuration

The Try feature works in conjunction with **Grafana Tempo**. Tempo is a backend server that collects and stores trace data.

### File Structure

```
.
├─ docker-compose.yml      # Tempo container configuration
├─ tempo.yaml              # Tempo configuration file
├─ .env                    # Docker ↔ App synchronization variables (required)
├─ tempo-data/             # Local trace data storage folder (auto-generated)
└─ src/main/resources/application.properties  # Spring Boot app configuration
```

### Create .env File

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

### Docker Compose Configuration

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

### Tempo Configuration File

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
> This file can be automatically managed by the Ouroboros SDK. You can create or modify the `tempo.yaml` file in the project root as needed.

### Start Tempo

```bash
# Start Tempo container
docker compose up -d

# Verify execution
docker ps
docker logs tempo -f
```

---

## 4. Using the Try Feature

### Making Try Requests

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

### Querying Try Results

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

### Regular Requests (Without Try)

Regular requests without the Try header will work normally, and no Try-related information will be included.

```bash
curl -X GET "http://localhost:8080/api/your-endpoint"
```

### How the Try Feature Works

1. **Request Detection**: Detects requests containing the `X-Ouroboros-Try: on` header
2. **Try ID Generation**: Generates a unique Try ID (UUID)
3. **Trace Collection**: Collects all traces during request execution through OpenTelemetry
4. **Tempo Storage**: Sends and stores collected trace data to Tempo
5. **Result Query**: Queries analysis results through the `/ouro/tries/{tryId}` endpoint

---

## 5. Gitignore Configuration

### .gitignore File Configuration

Add the following items to your project's `.gitignore` file to prevent unnecessary files from being committed to Git.

#### Tempo-related Files and Directories

```gitignore
# Tempo data directory
tempo-data/
```

> **Note**: The `tempo-data/` directory stores trace data generated by Tempo. It is large and varies by local environment, so it should not be committed to Git.

#### Environment Variable Files

```gitignore
# Environment variable files
.env
.env.local
.env.*.local
```

> **Note**: The `.env` file may contain sensitive information (API keys, passwords, etc.), so it should not be committed to Git.  
> Instead, create a `.env.example` file to share only the list of required environment variables.

#### Complete Example

Add the following to your project root's `.gitignore` file:

```gitignore
# Existing Gitignore configuration...

# ===== Ouroboros SDK Try Feature Related =====

# Tempo data directory
tempo-data/

# Environment variable files
.env
.env.local
.env.*.local

# Docker Compose override
docker-compose.override.yml

# Log files
*.log
logs/
```

### Important Notes

- `tempo.yaml`: This file is a project configuration file and **should be included** in Git. (Settings that need to be shared with team members)
- `docker-compose.yml`: This is a Docker Compose configuration file and **should be included** in Git.
- `tempo-data/`: The Tempo data directory **should not be included** in Git. (Local data)
- `.env`: Environment variable files **should not be included** in Git. (Sensitive information)

> 💡 **If already committed**:
> ```bash
> git rm -r --cached tempo-data
> echo "tempo-data/" >> .gitignore
> git add .gitignore
> git commit -m "ignore tempo trace data directory"
> ```

---

## Checklist

A checklist to verify that configuration is complete:

- [ ] Add Ouroboros SDK dependency to `build.gradle`
- [ ] Create `.env` file and configure Tempo-related environment variables
- [ ] Create `docker-compose.yml` file and configure Tempo service
- [ ] Create `tempo.yaml` file and configure Tempo
- [ ] Add basic Ouroboros configuration to `application.properties`
- [ ] Add Tempo integration configuration to `application.properties`
- [ ] Add Method Tracing configuration to `application.properties`
- [ ] Add OpenTelemetry Exporter configuration to `application.properties`
- [ ] Add `tempo-data/`, `.env` to `.gitignore`
- [ ] Start Tempo server (`docker compose up -d`)
- [ ] Run application and test Try requests

---

## Troubleshooting

### Try Feature Not Working

1. **Check Dependencies**: Verify that Ouroboros SDK is properly added to `build.gradle`
2. **Check Configuration**: Verify `ouroboros.enabled=true` setting in `application.properties`
3. **Check Header**: Verify that the request includes the `X-Ouroboros-Try: on` header exactly
4. **Check Logging**: Check detailed logs with `logging.level.kr.co.ouroboros=DEBUG` setting

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

### Changing OTLP Port

**Q. Can I change the OTLP port?**  
A. It's possible but not recommended. 4317 (gRPC) and 4318 (HTTP) are OpenTelemetry standard ports.

### Changing Tempo UI Port

**Q. I want to change the Tempo UI port.**  
A. Change `TEMPO_UI_PORT=3300` in the `.env` file, then restart with `docker compose down && docker compose up -d`.

### Trace Data Storage Location

**Q. Where is trace data stored?**  
A. It is stored locally in the `./tempo-data/` folder.  
   For production environments, modify `tempo.yaml` to switch to object storage like S3 or MinIO.

---

## References

- [Ouroboros SDK Official Documentation](https://github.com/whitesnakegang/ouroboros)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Grafana Tempo Documentation](https://grafana.com/docs/tempo/latest/)

---

## 🚀 Quick Start Summary

1. **Add Dependency**: Add Ouroboros SDK to `build.gradle`
2. **Set Environment Variables**: Create `.env` file
3. **Docker Configuration**: Create `docker-compose.yml`, `tempo.yaml`
4. **Application Configuration**: Add Tempo integration settings to `application.properties`
5. **Gitignore Configuration**: Add `tempo-data/`, `.env` to `.gitignore`
6. **Run**: Start Tempo with `docker compose up -d`, then run the application
7. **Try Request**: Make API requests with `X-Ouroboros-Try: on` header
8. **Query Results**: Check analysis results via `/ouro/tries/{tryId}`

