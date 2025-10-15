package c102.com.demoapigen.service;

import c102.com.demoapigen.model.Endpoint;
import c102.com.demoapigen.model.StatusResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RestController
@RequiredArgsConstructor
public class DynamicEndpointController {

    private final DummyDataGenerator dummyDataGenerator;
    private final Map<String, Endpoint> endpointRegistry = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Registers an Endpoint in the controller's registry under the given key.
     *
     * @param key registry key in the form "path:METHOD" used to look up this endpoint
     * @param endpoint the Endpoint definition to register
     */
    public void registerEndpoint(String key, Endpoint endpoint) {
        endpointRegistry.put(key, endpoint);
    }

    /**
     * Handle an incoming HTTP request against registered dynamic endpoints and produce a mocked HTTP response.
     *
     * Processes the request URI and method to find a matching endpoint, honors an optional `X-Mock-Status` header to force a status,
     * performs authentication when the endpoint requires it, and returns the endpoint's defined success response (the lowest 2xx)
     * populated with generated dummy data. If the endpoint has no defined responses, returns a 200 OK with a simple success message.
     *
     * @param httpRequest the incoming HttpServletRequest; its URI, method, headers (including `X-Mock-Status` and authentication headers),
     *                    and query parameters are used to resolve the endpoint and determine the response
     * @return a ResponseEntity containing:
     *         - a generated response body and the endpoint's status code for a matched success response;
     *         - a 404 Not Found body when no endpoint matches;
     *         - a 401 response when authentication is required but fails;
     *         - a forced status response when `X-Mock-Status` is present and valid;
     *         - a 500 error body when an internal error occurs
     */
    public ResponseEntity<?> handleRequest(HttpServletRequest httpRequest) {
        try {
            String path = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();

            log.info("Handling request for: {} {}", method, path);

            // endpointRegistry에서 path variable을 고려하여 endpoint 찾기
            Endpoint endpoint = findMatchingEndpoint(path, method);

            if (endpoint == null) {
                log.warn("No endpoint found for: {} {}", method, path);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Not Found", "message", "Endpoint not found"));
            }

            // X-Mock-Status 헤더 체크 (강제 status code 지정)
            String mockStatusHeader = httpRequest.getHeader("X-Mock-Status");
            if (mockStatusHeader != null && !mockStatusHeader.isEmpty()) {
                try {
                    int mockStatus = Integer.parseInt(mockStatusHeader);
                    log.info("Mock status header detected: {}", mockStatus);
                    return generateResponseForStatus(endpoint, mockStatus);
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-Mock-Status header value: {}", mockStatusHeader);
                }
            }

            // 자연스러운 로직 흐름
            // 인증 체크
            if (Boolean.TRUE.equals(endpoint.getRequiresAuth())) {
                boolean authFailed = checkAuthenticationFailed(httpRequest, endpoint);
                if (authFailed) {
                    log.info("Authentication failed, returning 401");
                    return generateResponseForStatus(endpoint, 401);
                }
            }

            // 정상 응답 (2xx 중 가장 낮은 status code)
            StatusResponse successResponse = findSuccessResponse(endpoint);
            if (successResponse != null) {
                Object responseData = dummyDataGenerator.generateDummyData(successResponse.getResponse());
                return ResponseEntity.status(successResponse.getStatusCode()).body(responseData);
            }

            // 응답 정의가 없으면 기본 200 OK
            return ResponseEntity.ok(Map.of("message", "Success"));

        } catch (Exception e) {
            log.error("Error handling request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate dummy data", "message", e.getMessage()));
        }
    }

    /**
     * Builds an HTTP response for the given status code using the endpoint's defined responses or a default message.
     *
     * @param endpoint   the endpoint whose response definitions should be consulted
     * @param statusCode the HTTP status code to produce
     * @return a ResponseEntity with the provided statusCode and a body containing either generated dummy data for a matching response definition or a map with a message indicating the status
     */
    private ResponseEntity<?> generateResponseForStatus(Endpoint endpoint, int statusCode) {
        if (endpoint.getResponses() == null || endpoint.getResponses().isEmpty()) {
            return ResponseEntity.status(statusCode)
                    .body(Map.of("message", "Response for status " + statusCode));
        }

        // 해당 status code의 응답 찾기
        StatusResponse statusResponse = endpoint.getResponses().stream()
                .filter(sr -> sr.getStatusCode() != null && sr.getStatusCode() == statusCode)
                .findFirst()
                .orElse(null);

        if (statusResponse != null && statusResponse.getResponse() != null) {
            Object responseData = dummyDataGenerator.generateDummyData(statusResponse.getResponse());
            return ResponseEntity.status(statusCode).body(responseData);
        }

        // 정의되지 않은 status code면 기본 메시지
        return ResponseEntity.status(statusCode)
                .body(Map.of("message", "Response for status " + statusCode));
    }

    /**
     * Selects the success response with the lowest 2xx status code from the given endpoint.
     *
     * @param endpoint the endpoint whose responses will be searched
     * @return the StatusResponse that has the smallest status code in the 200–299 range, or `null` if the endpoint has no responses or none in that range
     */
    private StatusResponse findSuccessResponse(Endpoint endpoint) {
        if (endpoint.getResponses() == null || endpoint.getResponses().isEmpty()) {
            return null;
        }

        // 2xx 중 가장 낮은 status code 찾기
        return endpoint.getResponses().stream()
                .filter(sr -> sr.getStatusCode() != null && sr.getStatusCode() >= 200 && sr.getStatusCode() < 300)
                .min((sr1, sr2) -> sr1.getStatusCode().compareTo(sr2.getStatusCode()))
                .orElse(null);
    }

    /**
     * Finds a registered Endpoint whose path pattern and HTTP method match the incoming request.
     *
     * @param requestPath the request URI path to match against registered endpoint patterns
     * @param method the HTTP method of the incoming request (e.g., "GET", "POST")
     * @return the matching Endpoint if found, or `null` if no registered endpoint matches
     */
    private Endpoint findMatchingEndpoint(String requestPath, String method) {
        for (Map.Entry<String, Endpoint> entry : endpointRegistry.entrySet()) {
            String key = entry.getKey();
            Endpoint endpoint = entry.getValue();

            // key format: "path:METHOD"
            String[] parts = key.split(":");
            if (parts.length == 2) {
                String pattern = parts[0];
                String endpointMethod = parts[1];

                // 메소드가 일치하고 path가 패턴과 매칭되면
                if (method.equalsIgnoreCase(endpointMethod) && pathMatcher.match(pattern, requestPath)) {
                    return endpoint;
                }
            }
        }
        return null;
    }

    /**
     * Determines whether authentication for the given request and endpoint fails.
     *
     * Selects the authentication method from the endpoint's `authType` (defaults to "bearer")
     * and validates the request using that method. For the "custom" type the endpoint's
     * `authHeader` value is used as the header name to validate.
     *
     * @param request the incoming HTTP request to validate
     * @param endpoint the endpoint definition containing authentication configuration
     * @return `true` if authentication failed, `false` otherwise
     */
    private boolean checkAuthenticationFailed(HttpServletRequest request, Endpoint endpoint) {
        String authType = endpoint.getAuthType();
        if (authType == null || authType.isEmpty()) {
            authType = "bearer"; // 기본값
        }

        switch (authType.toLowerCase()) {
            case "bearer":
                return checkBearerAuthFailed(request);

            case "basic":
                return checkBasicAuthFailed(request);

            case "apikey":
                return checkApiKeyAuthFailed(request);

            case "custom":
                return checkCustomAuthFailed(request, endpoint.getAuthHeader());

            default:
                return checkBearerAuthFailed(request);
        }
    }

    /**
     * Validates that the HTTP request contains an Authorization header with a Bearer token.
     *
     * @param request the incoming HTTP servlet request to inspect
     * @return `true` if the Authorization header is missing or does not start with `"Bearer "`, `false` otherwise
     */
    private boolean checkBearerAuthFailed(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Bearer token required but not provided");
            return true; // 인증 실패
        }
        log.info("Bearer token present");
        return false; // 인증 성공
    }

    /**
     * Checks whether the HTTP request fails Basic authentication.
     *
     * @param request the HTTP servlet request whose `Authorization` header is validated for Basic auth
     * @return `true` if the `Authorization` header is missing or does not start with `"Basic "`, `false` otherwise
     */
    private boolean checkBasicAuthFailed(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Basic auth required but not provided");
            return true; // 인증 실패
        }
        log.info("Basic auth present");
        return false; // 인증 성공
    }

    /**
     * Determines if the request lacks a valid API key.
     *
     * Looks for the API key first in the "api_key" query parameter, then in the "X-API-Key" header.
     *
     * @param request the incoming HTTP request to inspect for an API key
     * @return `true` if the API key is missing or empty, `false` otherwise
     */
    private boolean checkApiKeyAuthFailed(HttpServletRequest request) {
        // Query parameter 또는 Header에서 API Key 확인
        String apiKey = request.getParameter("api_key");
        if (apiKey == null) {
            apiKey = request.getHeader("X-API-Key");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("API key required but not provided");
            return true; // 인증 실패
        }
        log.info("API key present");
        return false; // 인증 성공
    }

    /**
     * Checks whether custom header-based authentication fails for the given request.
     *
     * Uses `headerName` (defaults to "X-Auth-Token" when null or empty) and validates that the header is present and non-empty.
     *
     * @param headerName name of the header to validate; if null or empty, "X-Auth-Token" is used
     * @return `true` if authentication failed (header missing or empty), `false` otherwise
     */
    private boolean checkCustomAuthFailed(HttpServletRequest request, String headerName) {
        if (headerName == null || headerName.isEmpty()) {
            headerName = "X-Auth-Token"; // 기본값
        }

        String authValue = request.getHeader(headerName);
        if (authValue == null || authValue.trim().isEmpty()) {
            log.warn("Custom auth header {} required but not provided", headerName);
            return true; // 인증 실패
        }
        log.info("Custom auth header {} present", headerName);
        return false; // 인증 성공
    }
}