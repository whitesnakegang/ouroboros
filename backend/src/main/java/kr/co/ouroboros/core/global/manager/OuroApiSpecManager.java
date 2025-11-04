package kr.co.ouroboros.core.global.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json31;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Component
@Slf4j
public class OuroApiSpecManager {

    private final Map<Protocol, OuroApiSpec> apiCache = new ConcurrentHashMap<>();
    private final Map<Protocol, OuroProtocolHandler> handlers;

    // 'classpath:' 경로에서 리소스를 읽기 위해 주입
    private final ResourceLoader resourceLoader;

    private static final ObjectMapper objectMapper = Json31.mapper();
    
    /**
     * Constructs an OuroApiSpecManager, registering protocol handlers and storing the
     * ResourceLoader.
     *
     * @param handlerList    a list of OuroProtocolHandler instances to be indexed by each handler's
     *                       protocol string
     * @param resourceLoader the ResourceLoader used to read YAML resources from the classpath
     */
    @Autowired
    public OuroApiSpecManager(List<OuroProtocolHandler> handlerList,
            ResourceLoader resourceLoader) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(OuroProtocolHandler::getProtocol, Function.identity()));
        this.resourceLoader = resourceLoader;
    }

    /**
     * Reconciles a protocol's API spec YAML with the current runtime state, persists the validated spec,
     * and updates the in-memory cache for that protocol.
     *
     * Performs parsing of the provided YAML, scans the live runtime state, synchronizes differences
     * to produce the validated spec that will be persisted via the protocol handler, and caches the
     * scanned specification for subsequent reads.
     *
     * @param protocol        the protocol whose API specification is being processed
     * @param yamlFileContent the YAML content of the protocol's spec file to reconcile with the runtime state
     */
    public void processAndCacheSpec(Protocol protocol, String yamlFileContent) {
        OuroProtocolHandler handler = getHandler(protocol);

        // 1. 파일(YAML) 스펙 파싱
        OuroApiSpec fileSpec = handler.loadFromFile(yamlFileContent);

        // 2. 코드 스캔
        OuroApiSpec scannedSpec = handler.scanCurrentState();

        // 3. 불일치 검증
        // 비교 후, 최종적으로 저장할 ApiSpec 반환
        OuroApiSpec validationResult = handler.synchronize(fileSpec, scannedSpec);
        
        // 4. 스캔한 최신 스펙을 YAML로 직렬화하고 파일에 저장
        handler.saveYaml(validationResult);

        // 5. 캐시 최신화 (validationResult 사용 - 파일+코드 동기화 결과)
        apiCache.put(protocol, validationResult);
    }

    /**
     * Provide the cached API specification for the given protocol, scanning and caching it on
     * demand if not already cached.
     *
     * @param protocol the protocol identifier corresponding to a registered OuroProtocolHandler
     *                 (case-insensitive)
     * @return the OuroApiSpec for the specified protocol; if not present in the cache, the current
     * state is scanned, cached, and returned
     * @throws IllegalArgumentException if the protocol is not supported
     */
    public OuroApiSpec getApiSpec(Protocol protocol) {
        // 캐시가 없으면(초기화 실패 시) 스캔/생성 시도
        // (Runner가 먼저 실행되므로 대부분 캐시에서 바로 반환됨)
        return apiCache.computeIfAbsent(protocol, this::findAndCacheSpecOnDemand);
    }

    /**
     * Initialize and load the API specification for the given protocol from classpath resources and
     * process it into the in-memory cache.
     *
     * @param protocol the protocol identifier whose specification should be initialized
     * @throws IllegalArgumentException if the protocol is not supported
     */
    public void initializeProtocolOnStartup(Protocol protocol) {

        OuroProtocolHandler handler = getHandler(protocol);
        String filePath = handler.getSpecFilePath();

        // (Job 6) 리소스에서 YAML 파일 읽기
        String yamlFromFile = loadYamlFromResources(filePath);

        // 공통 로직 실행
        processAndCacheSpec(protocol, yamlFromFile);
    }

    /**
     * Process and cache API specification from a Map representation.
     * This overload is used after CUD operations in ServiceImpl classes.
     * Converts Map to YAML string and delegates to the main processAndCacheSpec method.
     *
     * @param protocol the protocol whose API specification is being processed
     * @param openApiDoc the OpenAPI document as a Map (modified by CUD operations)
     */
    public void processAndCacheSpec(Protocol protocol, Map<String, Object> openApiDoc) {
        String yamlContent = convertMapToYaml(openApiDoc);
        processAndCacheSpec(protocol, yamlContent);
    }

    /**
     * Convert an OuroApiSpec to a Map representation.
     * Used when reading cached specs and converting them to Map for ServiceImpl operations.
     *
     * @param spec the OuroApiSpec to convert
     * @return Map representation of the spec
     */
    public Map<String, Object> convertSpecToMap(OuroApiSpec spec) {
        return objectMapper.convertValue(spec, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Retrieve the protocol handler for the given protocol.
     *
     * @param protocol the protocol name (case-insensitive)
     * @return the {@code OuroProtocolHandler} associated with the protocol
     * @throws IllegalArgumentException if no handler is registered for the protocol
     */

    private OuroProtocolHandler getHandler(Protocol protocol) {
        OuroProtocolHandler handler = handlers.get(protocol);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        return handler;
    }

    /**
     * Load spec from file, synchronize with scanned state, and cache the result.
     * This is called when cache is empty and needs to be populated on-demand.
     *
     * @param protocol the protocol identifier used to select the protocol handler
     * @return the synchronized OuroApiSpec that was stored in the cache
     */
    private OuroApiSpec findAndCacheSpecOnDemand(Protocol protocol) {
        OuroProtocolHandler handler = getHandler(protocol);
        String filePath = handler.getSpecFilePath();

        // Try to load from file
        String yamlContent = loadYamlFromResources(filePath);

        if (yamlContent != null && !yamlContent.isEmpty()) {
            // File exists - process and cache (includes validation)
            processAndCacheSpec(protocol, yamlContent);
            return apiCache.get(protocol);
        }

        // File doesn't exist - scan only and cache
        log.warn("Spec file not found for protocol {}, using scanned state only", protocol);
        OuroApiSpec scannedSpec = handler.scanCurrentState();
        apiCache.put(protocol, scannedSpec);
        return scannedSpec;
    }

    /**
     * Load a YAML file from classpath resources and return its contents decoded as UTF-8.
     *
     * @param filePath the classpath-relative path to the YAML file (for example, "specs/foo.yml")
     * @return the file contents as a UTF-8 string, or an empty string if the resource does not
     * exist or cannot be read
     */
    private String loadYamlFromResources(String filePath) {
        try {
            Resource resource = resourceLoader.getResource(
                    ResourceLoader.CLASSPATH_URL_PREFIX + filePath);
            if (!resource.exists()) {
                return ""; // 빈 문자열 반환
            }
            try (InputStream is = resource.getInputStream()) {
                byte[] bdata = FileCopyUtils.copyToByteArray(is);
                return new String(bdata, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return ""; // 오류 발생 시 빈 문자열
        }
    }

    /**
     * Convert a Map to YAML string format.
     * Used to serialize Map representation back to YAML for file operations.
     *
     * @param doc the OpenAPI document as a Map
     * @return YAML string representation
     */
    private String convertMapToYaml(Map<String, Object> doc) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);
        return yaml.dump(doc);
    }
}