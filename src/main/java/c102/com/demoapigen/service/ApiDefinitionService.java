package c102.com.demoapigen.service;

import c102.com.demoapigen.model.ApiDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiDefinitionService {

    private final ApiYamlParser apiYamlParser;
    private final DynamicEndpointRegistrar dynamicEndpointRegistrar;

    private static final String API_YML_PATH = "classpath:api.yml";
    private static final String API_YML_FILE_PATH = "src/main/resources/api.yml";

    /**
     * Load the API definition from the configured classpath YAML.
     *
     * Parses the YAML at "classpath:api.yml" and returns the resulting ApiDefinition.
     *
     * @return the parsed ApiDefinition loaded from the classpath YAML
     */
    public ApiDefinition loadApiDefinition() {
        return apiYamlParser.parseApiYaml(API_YML_PATH);
    }

    /**
     * Persists the given API definition to the service's configured API YAML file.
     *
     * Writes the provided ApiDefinition as YAML to the file system at the service's configured path.
     *
     * @param apiDefinition the API definition to save
     * @throws IOException if an I/O error occurs while creating directories or writing the file
     */
    public void saveApiDefinition(ApiDefinition apiDefinition) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(
            YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build()
        );

        File file = new File(API_YML_FILE_PATH);

        // Ensure parent directory exists
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        yamlMapper.writeValue(file, apiDefinition);
        log.info("API definition saved to: {}", file.getAbsolutePath());
    }

    /**
     * Reloads dynamic API endpoints so changes to the API definition are applied.
     */
    public void reloadEndpoints() {
        log.info("Reloading endpoints...");
        dynamicEndpointRegistrar.registerEndpoints();
    }
}