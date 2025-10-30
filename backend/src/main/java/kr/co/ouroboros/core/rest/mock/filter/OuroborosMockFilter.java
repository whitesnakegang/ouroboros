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
 * This filter runs before Spring Security (order = -101) to intercept mock endpoints before
 * authentication/authorization checks. Mock endpoints have no real Controller implementation,
 * so they must be handled before Handler Mapping to avoid 404 errors.
 * <p>
 * Execution flow:
 * 1. Routing: Check if endpoint is registered as mock (direct registry lookup)
 * 2. Validation: Validate request via MockValidationService (delegated to service)
 * 3. Response: Generate and return mock response (direct processing)
 *
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class OuroborosMockFilter implements Filter{
    private final RestMockRegistry registry;
    private final MockValidationService validationService;
    private final SchemaMockBuilder schemaMockBuilder;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    /**
     * Intercepts incoming HTTP requests to route, validate, and serve mock responses for registered endpoints,
     * or delegates to the next filter when no mock matches.
     *
     * If a matching mock endpoint is found the method validates the request and, on success, generates and writes
     * the configured mock response (including headers, status, and serialized body). If validation fails it sends
     * an error response and does not continue the filter chain. If no mock endpoint is found this filter delegates
     * to the provided FilterChain.
     *
     * @param req   the incoming ServletRequest (expected to be an HttpServletRequest)
     * @param res   the outgoing ServletResponse (expected to be an HttpServletResponse)
     * @param chain the filter chain to invoke when no mock endpoint is matched
     * @throws IOException      if an I/O error occurs while writing the response
     * @throws ServletException if the request could not be handled by the filter chain
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Phase 1: Routing - Check if endpoint should be mocked
        Optional<EndpointMeta> metaOpt = registry.find(request.getRequestURI(), request.getMethod());

        if (metaOpt.isEmpty()) {
            // Not a mock endpoint - pass through to next filter
            chain.doFilter(req, res);
            return;
        }

        EndpointMeta meta = metaOpt.get();
        log.debug("Mock endpoint matched: {} {}", meta.getMethod(), meta.getPath());

        // Phase 2: Validation - Delegate to validation service
        ValidationResult validationResult = validationService.validate(request, meta);

        if (!validationResult.isValid()) {
            // Validation failed - send error response and stop
            sendError(response, validationResult.getStatusCode(), validationResult.getMessage());
            return;
        }

        // Phase 3: Response - Generate and send mock response
        respond(response, request, meta);
        // Do NOT call chain.doFilter() - execution stops here for mock endpoints
    }

    /**
     * Generate and send a mock HTTP response for the given endpoint metadata.
     *
     * Selects the endpoint's 200 response definition, builds a mock body from its schema (if defined),
     * applies defined headers and status, determines the response content type (falling back to the
     * request's Accept header when needed), serializes the body to JSON or XML, and writes it to the response.
     *
     * @param request the HTTP request; used to inspect the Accept header when content type is not specified
     * @param meta    endpoint metadata containing response definitions, headers, body schema, and status code
     * @throws IOException if writing the response fails
     */
    private void respond(HttpServletResponse response, HttpServletRequest request, EndpointMeta meta)
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

        // Generate mock body
        Object body = null;
        if (responseMeta.getBody() != null && !responseMeta.getBody().isEmpty()) {
            body = schemaMockBuilder.build(responseMeta.getBody());
        }

        // Determine content type
        String contentType = responseMeta.getContentType();
        String accept = request.getHeader("Accept");

        if (contentType == null) {
            contentType = (accept != null && accept.toLowerCase().contains("xml"))
                    ? "application/xml"
                    : "application/json";
        }

        response.setContentType(contentType + ";charset=UTF-8");
        response.setStatus(responseMeta.getStatusCode() > 0 ? responseMeta.getStatusCode() : statusCode);

        // Serialize and send response
        String bodyText = "";
        if (body != null) {
            if (contentType.contains("xml")) {
                bodyText = xmlMapper.writeValueAsString(body);
            } else {
                bodyText = objectMapper.writeValueAsString(body);
            }
        }

        response.getWriter().write(bodyText);
        log.debug("Mock response sent: {} {} -> {}", meta.getMethod(), meta.getPath(), statusCode);
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
}