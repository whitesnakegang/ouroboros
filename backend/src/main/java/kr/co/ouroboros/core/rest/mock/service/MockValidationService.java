package kr.co.ouroboros.core.rest.mock.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for validating incoming HTTP requests
 * against mock endpoint requirements defined in {@link EndpointMeta}.
 *
 * <p>This validation ensures that requests meet header,
 * authentication, and query parameter conditions before a
 * mock response is generated.</p>
 *
 * <h2>Validation Priority</h2>
 * <ol>
 *     <li><b>X-Ouroboros-Error</b> header — Forces an error response if present.</li>
 *     <li><b>Authentication headers</b> — 401 Unauthorized if required headers are missing.</li>
 *     <li><b>Required headers</b> — 400 Bad Request if missing.</li>
 *     <li><b>Required query parameters</b> — 400 Bad Request if missing.</li>
 * </ol>
 *
 * <p>If all validations pass, the request is considered valid and
 * mock generation continues normally.</p>
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockValidationService {
    /**
     * Validate an incoming HttpServletRequest against the endpoint requirements defined in EndpointMeta for a mock endpoint.
     *
     * Performs prioritized checks and returns the first failing ValidationResult:
     * - forced error via X-Ouroboros-Error header (if parseable as an integer),
     * - required authentication headers (401),
     * - required headers (400),
     * - required query parameters (400),
     * - request body presence, JSON parsability, field types, and required fields per the request body schema (400).
     *
     * @param request the incoming HTTP servlet request to validate
     * @param meta    the endpoint metadata describing required headers, params, and request body schema/requirements
     * @return        a ValidationResult representing success or the first encountered error with an HTTP status code and message
     */
    public ValidationResult validate(HttpServletRequest request, EndpointMeta meta) {
        // ===== 우선순위 1: 강제 에러 헤더 체크 =====
        String forcedError = request.getHeader("x-ouroboros-error");
        if (forcedError != null) {
            try {
                int errorCode = Integer.parseInt(forcedError);
                return ValidationResult.error(errorCode,
                        "Forced error response via X-Ouroboros-Error header");
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Ouroboros-Error header value: {}", forcedError);
            }
        }

        // ===== 우선순위 2: 인증 헤더 검증 =====
        if (meta.getAuthHeaders() != null) {
            for (String header : meta.getAuthHeaders()) {
                if (request.getHeader(header) == null) {
                    return ValidationResult.error(401,
                            "Authentication required.");
                }
            }
        }

        // ===== 우선순위 3: 필수 헤더 검증 =====
        // 예: X-Request-ID, X-Tenant-ID 등
        if (meta.getRequiredHeaders() != null) {
            for (String header : meta.getRequiredHeaders()) {
                if (request.getHeader(header) == null) {
                    return ValidationResult.error(400,
                            "Missing required header");
                }
            }
        }

        // ===== 우선순위 4: 필수 쿼리 파라미터 검증 =====
        // 예: ?userId=xxx&role=yyy
        if (meta.getRequiredParams() != null) {
            for (String param : meta.getRequiredParams()) {
                if (request.getParameter(param) == null) {
                    return ValidationResult.error(400,
                            "Missing required parameter");
                }
            }
        }

        // ===== 우선순위 5-8: Request body 검증 =====
        // requestBodyRequired=true 이거나 스키마가 정의되어 있으면 검증
        if (meta.isRequestBodyRequired() || meta.getRequestBodySchema() != null) {
            ValidationResult bodyValidation = validateRequestBody(request, meta);
            if (!bodyValidation.valid()) {
                return bodyValidation;
            }
        }

        return ValidationResult.success();
    }


    /**
     * Validate the HTTP request body against the endpoint's schema and requirement flags.
     *
     * Performs presence checks, JSON structure checks, required-field checks, and type checks
     * according to the EndpointMeta.requestBodySchema and EndpointMeta.requestBodyRequired.
     *
     * @param request the incoming HttpServletRequest; the method reads the pre-parsed body from the `parsedRequestBody` attribute
     * @param meta    endpoint metadata containing requestBodySchema and requestBodyRequired
     * @return        a ValidationResult indicating success when the body satisfies requirements; otherwise an error result with an HTTP status code and message
     */
    @SuppressWarnings("unchecked")
    private ValidationResult validateRequestBody(HttpServletRequest request, EndpointMeta meta) {
        // ===== GET, DELETE 등은 body 없음 =====
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)) {
            return ValidationResult.success();
        }

        // ===== Attribute에서 미리 파싱된 body 가져오기 =====
        Object requestBody = request.getAttribute("parsedRequestBody");

        // ===== multipart는 Content-Type만 확인하고 검증 스킵 =====
        if (requestBody instanceof Map) {
            Map<String, Object> bodyMap = (Map<String, Object>) requestBody;
            if (Boolean.TRUE.equals(bodyMap.get("_multipart"))) {
                log.debug("Multipart request validation skipped (mock mode)");
                return ValidationResult.success();  // 검증 통과
            }
        }

        if (requestBody == null) {
            // InputStream이 비어있거나 파싱 실패
            if (meta.isRequestBodyRequired()) {
                return ValidationResult.error(400, "Invalid JSON format in request body");
            }
            return ValidationResult.success();
        }

        // ===== 스키마가 없으면 필드 검증 스킵 =====
        if (meta.getRequestBodySchema() == null) {
            return ValidationResult.success();
        }

        // ==== 스키마의 루트 타입 확인 ====
        Map<String, Object> schema = meta.getRequestBodySchema();
        String rootType = (String) schema.get("type");

        if (rootType == null) {
            return ValidationResult.success();
        }

        // 루트 타입에 따라 분기 처리
        if ("object".equals(rootType)) {
            if (!(requestBody instanceof Map<?, ?> bodyMap)) {
                return ValidationResult.error(400, "Request body must be a JSON object");
            }
            return validateSchemaFields((Map<String, Object>) bodyMap, schema, "");
        }

        if ("array".equals(rootType)) {
            if (!(requestBody instanceof List<?> bodyList)) {
                return ValidationResult.error(400, "Request body must be a JSON array");
            }
            Map<String, Object> itemSchema = (Map<String, Object>) schema.get("items");
            if (itemSchema == null) {
                return ValidationResult.success();
            }
            return validateArrayItems((List<Object>) bodyList, itemSchema, "");
        }

        // 프리미티브 타입 (string, number, boolean 등)
        ValidationResult rootTypeValidation = validateFieldType(requestBody, rootType, "requestBody");
        if (!rootTypeValidation.valid()) {
            return rootTypeValidation;
        }

        return ValidationResult.success();
    }

    /**
     * Recursively validates object fields against the provided JSON-like schema.
     *
     * @param data   the actual request data as a map of field names to values
     * @param schema the schema definition (may include keys like "type", "properties", "required", "items")
     * @param path   current field path used in error messages (e.g., "address.city"); may be empty
     * @return       a ValidationResult that is valid on success; when invalid it contains the HTTP status code and an explanatory message for the first detected violation
     */
    @SuppressWarnings("unchecked")
    private ValidationResult validateSchemaFields(Map<String, Object> data,
                                                  Map<String, Object> schema,
                                                  String path) {
        String type = (String) schema.get("type");

        // object 타입만 검증
        if (!"object".equals(type)) {
            return ValidationResult.success();
        }

        // ===== 우선순위 8: 필수 필드 검증 =====
        // schema.required = ["id", "name"] 같은 배열
        List<String> required = (List<String>) schema.get("required");
        if (required != null) {
            for (String field : required) {
                if (!data.containsKey(field)) {
                    // 필수 필드가 없으면 에러
                    return ValidationResult.error(400,
                            "Missing required field: " + (path.isEmpty() ? field : path + "." + field));
                }
            }
        }

        // ===== 우선순위 7: 필드 타입 검증 =====
        // schema.properties = {id: {type: "string"}, name: {type: "string"}, ...}
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties == null) {
            return ValidationResult.success();
        }

        // 요청 데이터의 각 필드를 검증
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();  // 예: "name"
            Object fieldValue = entry.getValue();  // 예: "홍길동"
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;

            // 스키마에서 해당 필드의 정의 가져오기
            Map<String, Object> propSchema = (Map<String, Object>) properties.get(fieldName);
            if (propSchema == null) {
                // 스키마에 없는 필드
                continue;
            }

            String expectedType = (String) propSchema.get("type");
            if (expectedType == null) {
                // 타입 정의 없으면 검증 스킵
                continue;
            }

            // ===== 타입 검증 =====
            ValidationResult typeValidation = validateFieldType(fieldValue, expectedType, fieldPath);
            if (!typeValidation.valid()) {
                return typeValidation;
            }

            // ===== 중첩 객체 재귀 검증 =====
            if ("object".equals(expectedType) && fieldValue instanceof Map) {
                ValidationResult nestedValidation = validateSchemaFields(
                        (Map<String, Object>) fieldValue, propSchema, fieldPath);
                if (!nestedValidation.valid()) {
                    return nestedValidation;
                }
            }

            // ===== 배열 아이템 검증 =====
            if ("array".equals(expectedType) && fieldValue instanceof List) {
                Map<String, Object> items = (Map<String, Object>) propSchema.get("items");
                if (items != null) {
                    ValidationResult arrayValidation = validateArrayItems(
                            (List<Object>) fieldValue, items, fieldPath);
                    if (!arrayValidation.valid()) {
                        return arrayValidation;
                    }
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * Validate the runtime type of a single field against the schema's expected type.
     *
     * @param value the actual field value to check; null values are treated as valid here
     * @param expectedType the schema type name to validate against (e.g. "string", "integer", "number", "boolean", "array", "object")
     * @param fieldPath the dot-separated field path used in error messages (e.g. "user.address.street")
     * @return a ValidationResult indicating success when the value matches the expected type (or is null), or an error with status 400 and a message describing the type mismatch
     */
    private ValidationResult validateFieldType(Object value, String expectedType, String fieldPath) {
        if (value == null) {
            // null은 허용 (required 검증은 별도로 함)
            return ValidationResult.success();
        }

        // 타입별 Java 인스턴스 체크
        boolean valid = switch (expectedType) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true; // Unknown type - allow
        };

        if (!valid) {
            // 타입 불일치 에러
            return ValidationResult.error(400,
                    String.format("Field '%s' has invalid type. Expected: %s, Got: %s",
                            fieldPath, expectedType, value.getClass().getSimpleName()));
        }

        return ValidationResult.success();
    }

    /**
         * Validate each element of an array against the provided item schema and return the first validation error encountered.
         *
         * Checks each item's declared type and, if the item type is "object", recursively validates the item's fields using the item schema.
         *
         * @param array      the list of items to validate (parsed from the request body)
         * @param itemSchema a schema map describing the expected type and structure of each array item (expects a "type" entry; may include object properties)
         * @param fieldPath  the JSON path to this array used in error messages (e.g., "addresses" or "addresses[0]")
         * @return           a ValidationResult indicating success when all items are valid, or the first error result encountered for an invalid item
         */
    @SuppressWarnings("unchecked")
    private ValidationResult validateArrayItems(List<Object> array,
                                                Map<String, Object> itemSchema,
                                                String fieldPath) {
        String itemType = (String) itemSchema.get("type");
        if (itemType == null) {
            return ValidationResult.success();
        }

        // 배열의 각 아이템 검증
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            String itemPath = fieldPath + "[" + i + "]";

            // 아이템 타입 검증
            ValidationResult typeValidation = validateFieldType(item, itemType, itemPath);
            if (!typeValidation.valid()) {
                return typeValidation;
            }

            // 배열 안의 객체를 재귀 검증
            if ("object".equals(itemType) && item instanceof Map) {
                ValidationResult nestedValidation = validateSchemaFields(
                        (Map<String, Object>) item, itemSchema, itemPath);
                if (!nestedValidation.valid()) {
                    return nestedValidation;
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * 검증 결과를 담는 클래스
     */
    public record ValidationResult(
            boolean valid,
            int statusCode,
            String message
    ) {
        /**
         * Create a ValidationResult representing a successful validation.
         *
         * @return the successful ValidationResult with statusCode 0 and no message
         */
        public static ValidationResult success() {
            return new ValidationResult(true, 0, null);
        }

        /**
         * Create a ValidationResult representing a failed validation with the given HTTP status code and message.
         *
         * @param statusCode the HTTP status code to associate with the failure
         * @param message    a human-readable error message describing the failure
         * @return           a ValidationResult with valid set to false, the provided statusCode, and the provided message
         */
        public static ValidationResult error(int statusCode, String message) {
            return new ValidationResult(false, statusCode, message);
        }
    }

}