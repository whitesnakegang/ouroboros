package kr.co.ouroboros.core.rest.handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Locale;
import java.util.Map;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@RequiredArgsConstructor
@Component
@Slf4j
public class OuroRestHandler implements OuroProtocolHandler {

    private final OpenAPIService openAPIService;
    private final RestSpecSyncPipeline pipeline;

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true); // DTO에 @JsonIgnoreProperties로 안전


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
     * Scans the running application's OpenAPI model and produces an API specification.
     *
     * <p>Fetches the cached OpenAPI model for Locale.KOREA and converts it into an OuroRestApiSpec.
     * If the model contains an info object with a missing version, sets that version to "v1".</p>
     *
     * @return the scanned REST API specification
     * @throws IllegalStateException if the OpenAPI model cannot be retrieved or converted
     */
    @Override
    public OuroApiSpec scanCurrentState() {
        try {
            OpenAPI model = openAPIService.getCachedOpenAPI(Locale.KOREA);
            log.info("OpenAPI model : {}", model);

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
     * Produces a synchronized API specification by reconciling the specification loaded from file with the currently scanned specification.
     *
     * @param fileSpec    the API specification parsed from the repository/file source
     * @param scannedSpec the API specification obtained from the running application's OpenAPI model
     * @return            the resulting OuroApiSpec that represents the reconciled/synchronized API specification
     */
    @Override
    public OuroApiSpec synchronize(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {
        OuroApiSpec validate = pipeline.validate(fileSpec, scannedSpec);
        return null;
    }

    @Override
    public String serializeToYaml(OuroApiSpec specToSave) {
        return "";
    }
}