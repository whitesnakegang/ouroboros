package kr.co.ouroboros.core.global.mock.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
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
                int minItems = ((Number) schema.getOrDefault("minItems", 1)).intValue();
                int maxItems = ((Number) schema.getOrDefault("maxItems", 3)).intValue();

                int size = ThreadLocalRandom.current().nextInt(minItems, maxItems + 1);
                List<Object> arr = new ArrayList<>(size);

                for (int i = 0; i < size; i++) {
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
