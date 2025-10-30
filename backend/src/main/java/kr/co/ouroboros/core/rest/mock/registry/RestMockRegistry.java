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

    private String key(String path, String method) {
        return method.toUpperCase() + ":" + path;
    }

    @Override
    public void register(EndpointMeta meta) {
        registry.put(key(normalizePath(meta.getPath()), meta.getMethod()), meta);
    }

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

    @Override
    public void clear() {
        registry.clear();
        patternCache.clear();
    }

    private String normalizePath(String path) {
        // 끝의 슬래시 제거
        if (path.endsWith("/") && path.length() > 1) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
