package kr.co.ouroboros.core.websocket.tryit.common;

/**
 * Defines header and attribute names used in STOMP Try functionality.
 * <p>
 * This class provides STOMP-specific Try headers.
 * <p>
 * <b>STOMP-specific headers:</b>
 * <ul>
 *   <li>{@link #INTERNAL_SCOPE_HEADER} - Internal scope management for STOMP interceptors</li>
 * </ul>
 */
public final class TryStompHeaders {

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws IllegalStateException indicating the class must not be instantiated
     */
    private TryStompHeaders() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Header name used internally by STOMP interceptors for Scope management.
     * <p>
     * This header is specific to the STOMP protocol implementation and is not
     * used in REST. It stores the OpenTelemetry Scope object for cleanup in
     * the afterSendCompletion phase of STOMP channel interceptors.
     */
    public static final String INTERNAL_SCOPE_HEADER = "kr.co.ouroboros.tryScope";
}
