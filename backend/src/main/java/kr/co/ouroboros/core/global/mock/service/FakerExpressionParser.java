package kr.co.ouroboros.core.global.mock.service;

import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

@Component
@RequiredArgsConstructor
public class FakerExpressionParser {
    private static final Pattern FAKER_PATTERN =
            Pattern.compile("\\{\\{\\$(.*?)\\}\\}");
    private static final Pattern PARAM_PATTERN =
            Pattern.compile("(\\w+)=(\\d+|'.*?'|\".*?\")");

    private final Faker faker;

    /**
     * Parses a DSL expression of the form {{$(category.method(...))}} and evaluates it against the injected Faker instance.
     *
     * The expression must match the pattern {{$(<category>.<method>[(<key>=<value>[,...])])}}. Supported special mappings:
     *   - `int` is treated as `numberBetween` (expects `min` and `max`).
     *   - `decimal` is treated as `randomDouble` (expects `min` and `max`).
     * For `numberBetween`, `min` defaults to 1 and `max` defaults to 100. For `randomDouble`, `min` defaults to 1000 and `max` defaults to 100000.
     *
     * @param expression the DSL expression to parse and evaluate; may be null or blank
     * @return the value produced by invoking the specified Faker category method, or `null` if the input is invalid or evaluation fails
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

    /**
     * Parse a raw parameter substring into a map of parameter names to their values with surrounding quotes removed.
     *
     * @param raw the raw parameters string (e.g., "min=1,max='10'") to parse for key=value pairs
     * @return a map where each key is a parameter name and each value is the parsed parameter value as a String (quotes stripped)
     */
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