package kr.co.ouroboros.core.global.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OuroApiSpecManager {

    /**
     * 최종 API 스펙을 메모리에 캐싱
     */
    private final Map<String, OuroApiSpec> apiCache = new ConcurrentHashMap<>();

    /**
     * 모든 프로토콜 핸들러(전략)를 Map으로 관리
     */
    private final Map<String, OuroProtocolHandler> handlers;

    /**
     * Construct an OuroApiSpecManager by indexing provided protocol handlers into a map.
     *
     * @param handlerList a list of all Spring-registered {@code OuroProtocolHandler} implementations; each handler is stored in the manager's internal map keyed by the handler's {@code getProtocol()} value
     */
    @Autowired
    public OuroApiSpecManager(List<OuroProtocolHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(OuroProtocolHandler::getProtocol, Function.identity()));
    }

    /**
     * Provide the cached final API specification for the given protocol.
     *
     * @param protocol the protocol identifier (case-insensitive) whose API spec is requested
     * @return the cached {@link OuroApiSpec} for the protocol; if none is cached, scans the current state via the protocol handler, caches the resulting spec, and returns it
     */
    public OuroApiSpec getApiSpec(String protocol) {
        // 캐시에서 가져오고, 없으면 스캔해서 캐시/반환
        return apiCache.computeIfAbsent(protocol, this::findAndCacheSpec);
    }

    /**
     * Validate a submitted API YAML against the current scanned API state for the given protocol and update the in-memory spec.
     *
     * <p>Loads the provided YAML, reconciles it with the protocol's current scanned state, converts the validated spec back
     * to YAML for persistence, and refreshes the internal cache for the protocol.</p>
     *
     * @param protocol          protocol identifier (case-insensitive) used to select the appropriate handler
     * @param yamlFromFrontend  YAML content submitted from the frontend representing the API spec to validate
     * @throws IllegalArgumentException if the specified protocol is not supported
     */
    public void validateAndSave(String protocol, String yamlFromFrontend) {
        // 1. 프로토콜에 맞는 핸들러(전략) 선택
        OuroProtocolHandler handler = getHandler(protocol);

        // 2. YAML 파싱
        OuroApiSpec fileSpec = handler.loadFromFile(yamlFromFrontend);

        // 3. 코드 스캔
        OuroApiSpec scannedSpec = handler.scanCurrentState();

        // 4. 불일치 검증
        OuroApiSpec validatedSpec = handler.validate(fileSpec, scannedSpec);

        // 5. 스캔한 스펙을 YAML 문자열로 변환
        String updatedYaml = handler.saveToString(validatedSpec);

        // 6. (TODO) 이 updatedYaml을 실제 .yml 파일에 저장 (File I/O)

        // 7. 캐시도 최신화
        apiCache.put(protocol, validatedSpec);
    }

    /**
     * Retrieves the registered protocol handler for the given protocol name.
     *
     * @param protocol the protocol name (case-insensitive)
     * @return the {@code OuroProtocolHandler} registered for the protocol
     * @throws IllegalArgumentException if no handler is registered for the protocol
     */
    private OuroProtocolHandler getHandler(String protocol) {
        OuroProtocolHandler handler = handlers.get(protocol.toLowerCase());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        return handler;
    }

    /**
     * Obtain the latest API specification for the given protocol when a cache miss occurs.
     *
     * @param protocol the protocol name (case-insensitive)
     * @return the latest {@code OuroApiSpec} scanned for the specified protocol
     * @throws IllegalArgumentException if no handler is registered for the protocol
     */
    private OuroApiSpec findAndCacheSpec(String protocol) {
        // 캐시가 없을 땐 코드를 스캔한 최신본을 캐시
        return getHandler(protocol).scanCurrentState();
    }
}