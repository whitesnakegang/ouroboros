package kr.co.ouroboros.core.rest.mock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private final ObjectMapper objectMapper;
    /**
     * Mock 엔드포인트로 들어온 HTTP 요청을 검증하는 서비스
     *
     * 검증 우선순위:
     * 1. X-Ouroboros-Error 헤더 (강제 에러 응답)
     * 2. 인증 헤더 (401)
     * 3. 필수 헤더 (400)
     * 4. 필수 쿼리 파라미터 (400)
     * 5. Request body 존재 여부 (400)
     * 6. Request body JSON 파싱 가능 여부 (400)
     * 7. Request body 필드 타입 검증 (400)
     * 8. Request body 필수 필드 검증 (400)
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
     * Request body를 검증하는 메서드
     * - body 존재 여부
     * - JSON 파싱 가능 여부
     * - 필드 타입 검증
     * - 필수 필드 검증
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
        Map<String, Object> requestBody = (Map<String, Object>) request.getAttribute("parsedRequestBody");

        if (requestBody == null) {
            // InputStream이 비어있거나 파싱 실패
            if (meta.isRequestBodyRequired()) {
                return ValidationResult.error(400, "Invalid JSON format in request body");
            }
            return ValidationResult.success();
        }

        // ===== body가 required인데 비어있으면 에러 =====
        if (meta.isRequestBodyRequired() && requestBody.isEmpty()) {
            return ValidationResult.error(400, "Request body is required but missing");
        }

        // ===== 스키마가 없으면 필드 검증 스킵 =====
        if (meta.getRequestBodySchema() == null) {
            return ValidationResult.success();
        }

        // ===== 우선순위 7 & 8: 필드 타입 & 필수 필드 검증 =====
        return validateSchemaFields(requestBody, meta.getRequestBodySchema(), "");
    }

    /**
     * 스키마에 정의된 필드들을 재귀적으로 검증
     *
     * @param data   실제 요청 데이터 (Map)
     * @param schema 스키마 정의 (type, properties, required 등)
     * @param path   현재 필드 경로 (에러 메시지용, 예: "address.city")
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
     * 단일 필드의 타입을 검증
     *
     * 예: "홍길동"이 string 타입인지, 30이 integer 타입인지 등
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
     * 배열 아이템들을 검증
     *
     * 예: previousAddresses: [{city: "부산"}, {city: "대구"}]
     *     → 각 아이템이 Address 스키마에 맞는지 검증
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
        public static ValidationResult success() {
            return new ValidationResult(true, 0, null);
        }

        public static ValidationResult error(int statusCode, String message) {
            return new ValidationResult(false, statusCode, message);
        }
    }

}
