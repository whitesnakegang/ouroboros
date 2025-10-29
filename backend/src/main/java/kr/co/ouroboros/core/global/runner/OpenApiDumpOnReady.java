package kr.co.ouroboros.core.global.runner;

import java.util.List;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenApiDumpOnReady {

    private final org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext webCtx;
    private final OuroApiSpecManager specManager;
    private final List<OuroProtocolHandler> handlers;

    /**
     * Initializes API specifications and protocol handlers once the application is ready.
     *
     * <p>Fetches the local OpenAPI JSON from /v3/api-docs and logs its size, then invokes
     * {@code specManager.initializeProtocolOnStartup} for each registered {@code OuroProtocolHandler},
     * logging any per-handler initialization failures without aborting the overall process.</p>
     */
    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onReady() {
        int port = webCtx.getWebServer().getPort();
        String ctx = webCtx.getServletContext().getContextPath();
        String base = "http://localhost:" + port + (ctx != null ? ctx : "");
        String url = base + "/v3/api-docs";

        var rt = new org.springframework.web.client.RestTemplate();
        String json = rt.getForObject(url, String.class);
        log.info("Fetched /v3/api-docs: {} bytes", json != null ? json.length() : 0);

        // 여기서 JSON -> OuroRestApiSpec 역직렬화 후, 저장/검증 로직 실행
        // specManager.initializeProtocolOnStartup(...); 등
        for (OuroProtocolHandler h : handlers) {
            try {
                specManager.initializeProtocolOnStartup(h.getProtocol());
            } catch (Exception e) {
                log.error("[{}] 초기화 실패: {}", h.getProtocol(), e.getMessage(), e);
            }
        }
    }
}
