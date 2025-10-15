package c102.com.demoapigen.service;

import c102.com.demoapigen.model.ApiDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ApiYamlParser {

    private final ObjectMapper yamlMapper;
    private final PathMatchingResourcePatternResolver resourceResolver;

    /**
     * Create a parser configured to read API definitions from YAML resources.
     *
     * Initializes a YAML-capable Jackson ObjectMapper and a PathMatchingResourcePatternResolver
     * for locating YAML resources by path patterns.
     */
    public ApiYamlParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.resourceResolver = new PathMatchingResourcePatternResolver();
    }

    /**
     * Loads an API definition from the given YAML resource path and returns its deserialized ApiDefinition.
     *
     * @param resourcePath the resource location or pattern to resolve the YAML file
     * @return the deserialized ApiDefinition, or an empty ApiDefinition whose endpoints list is initialized when the resource is missing or cannot be parsed
     */
    public ApiDefinition parseApiYaml(String resourcePath) {
        try {
            Resource resource = resourceResolver.getResource(resourcePath);
            if (!resource.exists()) {
                log.info("API YAML file not found at: {}. Starting with empty endpoint list.", resourcePath);
                return createEmptyApiDefinition();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                ApiDefinition apiDefinition = yamlMapper.readValue(inputStream, ApiDefinition.class);
                log.info("Successfully parsed API YAML from: {}", resourcePath);
                return apiDefinition;
            }
        } catch (IOException e) {
            log.error("Failed to parse API YAML from: {}. Starting with empty endpoint list.", resourcePath, e);
            return createEmptyApiDefinition();
        }
    }

    /**
     * Create an ApiDefinition with its endpoints list initialized.
     *
     * @return an ApiDefinition whose endpoints list is initialized to an empty ArrayList
     */
    private ApiDefinition createEmptyApiDefinition() {
        ApiDefinition definition = new ApiDefinition();
        definition.setEndpoints(new java.util.ArrayList<>());
        return definition;
    }
}