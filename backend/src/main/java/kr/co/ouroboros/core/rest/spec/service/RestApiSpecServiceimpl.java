package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;
import kr.co.ouroboros.core.rest.spec.model.RestApiSpec;
import kr.co.ouroboros.core.rest.spec.writer.OpenApiYamlWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RestApiSpecServiceimpl implements RestApiSpecService {

    @Value("${server.port:8080}")
    private String serverPort;

    @Override
    public CreateRestApiResponse createRestApiSpec(CreateRestApiRequest request) throws Exception {
        // Convert DTO to domain model
        RestApiSpec spec = convertToSpec(request);

        // Get base resource path from classpath
        String resourcePath = System.getProperty("user.dir") + "/src/main/resources";

        // Generate server URL
        String serverUrl = "http://127.0.0.1:" + serverPort;

        // Write YAML file
        OpenApiYamlWriter writer = new OpenApiYamlWriter(serverUrl);
        writer.writeToFile(spec, resourcePath);

        return CreateRestApiResponse.builder()
                .success(true)
                .message("REST API specification created successfully")
                .filePath(resourcePath + "/ouroboros/rest/rest.yml")
                .build();
    }

    private RestApiSpec convertToSpec(CreateRestApiRequest request) {
        return RestApiSpec.builder()
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
