package kr.co.ouroboros.core.rest.mock.registry;

import kr.co.ouroboros.core.global.mock.registry.MockRegistryBase;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Thread-safe registry for REST mock endpoints.
 * <p>
 * Supports two matching strategies:
 * <ol>
 *   <li>Exact path matching: {@code /users} matches {@code /users} only</li>
 *   <li>Pattern matching: {@code /users/{id}} matches {@code /users/123}</li>
 * </ol>
 * <p>
 * Pattern matching improvements:
 * <ul>
 *   <li>Escapes regex special characters (., ?, *, etc.) to prevent unintended matches</li>
 *   <li>Anchors patterns with {@code ^...$} to ensure full path matching</li>
 *   <li>Caches compiled patterns per endpoint for performance</li>
 * </ul>
 *
 * @since 0.0.1
 */
@Component
public class RestMockRegistry implements MockRegistryBase<EndpointMeta> {

    private final Map<String, EndpointMeta> registry = new ConcurrentHashMap<>();
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    private String key(String path, String method) {
        return method.toUpperCase() + ":" + path;
    }

    @Override
    public void register(EndpointMeta meta) {
        registry.put(key(normalizePath(meta.getPath()), meta.getMethod()), meta);
    }

    /**
     * Finds a mock endpoint by path and HTTP method.
     * <p>
     * Matching order:
     * <ol>
     *   <li>Exact match: Returns immediately if exact path + method found</li>
     *   <li>Pattern match: Tries path parameter patterns like {@code /users/{id}}</li>
     * </ol>
     * <p>
     * Pattern generation example:
     * <pre>
     * Registered: /files/{name}.json
     * Regex: ^/files/[^/]+\.json$
     * Matches: /files/report.json ✓
     * Rejects: /files/reportjson ✗ (. is properly escaped)
     * </pre>
     *
     * @param path the request path
     * @param method the HTTP method
     * @return Optional containing matched endpoint, or empty if not found
     */
    @Override
    public Optional<EndpointMeta> find(String path, String method) {
        // 정확한 매칭 우선
        String exactKey = key(normalizePath(path), method); // "/users/" → "/users"
        EndpointMeta exactMatch = registry.get(exactKey);
        if (exactMatch != null) {
            return Optional.of(exactMatch);
        }

        // Path parameter 패턴 매칭
        String methodPrefix = method.toUpperCase() + ":";
        String normalizedPath = normalizePath(path);

        return registry.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(methodPrefix))
                .filter(entry -> {
                    String cacheKey = entry.getKey();  // "GET:/users/{id}" - 캐시 키 (메서드 포함)
                    String registeredPath = cacheKey.substring(methodPrefix.length()); // "/users/{id}"

                    // 캐시 키는 메서드 포함, 패턴은 path만 사용
                    Pattern pattern = patternCache.computeIfAbsent(cacheKey, k -> {
                        String regex = registeredPath
                                .replaceAll("([.()\\[\\]\\-+*?^$|\\\\])", "\\\\$1")  // 특수문자 이스케이프
                                .replaceAll("\\{[^/]+\\}", "[^/]+");  // 경로 파라미터 처리
                        return Pattern.compile("^" + regex + "$");  // 전체 매칭 고정
                    });

                    return pattern.matcher(normalizedPath).matches();
                })
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Reset the registry state by removing all registered endpoints and cached patterns.
     *
     * After invocation the registry contains no endpoints and the pattern cache is empty.
     */
    @Override
    public void clear() {
        registry.clear();
        patternCache.clear();
    }

    /**
     * Normalize a request path by removing a trailing '/' except when the path is a single "/".
     *
     * @param path the original request path
     * @return the normalized path with a trailing slash removed if the path length is greater than 1
     */
    private String normalizePath(String path) {
        // 끝의 슬래시 제거
        if (path.endsWith("/") && path.length() > 1) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}