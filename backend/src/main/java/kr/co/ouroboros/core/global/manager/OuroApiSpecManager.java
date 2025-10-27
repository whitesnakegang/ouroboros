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
     * Spring이 @Component로 등록된 모든 OuroProtocolHandler 구현체를
     * List로 주입받아, Map으로 변환하여 저장
     */
    @Autowired
    public OuroApiSpecManager(List<OuroProtocolHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(OuroProtocolHandler::getProtocol, Function.identity()));
    }

    /**
     * 컨트롤러가 호출할 메서드 (캐시된 스펙 제공)
     */
    public OuroApiSpec getApiSpec(String protocol) {
        // 캐시에서 가져오고, 없으면 스캔해서 캐시/반환
        return apiCache.computeIfAbsent(protocol, this::findAndCacheSpec);
    }

    /**
     *  스캔후 검증
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

    private OuroProtocolHandler getHandler(String protocol) {
        OuroProtocolHandler handler = handlers.get(protocol.toLowerCase());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        return handler;
    }

    /**
     * 캐시가 없을 때 (Cache Miss) 실행되는 함수
     */
    private OuroApiSpec findAndCacheSpec(String protocol) {
        // 캐시가 없을 땐 코드를 스캔한 최신본을 캐시
        return getHandler(protocol).scanCurrentState();
    }
}
