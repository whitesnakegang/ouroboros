package kr.co.ouroboros.core.global.mock.registry;

import java.util.Optional;

public interface MockRegistryBase<T> {
    /**
 * Registers the given meta object in the registry for later retrieval.
 *
 * @param meta the meta object to store in the registry
 */
void register(T meta);
    /**
 * Locate a registered meta object by a pair of lookup keys.
 *
 * @param key1 the primary lookup key used to identify the meta
 * @param key2 the secondary lookup key used to identify the meta
 * @return an {@code Optional} containing the meta that matches both keys, or {@code Optional.empty()} if none is found
 */
Optional<T> find(String key1, String key2);
    /**
 * Removes all stored meta objects from the registry.
 *
 * After this call the registry contains no entries and subsequent lookups will not find previously registered items.
 */
void clear();
}