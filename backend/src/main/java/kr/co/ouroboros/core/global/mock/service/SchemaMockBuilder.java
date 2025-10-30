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
                Object propsObj = schema.get("properties");
                if (!(propsObj instanceof Map)) {
                    return Collections.emptyMap();
                }
                Map<String, Object> props = (Map<String, Object>) propsObj;
                Map<String, Object> result = new LinkedHashMap<>();
                for (var entry : props.entrySet()) {
                    Object propValue = entry.getValue();
                    if (propValue instanceof Map) {
                        result.put(entry.getKey(), build((Map<String, Object>) propValue));
                    }
                }
                return result;
            }
            case "array" -> {
                Object itemsObj = schema.get("items");
                if (!(itemsObj instanceof Map)) {
                    return Collections.emptyList();
                }
                Map<String, Object> items = (Map<String, Object>) itemsObj;

                int minItems = 1;
                int maxItems = 3;
                Object minObj = schema.get("minItems");
                Object maxObj = schema.get("maxItems");
                if (minObj instanceof Number) {
                    minItems = ((Number) minObj).intValue();
                }
                if (maxObj instanceof Number) {
                    maxItems = ((Number) maxObj).intValue();
                }

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
