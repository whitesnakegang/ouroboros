package kr.co.ouroboros.core.websocket.tryit.identification;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * tryId와 세션 매핑을 관리한다.
 */
@Slf4j
@Component
public class TrySessionRegistry {

    private final Map<UUID, TrySessionRegistration> registrations = new ConcurrentHashMap<>();

    public void register(@NonNull UUID tryId, @NonNull TrySessionRegistration registration) {
        registrations.put(tryId, registration);
        log.trace("Registered tryId {} for session {}", tryId, registration.sessionId());
    }

    public Optional<TrySessionRegistration> find(UUID tryId) {
        return Optional.ofNullable(registrations.get(tryId));
    }

    public Optional<TrySessionRegistration> remove(UUID tryId) {
        TrySessionRegistration removed = registrations.remove(tryId);
        if (removed != null) {
            log.trace("Removed tryId {} for session {}", tryId, removed.sessionId());
        }
        return Optional.ofNullable(removed);
    }

    public void removeBySessionId(@NonNull String sessionId) {
        registrations.entrySet()
                .removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));
        log.trace("Removed try mappings for session {}", sessionId);
    }

    public record TrySessionRegistration(String sessionId, TryDispatchMessage message) {
    }
}

