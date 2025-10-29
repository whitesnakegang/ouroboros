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
