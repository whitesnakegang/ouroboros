package kr.co.ouroboros.core.rest.mock.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockValidationService {
    /**
     * Validates the request against endpoint requirements.
     * Priority order:
     * 1. X-Ouroboros-Error header (forced error response)
     * 2. Authentication headers (401 if missing)
     * 3. Required headers (400 if missing)
     * 4. Required query parameters (400 if missing)
     *
     * @param request the HTTP request
     * @param meta    the endpoint metadata
     * @return validation result
     */
    public ValidationResult validate(HttpServletRequest request, EndpointMeta meta) {
        // Priority 1: Check for forced error via X-Ouroboros-Error header
        String forcedError = request.getHeader("x-ouroboros-error");
        if (forcedError != null) {
            try {
                int errorCode = Integer.parseInt(forcedError);
                return ValidationResult.error(errorCode,
                        "Forced error response via X-Ouroboros-Error header");
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Ouroboros-Error header value: {}", forcedError);
                // Continue with normal validation
            }
        }

        // Priority 2: Validate authentication headers
        if (meta.getAuthHeaders() != null) {
            for (String header : meta.getAuthHeaders()) {
                if (request.getHeader(header) == null) {
                    return ValidationResult.error(401,
                            "Authentication required. Missing header: " + header);
                }
            }
        }

        // Priority 3: Validate required headers
        if (meta.getRequiredHeaders() != null) {
            for (String header : meta.getRequiredHeaders()) {
                if (request.getHeader(header) == null) {
                    return ValidationResult.error(400,
                            "Missing required header: " + header);
                }
            }
        }

        // Priority 4: Validate required query parameters
        if (meta.getRequiredParams() != null) {
            for (String param : meta.getRequiredParams()) {
                if (request.getParameter(param) == null) {
                    return ValidationResult.error(400,
                            "Missing required parameter: " + param);
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * Result of validation with status code and message.
     */
    @Getter
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final int statusCode;
        private final String message;

        public static ValidationResult success() {
            return new ValidationResult(true, 0, null);
        }

        public static ValidationResult error(int statusCode, String message) {
            return new ValidationResult(false, statusCode, message);
        }
    }
}
