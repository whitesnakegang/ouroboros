package kr.co.ouroboros.core.global.manager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

@Component
public class OuroApiSpecManager {

    private final Map<Protocol, OuroApiSpec> apiCache = new ConcurrentHashMap<>();
    private final Map<Protocol, OuroProtocolHandler> handlers;

    // 'classpath:' 경로에서 리소스를 읽기 위해 주입
    private final ResourceLoader resourceLoader;

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
     * Processes an API spec for the given protocol by reconciling the provided YAML with the
     * current runtime state, persisting any validated updates back to the protocol's spec resource,
     * and updating the in-memory cache.
     *
     * @param protocol        the protocol identifier whose spec is being processed
     * @param yamlFileContent the YAML content of the spec file to be validated and reconciled with
     *                        the current state
     */
    public void processAndCacheSpec(Protocol protocol, String yamlFileContent) {
        OuroProtocolHandler handler = getHandler(protocol);

        // 1. 파일(YAML) 스펙 파싱
        OuroApiSpec fileSpec = handler.loadFromFile(yamlFileContent);

        // 2. 코드 스캔
        OuroApiSpec scannedSpec = handler.scanCurrentState();

        // 3. 불일치 검증
        // 비교 후, 최종적으로 저장할 ApiSpec 반환
        OuroApiSpec validationResult = handler.validate(fileSpec, scannedSpec);

        // 4. 스캔한 최신 스펙을 YAML 문자열로 변환
        String updatedYaml = handler.serializeToYaml(validationResult);

        // 5. .yml 파일 갱신
        saveYamlToResources(handler.getSpecFilePath(), updatedYaml);

        // 6. 캐시 최신화
        apiCache.put(protocol, scannedSpec);
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
     * Scan the current state for the given protocol and cache the resulting API spec.
     *
     * @param protocol the protocol identifier used to select the protocol handler
     * @return the scanned OuroApiSpec that was stored in the cache
     */
    private OuroApiSpec findAndCacheSpecOnDemand(Protocol protocol) {
        OuroApiSpec scannedSpec = getHandler(protocol).scanCurrentState();
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
     * Persist the given YAML content to the specified classpath resource path.
     * <p>
     * Writes the provided YAML string to the resource location identified by filePath
     * (classpath-relative), creating parent directories if necessary and overwriting any existing
     * file using UTF-8 encoding.
     *
     * @param filePath the classpath-relative path to the YAML resource to write (e.g.,
     *                 "specs/protocol.yml")
     * @param content  the YAML content to be written to the resource
     */
    private void saveYamlToResources(String filePath, String content) {
        // (TODO) 파일 저장 로직 구현
    }
}