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

    public TestResourceLoader() {
        this.objectMapper = new ObjectMapper();
        this.yaml = new Yaml();
    }

    /**
     * JSON 파일을 로드하여 OuroRestApiSpec으로 변환
     */
    public OuroRestApiSpec loadJsonFromResource(String filename) throws Exception {
        String content = loadResourceAsString(TEST_RESOURCE_DIR + filename);
        return objectMapper.readValue(content, OuroRestApiSpec.class);
    }

    /**
     * YAML 파일을 로드하여 OuroRestApiSpec으로 변환
     */
    public OuroRestApiSpec loadYamlFromResource(String filename) throws Exception {
        String content = loadResourceAsString(TEST_RESOURCE_DIR + filename);
        
        // YAML을 Map으로 파싱 후 JSON으로 변환
        java.util.Map<String, Object> yamlMap = yaml.load(content);
        String jsonContent = objectMapper.writeValueAsString(yamlMap);
        
        return objectMapper.readValue(jsonContent, OuroRestApiSpec.class);
    }

    /**
     * Response 테스트용 YAML 파일을 로드하여 OuroRestApiSpec으로 변환
     */
    public OuroRestApiSpec loadResponseTest(String filename) throws Exception {
        return loadYamlFromResource("response/" + filename);
    }

    /**
     * Schema 테스트용 YAML 파일을 로드하여 OuroRestApiSpec으로 변환
     */
    public OuroRestApiSpec loadSchemaTest(String filename) throws Exception {
        return loadYamlFromResource("schema/" + filename);
    }

    /**
     * 리소스 파일을 String으로 로드
     */
    private String loadResourceAsString(String resourcePath) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
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

