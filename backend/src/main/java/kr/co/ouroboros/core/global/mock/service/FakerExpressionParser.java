package kr.co.ouroboros.core.global.mock.service;

import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

/**
 * Parser for custom Faker DSL expressions in OpenAPI schemas.
 * <p>
 * Supports expression format: {@code {{$category.method(param=value)}}}<br>
 * Uses Java reflection to dynamically invoke DataFaker methods at runtime.
 * <p>
 * Supported expression examples:
 * <ul>
 *   <li>{@code {{$name.firstName}}} - Korean name (configured locale)</li>
 *   <li>{@code {{$number.int(min=1,max=100)}}} - Integer between 1-100</li>
 *   <li>{@code {{$number.decimal(min=1000,max=100000)}}} - Decimal with 2 decimal places</li>
 *   <li>{@code {{$internet.emailAddress}}} - Random email</li>
 *   <li>{@code {{$address.city}}} - Random city name</li>
 * </ul>
 * <p>
 * Method name aliasing:
 * <ul>
 *   <li>{@code int} → {@code numberBetween(min, max)}</li>
 *   <li>{@code decimal} → {@code randomDouble(scale=2, min, max)}</li>
 * </ul>
 *
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
public class FakerExpressionParser {
    private static final Pattern FAKER_PATTERN =
            Pattern.compile("\\{\\{\\$(.*?)\\}\\}");
    private static final Pattern PARAM_PATTERN =
            Pattern.compile("(\\w+)=(\\d+|'.*?'|\".*?\")");

    private final Faker faker;

    /**
     * Parses and executes a Faker DSL expression.
     * <p>
     * Parsing steps:
     * <ol>
     *   <li>Validate expression format with regex pattern</li>
     *   <li>Extract category (e.g., "name", "number")</li>
     *   <li>Extract method name (e.g., "firstName", "int")</li>
     *   <li>Parse parameters (e.g., "min=1,max=100")</li>
     *   <li>Invoke Faker method via reflection</li>
     * </ol>
     *
     * @param expression the Faker DSL expression (e.g., "{{$name.firstName}}")
     * @return generated value from DataFaker, or null if parsing fails
     */
    public Object parse(String expression) {
        if (expression == null || expression.isBlank()) return null;
        Matcher matcher = FAKER_PATTERN.matcher(expression.trim());
        if (!matcher.matches()) return null; // faker DSL 형식이 아님

        String inner = matcher.group(1).trim(); // number.int(min=1,max=100)
        String[] parts = inner.split("\\(", 2);
        String path = parts[0]; // number.int
        String[] pathParts = path.split("\\.");
        if (pathParts.length < 2) return null;

        String category = pathParts[0];
        String method = pathParts[1];
        Map<String, Object> params = parseParams(parts.length > 1 ? parts[1] : "");

        try {
            Method catMethod = Faker.class.getMethod(category);
            Object catObj = catMethod.invoke(faker);

            if (method.equals("int")) method = "numberBetween";
            if (method.equals("decimal")) method = "randomDouble";

            if (method.equals("numberBetween")) {
                int min = Integer.parseInt(params.getOrDefault("min", 1).toString());
                int max = Integer.parseInt(params.getOrDefault("max", 100).toString());
                return catObj.getClass().getMethod(method, int.class, int.class)
                        .invoke(catObj, min, max);
            }

            if (method.equals("randomDouble")) {
                long min = Long.parseLong(params.getOrDefault("min", "1000").toString());
                long max = Long.parseLong(params.getOrDefault("max", "100000").toString());
                return catObj.getClass().getMethod(method, int.class, long.class, long.class)
                        .invoke(catObj, 2, min, max);
            }

            Method m2 = catObj.getClass().getMethod(method);
            return m2.invoke(catObj);

        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseParams(String raw) {
        Map<String, Object> map = new HashMap<>();
        Matcher m = PARAM_PATTERN.matcher(raw);
        while (m.find()) {
            String key = m.group(1);
            String value = m.group(2).replaceAll("['\"]", "");
            map.put(key, value);
        }
        return map;
    }
}
