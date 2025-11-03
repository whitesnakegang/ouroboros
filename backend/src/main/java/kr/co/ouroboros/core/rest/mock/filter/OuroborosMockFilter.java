package kr.co.ouroboros.core.rest.mock.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.global.mock.model.ResponseMeta;
import kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import kr.co.ouroboros.core.rest.mock.service.MockValidationService;
import kr.co.ouroboros.core.rest.mock.service.MockValidationService.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Unified mock filter that handles routing, validation, and response generation.
 * <p>
 * This filter runs at {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} to intercept
 * mock endpoints before Spring Security and all other filters. Mock endpoints have no real
 * Controller implementation, so they must be handled before Handler Mapping to avoid 404 errors.
 * <p>
 * Execution flow:
 * <ol>
 *   <li><b>Routing</b>: Check if endpoint is registered as mock (registry lookup)</li>
 *   <li><b>Validation</b>: Validate request via {@link MockValidationService}</li>
 *   <li><b>Response</b>: Generate and return mock response via {@link SchemaMockBuilder}</li>
 * </ol>
 * <p>
 * If endpoint is not mock or validation passes, generates mock response and stops filter chain.
 * Otherwise, passes request to next filter.
 *
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class OuroborosMockFilter implements Filter {
    private final RestMockRegistry registry;
    private final MockValidationService validationService;
    private final SchemaMockBuilder schemaMockBuilder;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // ===== Phase 1: Routing - Check if endpoint should be mocked =====
        Optional<EndpointMeta> metaOpt = registry.find(request.getRequestURI(), request.getMethod());

        if (metaOpt.isEmpty()) {
            // Not a mock endpoint - pass through to next filter
            chain.doFilter(req, res);
            return;
        }

        EndpointMeta meta = metaOpt.get();
        log.debug("Mock endpoint matched: {} {}", meta.getMethod(), meta.getPath());

        // ===== Request Body 파싱 (Validation 전에 미리 읽기) =====
        Map<String, Object> requestJson = null;
        if ("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod())) {
            try {
                // InputStream은 한 번만 읽을 수 있으므로 여기서 미리 읽음
                requestJson = objectMapper.readValue(request.getInputStream(), Map.class);
                log.debug("Request body parsed for merge: {}", requestJson);

                // Validation에서 사용할 수 있도록 attribute에 저장
                request.setAttribute("parsedRequestBody", requestJson);
            } catch (Exception e) {
                log.debug("No readable request body found (ignored): {}", e.getMessage());
            }
        }

        // ===== Phase 2: Validation - Delegate to validation service =====
        ValidationResult validationResult = validationService.validate(request, meta);

        if (!validationResult.valid()) {
            // Validation failed - send error response and stop
            sendError(response, validationResult.statusCode(), validationResult.message());
            return;
        }

        // ===== Phase 3: Response - Generate and send mock response =====
        respond(response, request, meta, requestJson);
        // Do NOT call chain.doFilter() - execution stops here for mock endpoints
    }

    /**
     * Generates mock response based on schema and sends to client.
     *
     * @param response    the HTTP response
     * @param request     the HTTP request (for Accept header)
     * @param meta        the endpoint metadata
     * @param requestJson the parsed request body (for deep merge)
     * @throws IOException if response writing fails
     */
    private void respond(HttpServletResponse response, HttpServletRequest request,
                         EndpointMeta meta, Map<String, Object> requestJson)
            throws IOException {

        int statusCode = 200;
        ResponseMeta responseMeta = meta.getResponses().get(statusCode);

        if (responseMeta == null) {
            log.error("No 200 response defined for endpoint: {} {}", meta.getMethod(), meta.getPath());
            sendError(response, 500, "No response definition found for " + meta.getPath());
            return;
        }

        // Set response headers
        if (responseMeta.getHeaders() != null) {
            for (var entry : responseMeta.getHeaders().entrySet()) {
                response.setHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        // ===== Faker 기반 Mock body 생성 =====
        Object body = null;
        if (responseMeta.getBody() != null && !responseMeta.getBody().isEmpty()) {
            body = schemaMockBuilder.build(responseMeta.getBody());
        }

        // ===== 요청 body와 merge (요청 필드가 있으면 덮어씀) =====
        if (body instanceof Map && requestJson != null) {
            deepMerge((Map<String, Object>) body, requestJson);
        }

        // ===== Content type 결정 =====
        String contentType = responseMeta.getContentType();
        String accept = request.getHeader("Accept");

        if (contentType == null) {
            contentType = (accept != null && accept.toLowerCase().contains("xml"))
                    ? "application/xml"
                    : "application/json";
        }

        response.setContentType(contentType + ";charset=UTF-8");
        response.setStatus(responseMeta.getStatusCode() > 0 ? responseMeta.getStatusCode() : statusCode);

        // ===== 직렬화 및 전송 =====
        String bodyText = "";
        if (body != null) {
            if (contentType.contains("xml")) {
                bodyText = xmlMapper.writeValueAsString(body);
            } else {
                bodyText = objectMapper.writeValueAsString(body);
            }
        }

        response.getWriter().write(bodyText);
        log.debug("Mock response sent with merged request: {} {} -> {}", meta.getMethod(), meta.getPath(), statusCode);
    }

    /**
     * Sends error response in JSON format.
     *
     * @param response   the HTTP response
     * @param statusCode the HTTP status code
     * @param message    the error message
     * @throws IOException if response writing fails
     */
    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, String> error = Map.of("error", message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * 두 Map을 깊이 병합합니다. source의 값이 target의 값을 덮어씁니다.
     *
     * @param target 대상 Map (Faker가 생성한 데이터)
     * @param source 소스 Map (요청 body 데이터)
     */
    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();

            if (sourceValue instanceof Map && target.get(key) instanceof Map) {
                // 양쪽 모두 Map이면 재귀적으로 병합
                deepMerge((Map<String, Object>) target.get(key), (Map<String, Object>) sourceValue);
            } else {
                // 그 외의 경우 source 값으로 덮어씀
                target.put(key, sourceValue);
            }
        }
    }
}