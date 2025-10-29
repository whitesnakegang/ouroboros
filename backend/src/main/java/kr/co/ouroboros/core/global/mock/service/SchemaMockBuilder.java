package kr.co.ouroboros.core.global.mock.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchemaMockBuilder {
    private final DummyDataGenerator generator;
    private static final Random random = new Random();

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

                int size = random.nextInt(maxItems - minItems + 1) + minItems;
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
