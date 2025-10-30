package kr.co.ouroboros.core.global.mock.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchemaMockBuilder {
    private final DummyDataGenerator generator;

    /**
     * Builds mock data that conforms to the provided JSON-like schema.
     *
     * <p>The schema is a map that may include a "type" key to control construction:
     * - "object": reads a "properties" map and produces a LinkedHashMap with each property's mock value;
     * - "array": reads an "items" schema and optional numeric "minItems"/"maxItems" to produce a List of mock items;
     * - any other type: delegates value creation to the injected generator.</p>
     *
     * @param schema a JSON-like schema map describing the desired mock structure; may be null.
     *               Expected keys include "type" (defaults to "object"), "properties" (map) for objects,
     *               "items" (map) and numeric "minItems"/"maxItems" for arrays. If null, an empty map is returned.
     * @return an object built to match the schema: a Map for "object" types, a List for "array" types,
     *         or a generated primitive/boxed value for other types.
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