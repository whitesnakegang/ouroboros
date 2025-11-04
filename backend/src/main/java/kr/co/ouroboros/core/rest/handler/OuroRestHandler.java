package kr.co.ouroboros.core.rest.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import kr.co.ouroboros.core.rest.common.dto.PathItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@RequiredArgsConstructor
@Component
@Slf4j
public class OuroRestHandler implements OuroProtocolHandler {

    private final OpenAPIService openAPIService;
    private final RestSpecSyncPipeline pipeline;

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true); // DTO에 @JsonIgnoreProperties로 안전

    private static final String RESOURCE_PATH = System.getProperty("user.dir") + "/src/main/resources";


    /**
     * Identifies this handler's protocol as REST.
     *
     * @return the {@link Protocol#REST} enum value
     */
    @Override
    public Protocol getProtocol() {
        return Protocol.REST;
    }

    /**
     * Provide the path to the REST OpenAPI specification file used by this handler.
     *
     * @return the path to the REST OpenAPI spec file, "/ouroboros/rest/ourorest.yml"
     */
    @Override
    public String getSpecFilePath() {
        return "/ouroboros/rest/ourorest.yml";
    }

    /**
     * Produce an OuroRestApiSpec from the running application's cached OpenAPI model.
     *
     * <p>Retrieves the cached OpenAPI model for Locale.KOREA and converts it to an OuroRestApiSpec. If the spec's Info exists but its version is null, sets the version to "v1".</p>
     *
     * @return the scanned REST API specification
     * @throws IllegalStateException if the OpenAPI model cannot be retrieved or converted
     */
    @Override
    public OuroApiSpec scanCurrentState() {
        try {
            OpenAPI model = openAPIService.getCachedOpenAPI(Locale.KOREA);

            String json = Json31.mapper().writeValueAsString(model);
            OuroRestApiSpec spec = mapper.readValue(json, OuroRestApiSpec.class);

            if (spec.getInfo() != null && spec.getInfo().getVersion() == null) {
                spec.getInfo().setVersion("v1");
            }

            return spec;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan current OpenAPI state", e);
        }
    }

    /**
     * Parses YAML content and constructs an OuroRestApiSpec representing the API specification.
     *
     * @param yamlContent the YAML document text containing the API specification
     * @return an {@link OuroRestApiSpec} instance populated from the YAML content as an {@link OuroApiSpec}
     */
    @Override
    public OuroApiSpec loadFromFile(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlContent);

        return mapper.convertValue(map, OuroRestApiSpec.class);
    }

    /**
     * Reconciles the API specification loaded from file with the specification scanned from the running application into a single synchronized specification.
     *
     * @param fileSpec    the API specification parsed from the repository or file
     * @param scannedSpec the API specification obtained from the application's OpenAPI model
     * @return            the reconciled OuroApiSpec representing the synchronized specification
     */
    @Override
    public OuroApiSpec synchronize(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {
        return pipeline.validate(fileSpec, scannedSpec);
    }
    
    /**
     * Serialize an OuroRestApiSpec to YAML and persist it to the file path calculated from getSpecFilePath().
     *
     * @param specToSave the spec to serialize; must be an instance of {@code OuroRestApiSpec}
     * @throws IllegalArgumentException if {@code specToSave} is not an {@code OuroRestApiSpec}
     * @throws IllegalStateException if serialization or file I/O fails while creating directories or writing the YAML file
     */
    @Override
    public void saveYaml(OuroApiSpec specToSave) {
        try {
            if (!(specToSave instanceof OuroRestApiSpec)) {
                throw new IllegalArgumentException("Spec must be OuroRestApiSpec");
            }

            OuroRestApiSpec restSpec = (OuroRestApiSpec) specToSave;
            
            // Debug: Check security before serialization
            if (restSpec.getComponents() != null && restSpec.getComponents().getSecuritySchemes() != null) {
                log.info("🔐 [BEFORE SAVE] SecuritySchemes exists: {}", restSpec.getComponents().getSecuritySchemes().keySet());
            } else {
                log.warn("⚠️ [BEFORE SAVE] No securitySchemes in spec!");
            }
            
            // Debug: Check operation-level security
            if (restSpec.getPaths() != null) {
                int opsWithSecurity = 0;
                for (Map.Entry<String, PathItem> pathEntry : restSpec.getPaths().entrySet()) {
                    PathItem pathItem = pathEntry.getValue();
                    if (pathItem.getPost() != null && pathItem.getPost().getSecurity() != null && !pathItem.getPost().getSecurity().isEmpty()) {
                        opsWithSecurity++;
                        log.info("🔐 [BEFORE SAVE] POST {} has security: {}", pathEntry.getKey(), pathItem.getPost().getSecurity());
                    }
                    if (pathItem.getGet() != null && pathItem.getGet().getSecurity() != null && !pathItem.getGet().getSecurity().isEmpty()) {
                        opsWithSecurity++;
                        log.info("🔐 [BEFORE SAVE] GET {} has security: {}", pathEntry.getKey(), pathItem.getGet().getSecurity());
                    }
                }
                if (opsWithSecurity == 0) {
                    log.warn("⚠️ [BEFORE SAVE] No operations have security field!");
                }
            }

            // Object를 Map으로 변환
            Map<String, Object> map = Json31.mapper().convertValue(
                    specToSave,
                    new TypeReference<Map<String, Object>>() {}
            );
            
            // Debug: Check if securitySchemes survived conversion
            @SuppressWarnings("unchecked")
            Map<String, Object> components = (Map<String, Object>) map.get("components");
            if (components != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");
                if (securitySchemes != null) {
                    log.info("🔐 [AFTER CONVERT] SecuritySchemes in map: {}", securitySchemes.keySet());
                } else {
                    log.warn("⚠️ [AFTER CONVERT] SecuritySchemes lost during Json31 conversion!");
                }
            }

            // YAML로 직렬화
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);

            Yaml yaml = new Yaml(options);
            String yamlContent = yaml.dump(map);

            // getSpecFilePath()를 활용해서 파일 경로 계산
            String specPath = getSpecFilePath();
            // "/ouroboros/rest/ourorest.yml" -> "ouroboros/rest/ourorest.yml"
            String relativePath = specPath.startsWith("/") ? specPath.substring(1) : specPath;
            Path filePath = Paths.get(RESOURCE_PATH, relativePath);

            Files.createDirectories(filePath.getParent());

            try (FileWriter writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8)) {
                writer.write(yamlContent);
            }
            log.info("Successfully saved YAML to: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to serialize and save OuroApiSpec to YAML", e);
            throw new IllegalStateException("Failed to serialize and save spec to YAML", e);
        }
    }
}