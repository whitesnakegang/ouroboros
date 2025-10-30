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
     * Validate an HTTP request against the endpoint's requirements.
     *
     * Performs prioritized checks in the following order: forced error header, authentication headers,
     * required headers, and required query parameters.
     *
     * @param request the incoming HTTP servlet request
     * @param meta    endpoint metadata describing required authentication headers, headers, and parameters
     * @return a ValidationResult describing success or an error with the HTTP status code and message
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
                            "Authentication required.");
                }
            }
        }

        // Priority 3: Validate required headers
        if (meta.getRequiredHeaders() != null) {
            for (String header : meta.getRequiredHeaders()) {
                if (request.getHeader(header) == null) {
                    return ValidationResult.error(400,
                            "Missing required header");
                }
            }
        }

        // Priority 4: Validate required query parameters
        if (meta.getRequiredParams() != null) {
            for (String param : meta.getRequiredParams()) {
                if (request.getParameter(param) == null) {
                    return ValidationResult.error(400,
                            "Missing required parameter");
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

        /**
         * Create a ValidationResult representing a successful validation.
         *
         * @return a ValidationResult with valid set to true, statusCode set to 0, and message set to null.
         */
        public static ValidationResult success() {
            return new ValidationResult(true, 0, null);
        }

        /**
         * Create a ValidationResult representing a failed validation with the given HTTP status and message.
         *
         * @param statusCode the HTTP status code describing the failure
         * @param message    a human-readable message explaining the failure
         * @return           a ValidationResult with valid=false, the provided statusCode, and message
         */
        public static ValidationResult error(int statusCode, String message) {
            return new ValidationResult(false, statusCode, message);
        }
    }
}