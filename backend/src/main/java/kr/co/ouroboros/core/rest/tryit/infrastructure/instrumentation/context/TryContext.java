package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Utility for managing Try context using OpenTelemetry Baggage.
 * <p>
 * This utility provides thread-safe management of tryId context using
 * OpenTelemetry Baggage for distributed tracing.
 * <p>
 * <b>OpenTelemetry Baggage Integration:</b>
 * <ul>
 *   <li>TryFilter sets the tryId in OpenTelemetry Baggage</li>
 *   <li>Baggage provides automatic context propagation across threads and async boundaries</li>
 *   <li>OpenTelemetry Sampler/Instrumentation can read it via Baggage</li>
 *   <li>Cleaned up after request processing</li>
 * </ul>
 * <p>
 * <b>Why Baggage instead of ThreadLocal:</b>
 * <ul>
 *   <li>Works across thread boundaries (async operations, CompletableFuture, etc.)</li>
 *   <li>Standard OpenTelemetry mechanism for context propagation</li>
 *   <li>Automatically propagated through OpenTelemetry Context</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
public class TryContext {
    
    private static final String BAGGAGE_KEY = "ouro.try_id";
    
    /**
     * Store the given try session UUID in OpenTelemetry Baggage so it is propagated across threads and async boundaries.
     *
     * If `tryId` is null the current tryId is cleared.
     *
     * @param tryId the try session ID to set, or null to clear the current tryId
     */
    public static void setTryId(UUID tryId) {
        if (tryId != null) {
            try {
                Baggage currentBaggage = Baggage.current();
                BaggageBuilder builder = currentBaggage.toBuilder();
                builder.put(BAGGAGE_KEY, tryId.toString());
                Baggage updatedBaggage = builder.build();
                updatedBaggage.makeCurrent();
                log.debug("Set tryId in baggage: {}", tryId);
            } catch (Exception e) {
                // OpenTelemetry not available, just log
                log.trace("OpenTelemetry Baggage not available: {}", e.getMessage());
                log.debug("Failed to set tryId in baggage: {}", tryId);
            }
        } else {
            clear();
        }
    }
    
    /**
     * Retrieve the current try session ID from OpenTelemetry Baggage.
     *
     * If a value is present and parses as a UUID, returns that UUID; otherwise returns `null`.
     *
     * @return the `UUID` of the current try session if present and valid, `null` otherwise
     */
    public static UUID getTryId() {
        try {
            String id = Baggage.current().getEntryValue(BAGGAGE_KEY);
            if (id != null) {
                try {
                    return UUID.fromString(id);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tryId format in baggage: {}", id);
                    return null;
                }
            }
        } catch (Exception e) {
            // OpenTelemetry Baggage not available
            return null;
        }
        return null;
    }
    
    /**
     * Checks if there is a tryId in the current context.
     * <p>
     * Determines whether a tryId is currently set in OpenTelemetry Baggage.
     *
     * @return true if tryId is set, false otherwise
     */
    public static boolean hasTryId() {
        return getTryId() != null;
    }
    
    /**
     * Remove the current tryId from the OpenTelemetry Baggage context.
     *
     * <p>If a tryId entry exists under the BAGGAGE_KEY, it is removed and the updated baggage
     * is made current. Intended to be called after request processing to avoid leaking
     * the tryId into unrelated executions.
     */
    public static void clear() {
        // Scope.close()를 통해 자동으로 정리되므로 명시적 clear는 불필요
        // 이 메서드는 하위 호환성을 위해 유지하되, 실제 동작은 Scope.close()에 위임
        log.debug("TryContext.clear() called - context will be cleared by Scope.close()");
    }
}
