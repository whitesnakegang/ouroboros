package kr.co.ouroboros.core.global.mock.registry;

import java.util.Optional;

/**
 * Generic registry interface for mock endpoint metadata.
 * <p>
 * Provides protocol-agnostic operations for registering, finding, and clearing mock endpoints.
 * Implementations should be thread-safe to support concurrent requests.
 * <p>
 * Example implementations:
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry} - REST endpoints with path+method keys</li>
 *   <li>Future: gRPC registry with service+method keys</li>
 * </ul>
 *
 * @param <T> the type of mock metadata (e.g., EndpointMeta for REST)
 * @since 0.0.1
 */
public interface MockRegistryBase<T> {
    /**
     * Registers a mock endpoint in the registry.
     *
     * @param meta the endpoint metadata to register
     */
    void register(T meta);

    /**
     * Finds a mock endpoint by two identifying keys.
     * <p>
     * For REST: key1=path, key2=method (e.g., "/users", "GET")
     *
     * @param key1 the first identifying key
     * @param key2 the second identifying key
     * @return Optional containing the metadata if found, empty otherwise
     */
    Optional<T> find(String key1, String key2);

    void clear();
}
