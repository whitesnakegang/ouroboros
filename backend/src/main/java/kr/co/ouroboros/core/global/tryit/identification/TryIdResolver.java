package kr.co.ouroboros.core.global.tryit.identification;

import io.opentelemetry.context.Scope;
import kr.co.ouroboros.core.global.tryit.TryHeaders;
import kr.co.ouroboros.core.global.tryit.context.TryContext;

import java.util.UUID;

/**
 * Utility for Try request identification and tryId generation.
 * <p>
 * This class provides protocol-agnostic utilities for:
 * <ul>
 *   <li>Generating unique tryId (UUID)</li>
 *   <li>Validating Try request headers</li>
 *   <li>Activating Try scope with TryContext</li>
 * </ul>
 * <p>
 * Used by both REST (TryFilter) and WebSocket (TryStompChannelInterceptor)
 * to ensure consistent tryId generation and context management.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public final class TryIdResolver {

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws IllegalStateException indicating the class must not be instantiated
     */
    private TryIdResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Generates a new unique tryId.
     * <p>
     * This method creates a random UUID to be used as the tryId for a Try request.
     *
     * @return a new random UUID representing the tryId
     */
    public static UUID generateTryId() {
        return UUID.randomUUID();
    }

    /**
     * Checks if the given header value indicates a Try request.
     * <p>
     * Compares the header value against {@link TryHeaders#TRY_HEADER_ENABLED_VALUE}
     * in a case-insensitive manner.
     *
     * @param headerValue the value of the Try header (e.g., X-Ouroboros-Try)
     * @return true if the header value equals "on" (case-insensitive), false otherwise
     */
    public static boolean isTryRequest(String headerValue) {
        return TryHeaders.TRY_HEADER_ENABLED_VALUE.equalsIgnoreCase(headerValue);
    }

    /**
     * Activates Try scope by setting the tryId in TryContext.
     * <p>
     * This method generates a new tryId and sets it in the TryContext,
     * returning a Scope that should be closed when the Try request processing is complete.
     *
     * @param tryId the tryId to set in the context
     * @return a {@link Scope} that should be closed to clean up the context,
     *         or null if tryId is null or OpenTelemetry is not available
     */
    public static Scope activateTryScope(UUID tryId) {
        return TryContext.setTryId(tryId);
    }
}
