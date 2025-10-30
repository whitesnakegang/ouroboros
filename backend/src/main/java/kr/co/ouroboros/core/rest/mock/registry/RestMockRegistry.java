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
