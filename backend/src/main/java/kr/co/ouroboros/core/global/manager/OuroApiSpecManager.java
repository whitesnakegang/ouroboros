package kr.co.ouroboros.core.global.manager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

@Component
public class OuroApiSpecManager {

    private final Map<String, OuroApiSpec> apiCache = new ConcurrentHashMap<>();
    private final Map<String, OuroProtocolHandler> handlers;

    // 'classpath:' 경로에서 리소스를 읽기 위해 주입
    private final ResourceLoader resourceLoader;

    @Autowired
    public OuroApiSpecManager(List<OuroProtocolHandler> handlerList, ResourceLoader resourceLoader) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(OuroProtocolHandler::getProtocol, Function.identity()));
        this.resourceLoader = resourceLoader;
    }

    /**
     * Startup Runner 또는 Controller가 호출할 공통 로직
     * (스캔 -> 검증 -> 파일 갱신 -> 캐싱)
     */
    public void processAndCacheSpec(String protocol, String yamlFileContent) {
        OuroProtocolHandler handler = getHandler(protocol);

        // 1. 파일(YAML) 스펙 파싱
        OuroApiSpec fileSpec = handler.loadFromFile(yamlFileContent);

        // 2. 코드 스캔
        OuroApiSpec scannedSpec = handler.scanCurrentState();

        // 3. 불일치 검증
        // 비교 후, 최종적으로 저장할 ApiSpec 반환
        OuroApiSpec validationResult = handler.validate(fileSpec, scannedSpec);

        // 4. 스캔한 최신 스펙을 YAML 문자열로 변환
        String updatedYaml = handler.saveToString(validationResult);

        // 5. .yml 파일 갱신
        saveYamlToResources(handler.getSpecFilePath(), updatedYaml);

        // 6. 캐시 최신화
        apiCache.put(protocol, scannedSpec);
    }

    /**
     * 컨트롤러가 호출할 메서드 (캐시된 스펙 제공)
     */
    public OuroApiSpec getApiSpec(String protocol) {
        // 캐시가 없으면(초기화 실패 시) 스캔/생성 시도
        // (Runner가 먼저 실행되므로 대부분 캐시에서 바로 반환됨)
        return apiCache.computeIfAbsent(protocol, this::findAndCacheSpecOnDemand);
    }

    /**
     * Startup Runner가 호출할 초기화 메서드
     */
    public void initializeProtocolOnStartup(String protocol) {

        OuroProtocolHandler handler = getHandler(protocol);
        String filePath = handler.getSpecFilePath();

        // (Job 6) 리소스에서 YAML 파일 읽기
        String yamlFromFile = loadYamlFromResources(filePath);

        // 공통 로직 실행
        processAndCacheSpec(protocol, yamlFromFile);
    }

    // --- Helper Methods ---

    private OuroProtocolHandler getHandler(String protocol) {
        OuroProtocolHandler handler = handlers.get(protocol.toLowerCase());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        return handler;
    }

    /**
     * 캐시가 없을 때만 스캔
     */
    private OuroApiSpec findAndCacheSpecOnDemand(String protocol) {
        OuroApiSpec scannedSpec = getHandler(protocol).scanCurrentState();
        apiCache.put(protocol, scannedSpec);
        return scannedSpec;
    }

    /**
     * 리소스 경로에서 .yml 파일 읽기
     */
    private String loadYamlFromResources(String filePath) {
        try {
            Resource resource = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + filePath);
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
     * 스캔한 내용을 .yml 파일에 다시 저장
     */
    private void saveYamlToResources(String filePath, String content) {
        // (TODO) 파일 저장 로직 구현
    }
}