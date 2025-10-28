package kr.co.ouroboros.core.rest.tryit.util;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Thread-local utility for managing Try context.
 * Stores the current tryId in ThreadLocal and propagates it via OpenTelemetry Baggage.
 * 
 * OpenTelemetry Baggage Integration:
 * - TryFilter sets the tryId in ThreadLocal
 * - Propagates tryId to OpenTelemetry Baggage for distributed tracing
 * - OpenTelemetry Sampler/Instrumentation can read it via Baggage
 * - Cleaned up after request processing
 */
@Slf4j
public class TryContext {
    
    private static final String BAGGAGE_KEY = "ouro.try_id";
    private static final ThreadLocal<UUID> CURRENT_TRY_ID = new ThreadLocal<>();
    
    /**
     * Sets the current tryId for this thread and propagates it via OpenTelemetry Baggage.
     * If null is provided, clears the current tryId.
     * 
     * @param tryId the try session ID
     */
    public static void setTryId(UUID tryId) {
        if (tryId != null) {
            CURRENT_TRY_ID.set(tryId);
            
            // Propagate to OpenTelemetry Baggage
            try {
                Baggage currentBaggage = Baggage.current();
                BaggageBuilder builder = currentBaggage.toBuilder();
                builder.put(BAGGAGE_KEY, tryId.toString());
                Baggage updatedBaggage = builder.build();
                updatedBaggage.makeCurrent();
                log.debug("Set tryId in context and baggage: {}", tryId);
            } catch (Exception e) {
                // OpenTelemetry not available, just log
                log.trace("OpenTelemetry Baggage not available: {}", e.getMessage());
                log.debug("Set tryId in context: {}", tryId);
            }
        } else {
            // Clear the tryId if null is provided
            UUID removed = CURRENT_TRY_ID.get();
            CURRENT_TRY_ID.remove();
            
            if (removed != null) {
                // Clear from Baggage
                try {
                    Baggage currentBaggage = Baggage.current();
                    BaggageBuilder builder = currentBaggage.toBuilder();
                    builder.remove(BAGGAGE_KEY);
                    Baggage updatedBaggage = builder.build();
                    updatedBaggage.makeCurrent();
                    log.debug("Cleared tryId from context and baggage: {}", removed);
                } catch (Exception e) {
                    log.trace("OpenTelemetry Baggage not available: {}", e.getMessage());
                    log.debug("Cleared tryId from context: {}", removed);
                }
            }
        }
    }
    
    /**
     * Gets the current tryId for this thread.
     * 
     * @return the try session ID, or null if not set
     */
    public static UUID getTryId() {
        return CURRENT_TRY_ID.get();
    }
    
    /**
     * Checks if there is a tryId in the current context.
     * 
     * @return true if tryId is set, false otherwise
     */
    public static boolean hasTryId() {
        return CURRENT_TRY_ID.get() != null;
    }
    
    /**
     * Clears the current tryId from this thread and Baggage.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        UUID removed = CURRENT_TRY_ID.get();
        CURRENT_TRY_ID.remove();
        
        if (removed != null) {
            // Clear from Baggage
            try {
                Baggage currentBaggage = Baggage.current();
                BaggageBuilder builder = currentBaggage.toBuilder();
                builder.remove(BAGGAGE_KEY);
                Baggage updatedBaggage = builder.build();
                updatedBaggage.makeCurrent();
                log.debug("Cleared tryId from context and baggage: {}", removed);
            } catch (Exception e) {
                log.trace("OpenTelemetry Baggage not available: {}", e.getMessage());
                log.debug("Cleared tryId from context: {}", removed);
            }
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
