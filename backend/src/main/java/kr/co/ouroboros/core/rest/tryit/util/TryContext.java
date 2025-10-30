package kr.co.ouroboros.core.rest.tryit.util;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Utility for managing Try context using OpenTelemetry Baggage.
 * 
 * OpenTelemetry Baggage Integration:
 * - TryFilter sets the tryId in OpenTelemetry Baggage
 * - Baggage provides automatic context propagation across threads and async boundaries
 * - OpenTelemetry Sampler/Instrumentation can read it via Baggage
 * - Cleaned up after request processing
 * 
 * Baggage is used instead of ThreadLocal because:
 * - Works across thread boundaries (async operations, CompletableFuture, etc.)
 * - Standard OpenTelemetry mechanism for context propagation
 * - Automatically propagated through OpenTelemetry Context
 */
@Slf4j
public class TryContext {
    
    private static final String BAGGAGE_KEY = "ouro.try_id";
    
    /**
     * Sets the current tryId in OpenTelemetry Baggage.
     * If null is provided, clears the current tryId.
     * 
     * @param tryId the try session ID
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
     * 
     * @return the try session ID, or null if not set
     */
    public static UUID getTryId() {
        String id = getTryIdFromBaggage();
        if (id != null) {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tryId format in baggage: {}", id);
                return null;
            }
        }
        return null;
    }
    
    /**
     * Checks if there is a tryId in the current context.
     * 
     * @return true if tryId is set, false otherwise
     */
    public static boolean hasTryId() {
        return getTryIdFromBaggage() != null;
    }
    
    /**
     * Clears the current tryId from OpenTelemetry Baggage.
     * Should be called after request processing to prevent memory leaks.
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
    
    /**
     * Gets the tryId from OpenTelemetry Baggage.
     * Useful for Conditional Sampler to check if this is a Try request.
     * 
     * @return the try session ID from Baggage, or null if not set
     */
    public static String getTryIdFromBaggage() {
        try {
            return Baggage.current().getEntryValue(BAGGAGE_KEY);
        } catch (Exception e) {
            return null;
        }
    }
}
