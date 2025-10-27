package kr.co.ouroboros.core.rest.tryit.util;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Thread-local utility for managing Try context.
 * Stores the current tryId in ThreadLocal for use throughout the request lifecycle.
 * 
 * This acts as a bridge for OpenTelemetry Baggage integration:
 * - TryFilter sets the tryId in ThreadLocal
 * - OpenTelemetry Sampler/Instrumentation can read it
 * - Cleaned up after request processing
 */
@Slf4j
public class TryContext {
    
    private static final ThreadLocal<UUID> CURRENT_TRY_ID = new ThreadLocal<>();
    
    /**
     * Sets the current tryId for this thread.
     * 
     * @param tryId the try session ID
     */
    public static void setTryId(UUID tryId) {
        if (tryId != null) {
            CURRENT_TRY_ID.set(tryId);
            log.debug("Set tryId in context: {}", tryId);
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
     * Clears the current tryId from this thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        UUID removed = CURRENT_TRY_ID.get();
        CURRENT_TRY_ID.remove();
        if (removed != null) {
            log.debug("Cleared tryId from context: {}", removed);
        }
    }
}
