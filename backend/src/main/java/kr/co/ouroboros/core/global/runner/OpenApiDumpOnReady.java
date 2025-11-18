package kr.co.ouroboros.core.global.runner;

import java.util.List;
import java.util.Map;

import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.manager.OuroApiSpecManager;
import kr.co.ouroboros.core.rest.spec.validator.OurorestYamlValidator;
import kr.co.ouroboros.core.websocket.spec.validator.OurowebsocketYamlValidator;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import kr.co.ouroboros.core.rest.mock.service.RestMockLoaderService;
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
    private final RestMockLoaderService mockLoaderService;
    private final RestMockRegistry mockRegistry;
    private final OurorestYamlValidator ourorestValidator;
    private final OurowebsocketYamlValidator ourowebsocketValidator;

    /**
     * Initializes API specifications and protocol handlers once the application is ready.
     * <p>
     * Execution order:
     * <ol>
     *   <li>Validate and enrich ourorest.yml (non-blocking)</li>
     *   <li>Validate and enrich ourowebsocket.yml (non-blocking)</li>
     *   <li>Fetch OpenAPI JSON from /v3/api-docs</li>
     *   <li>Initialize each protocol handler</li>
     * </ol>
     * <p>
     * All errors are logged but do not prevent application startup.
     */
    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onReady() {
        // Step 1: Validate and enrich ourorest.yml
        try {
            ourorestValidator.validateAndEnrich();
        } catch (Exception e) {
            log.error("❌ ourorest.yml validation failed: {}", e.getMessage(), e);
            log.error("⚠️  Continuing with application startup...");
        }

        // Step 1.5: Validate and enrich ourowebsocket.yml
        try {
            ourowebsocketValidator.validateAndEnrich();
        } catch (Exception e) {
            log.error("❌ ourowebsocket.yml validation failed: {}", e.getMessage(), e);
            log.error("⚠️  Continuing with application startup...");
        }

        // Step 2: Fetch OpenAPI docs
        int port = webCtx.getWebServer().getPort();
        String ctx = webCtx.getServletContext().getContextPath();
        String base = "http://localhost:" + port + (ctx != null ? ctx : "");
        String url = base + "/v3/api-docs";

        var rt = new org.springframework.web.client.RestTemplate();
        String json = rt.getForObject(url, String.class);
        log.info("Fetched /v3/api-docs: {} bytes", json != null ? json.length() : 0);

        // Step 3: Initialize protocol handlers
        for (OuroProtocolHandler h : handlers) {
            try {
                specManager.initializeProtocolOnStartup(h.getProtocol());
            } catch (Exception e) {
                log.error("[{}] 초기화 실패: {}", h.getProtocol(), e.getMessage(), e);
            }
        }

        // Load mock endpoints from YAML into registry
        try {
            log.info("Loading mock endpoints from YAML...");
            Map<String, EndpointMeta> endpoints = mockLoaderService.loadFromYaml();
            endpoints.values().forEach(mockRegistry::register);
            log.info("Successfully loaded {} mock endpoints into registry", endpoints.size());
        } catch (Exception e) {
            log.error("Failed to load mock registry", e);
        }
    }
}
