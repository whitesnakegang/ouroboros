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
     * Sets the current tryId in OpenTelemetry Baggage.
     * <p>
     * If null is provided, clears the current tryId.
     * The tryId is stored in OpenTelemetry Baggage and automatically
     * propagated across thread boundaries and async operations.
     *
     * @param tryId the try session ID (UUID), or null to clear
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
     * Gets the current tryId from OpenTelemetry Baggage.
     * <p>
     * Retrieves the tryId from the current OpenTelemetry Baggage context.
     * Returns null if not set or if OpenTelemetry Baggage is not available.
     *
     * @return the try session ID (UUID), or null if not set or unavailable
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
     * Clears the current tryId from OpenTelemetry Baggage.
     * <p>
     * Removes the tryId from OpenTelemetry Baggage context.
     * Should be called after request processing to prevent memory leaks
     * and ensure clean context state.
     */
    public static void clear() {
        try {
            Baggage currentBaggage = Baggage.current();
            String existingId = currentBaggage.getEntryValue(BAGGAGE_KEY);
            
            if (existingId != null) {
                BaggageBuilder builder = currentBaggage.toBuilder();
                builder.remove(BAGGAGE_KEY);
                Baggage updatedBaggage = builder.build();
                updatedBaggage.makeCurrent();
                log.debug("Cleared tryId from baggage: {}", existingId);
            }
        } catch (Exception e) {
            log.trace("OpenTelemetry Baggage not available: {}", e.getMessage());
        }
    }
}

