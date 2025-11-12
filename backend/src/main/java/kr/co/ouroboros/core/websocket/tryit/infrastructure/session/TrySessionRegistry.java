package kr.co.ouroboros.core.websocket.tryit.infrastructure.session;

import kr.co.ouroboros.core.websocket.tryit.infrastructure.messaging.TryDispatchMessage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mappings between tryId and sessionId.
 */
@Slf4j
@Component
public class TrySessionRegistry {

    private final Map<UUID, TrySessionRegistration> registrations = new ConcurrentHashMap<>();

    /**
     * Associates the given tryId with the provided session registration in the registry.
     *
     * If a mapping for the same tryId already exists, it is replaced with the new registration.
     *
     * @param tryId        the identifier for the try operation to register
     * @param registration the session registration (sessionId and associated message) to associate with the tryId
     */
    public void register(@NonNull UUID tryId, @NonNull TrySessionRegistration registration) {
        registrations.put(tryId, registration);
        log.trace("Registered tryId {} for session {}", tryId, registration.sessionId());
    }

    /**
     * Look up the TrySessionRegistration associated with a given tryId.
     *
     * @param tryId the UUID of the try execution to look up
     * @return an Optional containing the registration for the given tryId, or {@link Optional#empty()} if none exists
     */
    public Optional<TrySessionRegistration> find(UUID tryId) {
        return Optional.ofNullable(registrations.get(tryId));
    }

    /**
     * Remove the registration associated with the given try identifier.
     *
     * @param tryId the try identifier whose registration should be removed
     * @return the removed registration wrapped in an Optional if present, otherwise an empty Optional
     */
    public Optional<TrySessionRegistration> remove(UUID tryId) {
        TrySessionRegistration removed = registrations.remove(tryId);
        if (removed != null) {
            log.trace("Removed tryId {} for session {}", tryId, removed.sessionId());
        }
        return Optional.ofNullable(removed);
    }

    /**
     * Remove all mappings whose registration's sessionId matches the given sessionId.
     *
     * @param sessionId the session identifier whose associated try mappings will be removed
     */
    public void removeBySessionId(@NonNull String sessionId) {
        registrations.entrySet()
                .removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));
        log.trace("Removed try mappings for session {}", sessionId);
    }

    public record TrySessionRegistration(String sessionId, TryDispatchMessage message) {
    }
}
