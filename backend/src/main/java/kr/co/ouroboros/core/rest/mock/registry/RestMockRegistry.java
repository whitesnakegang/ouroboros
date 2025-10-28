package kr.co.ouroboros.core.rest.mock.registry;

import kr.co.ouroboros.core.global.mock.registry.MockRegistryBase;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RestMockRegistry implements MockRegistryBase<EndpointMeta> {

    private final Map<String, EndpointMeta> registry = new ConcurrentHashMap<>();

    private String key(String path, String method) {
        return method.toUpperCase() + ":" + path;
    }

    @Override
    public void register(EndpointMeta meta) {
        registry.put(meta.getId(), meta);
    }

    @Override
    public Optional<EndpointMeta> find(String path, String method) {
        return Optional.ofNullable(registry.get(key(path, method)));
    }

    @Override
    public void clear() {
        registry.clear();
    }
}
