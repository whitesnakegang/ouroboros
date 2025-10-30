package kr.co.ouroboros.core.rest.mock.registry;

import kr.co.ouroboros.core.global.mock.registry.MockRegistryBase;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class RestMockRegistry implements MockRegistryBase<EndpointMeta> {

    private final Map<String, EndpointMeta> registry = new ConcurrentHashMap<>();
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * Constructs the registry key for an endpoint from an HTTP method and path.
     *
     * @param path   the endpoint path (should be normalized, e.g. no trailing slash for non-root)
     * @param method the HTTP method (e.g. "GET", "post"); will be uppercased
     * @return       the composite key in the format METHOD:PATH (method uppercased)
     */
    private String key(String path, String method) {
        return method.toUpperCase() + ":" + path;
    }

    /**
     * Registers the given endpoint metadata in the in-memory registry using its HTTP method and a normalized path.
     *
     * The entry is stored under a composite key of the uppercase HTTP method and the normalized path; any existing
     * entry with the same key is replaced.
     *
     * @param meta the endpoint metadata (provides path and HTTP method) to register
     */
    @Override
    public void register(EndpointMeta meta) {
        registry.put(key(normalizePath(meta.getPath()), meta.getMethod()), meta);
    }

    /**
     * Finds a registered EndpointMeta that matches the given HTTP path and method.
     *
     * @param path   the request path to match; trailing slash is ignored for matching (root "/" is preserved)
     * @param method the HTTP method to match (case-insensitive)
     * @return       an Optional containing the matching EndpointMeta if found, empty otherwise. Exact path matches are preferred; if no exact match exists, template-style paths containing `{param}` are matched where each `{param}` segment matches one path segment.
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
        return registry.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(methodPrefix))
                .filter(entry -> {
                    Pattern pattern = patternCache.computeIfAbsent(entry.getKey(),
                            p -> Pattern.compile(p.replaceAll("\\{[^/]+\\}", "[^/]+")));
                    return pattern.matcher(normalizePath(path)).matches();
                })
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Removes all registered endpoint metadata and clears the compiled pattern cache.
     */
    @Override
    public void clear() {
        registry.clear();
        patternCache.clear();
    }

    /**
     * Normalize a REST path by removing a trailing slash while preserving the root path "/".
     *
     * @param path the input path to normalize
     * @return the normalized path with a trailing slash removed if the path length is greater than one; otherwise the original path
     */
    private String normalizePath(String path) {
        // 끝의 슬래시 제거
        if (path.endsWith("/") && path.length() > 1) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}