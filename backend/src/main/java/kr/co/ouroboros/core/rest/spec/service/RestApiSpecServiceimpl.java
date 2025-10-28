package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.global.properties.OuroborosProperties;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.dto.GetRestApiSpecsResponse;
import kr.co.ouroboros.core.rest.spec.model.RestApiSpec;
import kr.co.ouroboros.core.rest.spec.writer.OpenApiYamlWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link RestApiSpecService}.
 * <p>
 * Handles the creation of REST API specifications by converting DTOs to domain models
 * and delegating YAML file generation to {@link OpenApiYamlWriter}.
 *
 * @since 0.0.1
 */
@Service
public class RestApiSpecServiceimpl implements RestApiSpecService {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "delete", "patch", "options", "head", "trace"
    );

    @Value("${server.port:8080}")
    private String serverPort;

    private final OuroborosProperties properties;

    @Autowired
    public RestApiSpecServiceimpl(OuroborosProperties properties) {
        this.properties = properties;
    }

    @Override
    public CreateRestApiResponse createRestApiSpec(CreateRestApiRequest request) throws Exception {
        // Generate UUID if not provided
        String id = request.getId();
        if (id == null || id.trim().isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }

        // Convert DTO to domain model
        RestApiSpec spec = convertToSpec(request, id);

        // Get base resource path from classpath
        String resourcePath = System.getProperty("user.dir") + "/src/main/resources";

        // Determine server URL
        String serverUrl = properties.getServer().getUrl();
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            serverUrl = "http://127.0.0.1:" + serverPort;
        }

        // Determine server description
        String serverDescription = properties.getServer().getDescription();

        // Write YAML file
        OpenApiYamlWriter writer = new OpenApiYamlWriter(serverUrl, serverDescription);
        writer.writeToFile(spec, resourcePath);

        return CreateRestApiResponse.builder()
                .id(id)
                .filePath(resourcePath + "/ouroboros/rest/ourorest.yml")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public GetRestApiSpecsResponse getAllRestApiSpecs() throws Exception {
        // Set resource path
        String resourcePath = System.getProperty("user.dir") + "/src/main/resources";
        Path filePath = Paths.get(resourcePath, "ouroboros", "rest", "ourorest.yml");

        // Return empty response if file does not exist
        if (!Files.exists(filePath)) {
            return GetRestApiSpecsResponse.builder()
                    .baseUrl("")
                    .version("")
                    .specs(new ArrayList<>())
                    .build();
        }

        // Read YAML file
        Yaml yaml = new Yaml();
        Map<String, Object> openApiDoc;
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Object loaded = yaml.load(fis);
            if (!(loaded instanceof Map)) {
                return GetRestApiSpecsResponse.builder()
                        .baseUrl("")
                        .version("")
                        .specs(new ArrayList<>())
                        .build();
            }
            openApiDoc = (Map<String, Object>) loaded;
        }

        // Extract baseUrl from servers section
        String baseUrl = "";
        List<Map<String, String>> servers = (List<Map<String, String>>) openApiDoc.get("servers");
        if (servers != null && !servers.isEmpty()) {
            baseUrl = servers.get(0).getOrDefault("url", "");
        }

        // Extract version from info section
        String version = "";
        Map<String, Object> info = (Map<String, Object>) openApiDoc.get("info");
        if (info != null) {
            version = (String) info.getOrDefault("version", "");
        }

        // Iterate through paths section to generate API specification summary list
        List<GetRestApiSpecsResponse.RestApiSpecSummary> specs = new ArrayList<>();
        Map<String, Object> paths = (Map<String, Object>) openApiDoc.get("paths");
        if (paths != null) {
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();

                // Process each HTTP method
                for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                    String key = methodEntry.getKey().toLowerCase();
                    Object value = methodEntry.getValue();

                    // Filter: only process valid HTTP methods
                    if (!HTTP_METHODS.contains(key)) {
                        continue; // Skip non-HTTP keys (summary, description, servers, parameters)
                    }

                    // Type check: ensure value is a Map (operation object)
                    if (!(value instanceof Map)) {
                        continue; // Skip unexpected types
                    }

                    String method = key.toUpperCase();
                    Map<String, Object> operation = (Map<String, Object>) value;

                    // Extract domain from tags field
                    List<String> domain = (List<String>) operation.get("tags");
                    if (domain == null) {
                        domain = new ArrayList<>();
                    }

                    // Extract Ouroboros custom fields
                    String id = (String) operation.get("x-ouroboros-id");
                    String progress = (String) operation.getOrDefault("x-ouroboros-progress", "mock");
                    String tag = (String) operation.getOrDefault("x-ouroboros-tag", "none");
                    Boolean isValid = (Boolean) operation.getOrDefault("x-ouroboros-isvalid", true);

                    // Build RestApiSpecSummary
                    GetRestApiSpecsResponse.RestApiSpecSummary summary = GetRestApiSpecsResponse.RestApiSpecSummary.builder()
                            .domain(domain)
                            .method(method)
                            .path(path)
                            .protocol("rest")
                            .id(id)
                            .progress(progress)
                            .tag(tag)
                            .isValid(isValid)
                            .build();

                    specs.add(summary);
                }
            }
        }

        return GetRestApiSpecsResponse.builder()
                .baseUrl(baseUrl)
                .version(version)
                .specs(specs)
                .build();
    }

    private RestApiSpec convertToSpec(CreateRestApiRequest request, String id) {
        return RestApiSpec.builder()
                .id(id)
                .path(request.getPath())
                .method(request.getMethod())
                .summary(request.getSummary())
                .description(request.getDescription())
                .deprecated(request.isDeprecated())
                .tags(request.getTags())
                .parameters(request.getParameters())
                .requestBody(request.getRequestBody())
                .responses(request.getResponses())
                .security(request.getSecurity())
                // Ouroboros custom fields with defaults
                .progress(request.getProgress() != null ? request.getProgress() : "mock")
                .tag(request.getTag() != null ? request.getTag() : "none")
                .isValid(request.getIsValid() != null ? request.getIsValid() : true)
                .build();
    }
}
