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

    public void registerEndpoint(String key, Endpoint endpoint) {
        endpointRegistry.put(key, endpoint);
    }

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

    private boolean checkBearerAuthFailed(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Bearer token required but not provided");
            return true; // 인증 실패
        }
        log.info("Bearer token present");
        return false; // 인증 성공
    }

    private boolean checkBasicAuthFailed(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Basic auth required but not provided");
            return true; // 인증 실패
        }
        log.info("Basic auth present");
        return false; // 인증 성공
    }

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
