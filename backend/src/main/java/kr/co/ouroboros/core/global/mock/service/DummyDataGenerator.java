package kr.co.ouroboros.core.global.mock.service;

import net.datafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service for generating mock data values based on OpenAPI schema definitions.
 * <p>
 * Supports three mock data generation strategies in order of priority:
 * <ol>
 *   <li>Faker DSL expressions: {@code {{$category.method(params)}}}</li>
 *   <li>Custom x-ouroboros-mock values: Preserves original data types (String, Integer, Boolean, etc.)</li>
 *   <li>Type-based random generation: Falls back to DataFaker for primitive types</li>
 * </ol>
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class DummyDataGenerator {
    private final Faker faker;
    private final FakerExpressionParser parser;

    /**
     * Generates a mock value for a given schema field.
     * <p>
     * Type preservation: Non-string values in {@code x-ouroboros-mock} are returned with their original type.
     * For example, {@code x-ouroboros-mock: 123} returns Integer 123, not String "123".
     *
     * @param schema the JSON schema map containing type and mock definitions
     * @return generated mock value with preserved type, or null if schema is null
     */
    public Object generateValue(Map<String, Object> schema) {
        if (schema == null) return null;

        Object mockValue = schema.get("x-ouroboros-mock");
        String type = (String) schema.getOrDefault("type", "string");

        // faker DSL
        if (mockValue instanceof String str && str.startsWith("{{$") && str.endsWith("}}")) {
            Object parsed = parser.parse(str);
            if (parsed != null) {
                return parsed;
            }

            // 파싱 실패 시 로그 및 에러 메시지 반환
            return "[FAKER_ERROR] " + str;
        }

        // faker 형식이 아니고 값이 존재하면 원본 타입 그대로 반환
        if (mockValue instanceof String str) {
            if (!str.isBlank()) {
                return str;
            }
            // 빈 문자열이면 빈 값
            return "";
        }

        if (mockValue != null) {
            return mockValue;
        }

        // type 기반 기본 랜덤값
        return switch (type) {
            case "integer", "number" -> faker.number().numberBetween(1, 1000);
            case "boolean" -> faker.bool().bool();
            case "array" -> List.of(faker.lorem().word(), faker.lorem().word());
            case "object" -> Map.of("message", faker.lorem().sentence());
            default -> faker.lorem().word();
        };
    }
}
