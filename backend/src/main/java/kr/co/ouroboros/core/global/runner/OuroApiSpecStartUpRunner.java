package kr.co.ouroboros.core.global.runner;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import java.util.Locale;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
//@Component
// TODO delete class
public class OuroApiSpecStartUpRunner implements ApplicationRunner {

    private final OpenAPIService openAPIService;
    private final OuroApiSpecManager specManager;
    private final List<OuroProtocolHandler> handlers;

    /**
     * Initializes OpenAPI for Locale.KOREA and invokes protocol initialization for each registered handler on application startup.
     *
     * For each handler, delegates initialization to the spec manager and logs errors for any handler that fails without preventing remaining handlers from being processed.
     *
     * @param args the application arguments provided at startup
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        openAPIService.build(Locale.KOREA);

        OpenAPI cachedOpenAPI = openAPIService.getCachedOpenAPI(Locale.KOREA);

        log.info("캐시된 OpenAPI 정보  : {}", cachedOpenAPI);

        // (Job 3-11)
        // 등록된 모든 프로토콜 핸들러를 순회하며
        // 매니저에게 초기화 작업을 위임
        for (OuroProtocolHandler handler : handlers) {
            try {
                specManager.initializeProtocolOnStartup(handler.getProtocol());
            } catch (Exception e) {
                // 특정 프로토콜 초기화에 실패해도 서버가 죽지 않도록 처리
                log.error("[{}] 프로토콜 스펙 초기화 실패: {}", handler.getProtocol(), e.getMessage(), e);
            }
        }
    }
}