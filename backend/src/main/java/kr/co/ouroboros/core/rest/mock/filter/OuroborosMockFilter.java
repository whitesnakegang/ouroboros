package kr.co.ouroboros.core.rest.mock.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import kr.co.ouroboros.core.rest.mock.model.RestResponseMeta;
import kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import kr.co.ouroboros.core.rest.mock.service.MockValidationService;
import kr.co.ouroboros.core.rest.mock.service.MockValidationService.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
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

    /**
         * Handle HTTP requests for registered mock endpoints by validating the request and producing a mock response;
         * if the request does not match a registered mock endpoint, delegate to the provided filter chain.
         *
         * <p>When the incoming request is a POST, PUT, or PATCH and an expected request content type is defined for the
         * endpoint, the request body is parsed (JSON, XML, form data, or multipart indicator) and stored as the request
         * attribute "parsedRequestBody". If validation fails, an error response is sent using the validation result's
         * status and message and processing stops; on successful validation a mock response is generated and written to
         * the response, and the filter chain is not continued.</p>
         *
         * @throws IOException      if an I/O error occurs while reading the request or writing the response
         * @throws ServletException if a servlet error occurs during filtering
         */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // ===== 1단계: 라우팅 - Mock 엔드포인트인지 확인 =====
        Optional<EndpointMeta> metaOpt = registry.find(request.getRequestURI(), request.getMethod());

        if (metaOpt.isEmpty()) {
            // Mock 엔드포인트가 아니면 다음 필터로 전달
            chain.doFilter(req, res);
            return;
        }

        EndpointMeta meta = metaOpt.get();

        // ===== 2단계: Request Body 파싱 (검증 전에 미리 읽기) =====
        Object requestJson = null;
        if ("POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod())) {
            try {
                String expectedContentType = meta.getRequestBodyContentType();
                String actualContentType = request.getContentType();

                // Content-Type이 없으면 JSON으로 기본 처리
                if (expectedContentType == null) {
                    expectedContentType = "application/json";
                }

                // Registry에 등록된 Content-Type에 따라 파싱
                if (expectedContentType.contains("multipart/form-data")) {
                    // ===== multipart/form-data 처리 =====
                    if (actualContentType != null && actualContentType.contains("multipart/form-data")) {
                        // Mock 응답: 파일이 정상적으로 전송되었다고 가정
                        Map<String, Object> mockFormData = new HashMap<>();
                        mockFormData.put("_multipart", true);
                        requestJson = mockFormData;
                    } else {
                        log.warn("Expected multipart/form-data but got: {}", actualContentType);
                        requestJson = null;
                    }
                } else if (expectedContentType.contains("application/x-www-form-urlencoded")) {
                    // ===== application/x-www-form-urlencoded 처리 =====
                    Map<String, Object> formData = new HashMap<>();
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                        String[] values = entry.getValue();
                        formData.put(entry.getKey(), values.length == 1 ? values[0] : Arrays.asList(values));
                    }
                    requestJson = formData;
                } else {
                    // ===== XML 처리 =====
                    byte[] bodyBytes = request.getInputStream().readAllBytes();
                    if (bodyBytes.length > 0) {
                        String body = new String(bodyBytes, StandardCharsets.UTF_8);
                        if (expectedContentType.contains("application/xml")) {
                            requestJson = xmlMapper.readValue(body, Object.class);
                        } else {
                            requestJson = objectMapper.readValue(body, Object.class);
                        }
                    } else {
                        log.warn("Request body is empty (0 bytes)");
                    }
                }
                request.setAttribute("parsedRequestBody", requestJson);
            } catch (Exception e) {
                log.error("Failed to parse request body", e);
                // 파싱 실패 시 attribute에 null을 명시적으로 저장
                request.setAttribute("parsedRequestBody", null);
            }
        }
        // ===== 3단계: 검증 - Validation Service에 위임 =====
        ValidationResult validationResult = validationService.validate(request, meta);

        if (!validationResult.valid()) {
            // 검증 실패 - 에러 응답 전송 후 종료
            sendError(response, validationResult.statusCode(), validationResult.message());
            return;
        }

        // ===== 4단계: Mock 응답 생성 및 전송 =====
        respond(response, request, meta, requestJson);
        // chain.doFilter() 호출하지 않음 - mock 엔드포인트는 여기서 실행 종료
    }

    /**
     * Build and send the mock HTTP response defined by the endpoint metadata.
     *
     * Sets response headers from the response definition, generates a mock body from the response schema (if present),
     * deep-merges fields from the parsed request body into the generated body when both are maps (request values override),
     * chooses Content-Type from the response metadata or the request Accept header (XML if Accept indicates XML, otherwise JSON),
     * applies UTF-8 charset, uses the response definition's status code (or 200 if not specified), serializes the body to XML or JSON, and writes it to the response.
     *
     * @param response    the HTTP response to write to
     * @param request     the HTTP request (used to inspect Accept header)
     * @param meta        endpoint metadata containing response definition and headers
     * @param requestBody parsed request body (used for deep merge into generated mock body)
     * @throws IOException if writing the serialized response to the client fails
     */
    private void respond(HttpServletResponse response, HttpServletRequest request,
                         EndpointMeta meta, Object requestBody)
            throws IOException {

        int statusCode = determineSuccessStatusCode(meta);
        RestResponseMeta responseMeta = meta.getResponses().get(statusCode);

        if (responseMeta == null) {
            log.error("No 200 response defined for endpoint: {} {}", meta.getMethod(), meta.getPath());
            sendError(response, 500, "No response definition found for " + meta.getPath());
            return;
        }

        // 응답 헤더 설정
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

        // ===== 요청 body와 병합 (요청 필드가 있으면 덮어씀) =====
        if (body instanceof Map<?, ?> bodyMap && requestBody instanceof Map<?, ?> requestMap) {
            deepMerge((Map<String, Object>) bodyMap, (Map<String, Object>) requestMap);
        }

        // ===== Content-Type 결정 =====
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
     * Determine the appropriate success status code (2xx) from the endpoint's response definitions.
     *
     * Priority order: 200 > 201 > 204 > other 2xx > first available response
     *
     * @param meta endpoint metadata containing response definitions
     * @return the selected status code, or null if no responses are defined
     */
    private Integer determineSuccessStatusCode(EndpointMeta meta) {
        if (meta.getResponses() == null || meta.getResponses().isEmpty()) {
            return null;
        }

        // Priority 1: 200 OK
        if (meta.getResponses().containsKey(200)) {
            return 200;
        }

        // Priority 2: 201 Created
        if (meta.getResponses().containsKey(201)) {
            return 201;
        }

        // Priority 3: 204 No Content
        if (meta.getResponses().containsKey(204)) {
            return 204;
        }

        // Priority 4: Any other 2xx response
        for (Integer code : meta.getResponses().keySet()) {
            if (code >= 200 && code < 300) {
                return code;
            }
        }

        // Fallback: First available response (even if not 2xx)
        return meta.getResponses().keySet().iterator().next();
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
     * Recursively merges entries from `source` into `target`, overriding `target` values with `source` values.
     *
     * The merge mutates the `target` map in place. When a value for the same key is a map in both
     * `target` and `source`, the maps are merged recursively; otherwise the `source` value replaces
     * the `target` value.
     *
     * @param target the map to be mutated with merged values (e.g., generated mock data)
     * @param source the map whose values take precedence and will overwrite or be merged into `target` (e.g., request body)
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