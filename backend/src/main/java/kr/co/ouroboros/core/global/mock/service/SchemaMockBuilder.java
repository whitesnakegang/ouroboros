package kr.co.ouroboros.core.global.mock.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchemaMockBuilder {
    private final DummyDataGenerator generator;

    public Object build(Map<String, Object> schema) {
        if (schema == null) return Collections.emptyMap();

        String type = (String) schema.getOrDefault("type", "object");

        switch (type) {
            case "object" -> {
                Map<String, Object> props = (Map<String, Object>) schema.get("properties");
                Map<String, Object> result = new LinkedHashMap<>();
                if (props != null) {
                    for (var entry : props.entrySet()) {
                        result.put(entry.getKey(), build((Map<String, Object>) entry.getValue()));
                    }
                }
                return result;
            }
            case "array" -> {
                Map<String, Object> items = (Map<String, Object>) schema.get("items");
                List<Object> arr = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    arr.add(build(items));
                }
                return arr;
            }
            default -> {
                return generator.generateValue(schema);
            }
        }
    }
}
