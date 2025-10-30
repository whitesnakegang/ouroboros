package kr.co.ouroboros.core.global.mock.registry;

import java.util.Optional;

public interface MockRegistryBase<T> {
    void register(T meta);
    Optional<T> find(String key1, String key2);
    void clear();
}
