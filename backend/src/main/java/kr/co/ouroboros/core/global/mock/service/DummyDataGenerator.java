package kr.co.ouroboros.core.global.mock.service;

import net.datafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DummyDataGenerator {
    private final Faker faker;
    private final FakerExpressionParser parser;

    /**
     * Generate a mock value based on the provided schema map.
     *
     * <p>If {@code schema} is {@code null} this returns {@code null}. If the schema contains
     * an {@code "x-ouroboros-mock"} entry with a Faker DSL expression of the form
     * {@code {{$...}}} the method attempts to parse and return its value; on parse failure
     * it returns a string prefixed with {@code "[FAKER_ERROR] "}. If {@code "x-ouroboros-mock"}
     * exists and is non-blank its string representation is returned. If it exists and is blank
     * an empty string is returned. Otherwise a default value is produced according to the
     * schema's {@code "type"} (defaults to {@code "string"}): integer/number => random int,
     * boolean => random boolean, array => two-word list, object => map with a message, default =>
     * a word.
     *
     * @param schema a map representation of a JSON schema; recognizes the keys {@code "x-ouroboros-mock"}
     *               and {@code "type"} to determine the returned mock value
     * @return {@code null} when {@code schema} is {@code null}; otherwise a mock value which may be
     *         a parsed Faker value, a {@code String}, a {@link Number}, a {@link Boolean},
     *         a {@link java.util.List} of {@link String}, or a {@link java.util.Map} depending on the schema
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

        // faker 형식이 아니고 값이 존재하면 그대로 반환
        if (mockValue != null && !mockValue.toString().isBlank()) {
            return mockValue.toString();
        }

        // 빈 문자열이면 빈 값
        if (mockValue != null && mockValue.toString().isBlank()) {
            return "";
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