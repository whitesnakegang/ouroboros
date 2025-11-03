package kr.co.ouroboros.core.rest.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import kr.co.ouroboros.core.rest.common.dto.OuroRestApiSpec;
import org.yaml.snakeyaml.Yaml;

/**
 * 테스트용 리소스 로더
 * YAML/JSON 파일을 classpath에서 로드하여 OuroRestApiSpec으로 변환
 */
public class TestResourceLoader {

    private static final String TEST_RESOURCE_DIR = "test-fixtures/";
    private final ObjectMapper objectMapper;
    private final Yaml yaml;

    /**
     * Creates a TestResourceLoader configured with default Jackson {@code ObjectMapper}
     * and SnakeYAML {@code Yaml} instances for parsing JSON and YAML test fixtures.
     */
    public TestResourceLoader() {
        this.objectMapper = new ObjectMapper();
        this.yaml = new Yaml();
    }

    /**
     * Load a JSON test fixture from the test-fixtures directory and parse it into an OuroRestApiSpec.
     *
     * @param filename the filename relative to the "test-fixtures/" directory
     * @return the deserialized OuroRestApiSpec represented by the JSON file
     * @throws Exception if the resource cannot be read or the JSON cannot be parsed
     */
    public OuroRestApiSpec loadJsonFromResource(String filename) throws Exception {
        String content = loadResourceAsString(TEST_RESOURCE_DIR + filename);
        return objectMapper.readValue(content, OuroRestApiSpec.class);
    }

    /**
     * Load a YAML test resource and convert it to an OuroRestApiSpec.
     *
     * @param filename the resource file path relative to "test-fixtures/" (may include subdirectories)
     * @return the parsed OuroRestApiSpec instance
     */
    public OuroRestApiSpec loadYamlFromResource(String filename) throws Exception {
        String content = loadResourceAsString(TEST_RESOURCE_DIR + filename);
        
        // YAML을 Map으로 파싱 후 JSON으로 변환
        java.util.Map<String, Object> yamlMap = yaml.load(content);
        String jsonContent = objectMapper.writeValueAsString(yamlMap);
        
        return objectMapper.readValue(jsonContent, OuroRestApiSpec.class);
    }

    /**
     * Load a response YAML test fixture from the classpath and convert it to an OuroRestApiSpec.
     *
     * @param filename the YAML file name located under "test-fixtures/response/"
     * @return the parsed OuroRestApiSpec represented by the YAML file
     */
    public OuroRestApiSpec loadResponseTest(String filename) throws Exception {
        return loadYamlFromResource("response/" + filename);
    }

    /**
     * Load a schema test YAML resource from test-fixtures/schema/ and convert it to an OuroRestApiSpec.
     *
     * @param filename relative filename under test-fixtures/schema/ (for example "my-schema.yaml")
     * @return the deserialized OuroRestApiSpec
     * @throws Exception if the resource cannot be found, read, or deserialized
     */
    public OuroRestApiSpec loadSchemaTest(String filename) throws Exception {
        return loadYamlFromResource("schema/" + filename);
    }

    /**
     * Load a classpath resource and return its contents as a UTF-8 string.
     *
     * @param resourcePath the classpath-relative path to the resource
     * @return the resource contents decoded using UTF-8
     * @throws IllegalArgumentException if the resource cannot be found on the classpath
     */
    private String loadResourceAsString(String resourcePath) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if( classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        
        if (inputStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        
        try (inputStream) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
