package kr.co.ouroboros.core.global.tryit;

/**
 * Defines protocol-agnostic header names used in Try functionality across REST and WebSocket.
 * <p>
 * These constants represent HTTP headers that identify and track Try requests,
 * regardless of the underlying protocol (REST or WebSocket/STOMP).
 * <p>
 * <b>Usage:</b>
 * <ul>
 *   <li>REST: {@link kr.co.ouroboros.core.rest.tryit.identification.TryFilter}</li>
 *   <li>REST: {@link kr.co.ouroboros.core.rest.tryit.identification.TryResponseAdvice}</li>
 *   <li>Global: {@link kr.co.ouroboros.core.global.tryit.infrastructure.instrumentation.sampler.TryOnlySampler}</li>
 *   <li>WebSocket: {@link kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders}</li>
 * </ul>
 *
 * @since 0.0.1
 * @see kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders
 */
public final class TryHeaders {

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws IllegalStateException indicating the class must not be instantiated
     */
    private TryHeaders() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Header name indicating whether a Try request is present.
     * <p>
     * Used in both REST and WebSocket protocols to identify Try requests.
     * When this header is set to {@link #TRY_HEADER_ENABLED_VALUE}, the request
     * is treated as a Try request and a tryId is generated.
     */
    public static final String TRY_HEADER = "X-Ouroboros-Try";

    /**
     * Header name for carrying Try identifier.
     * <p>
     * This header carries the UUID that uniquely identifies a Try request.
     * Used in both REST responses and WebSocket STOMP messages.
     */
    public static final String TRY_ID_HEADER = "X-Ouroboros-Try-Id";

    /**
     * Value indicating that Try header is enabled.
     * <p>
     * When {@link #TRY_HEADER} is set to this value ("on"),
     * the request is identified as a Try request.
     */
    public static final String TRY_HEADER_ENABLED_VALUE = "on";
}
