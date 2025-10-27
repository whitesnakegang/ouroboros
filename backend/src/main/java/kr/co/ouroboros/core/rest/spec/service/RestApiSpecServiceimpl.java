package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.global.properties.OuroborosProperties;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.model.RestApiSpec;
import kr.co.ouroboros.core.rest.spec.writer.OpenApiYamlWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
                .success(true)
                .message("REST API specification created successfully")
                .id(id)
                .filePath(resourcePath + "/ouroboros/rest/ourorest.yml")
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
                .build();
    }
}
