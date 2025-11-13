package kr.co.ouroboros.core.websocket.tryit.common;

/**
 * Defines header and attribute names used in STOMP Try functionality.
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
     * Header name indicating whether a Try request is present.
     */
    public static final String TRY_HEADER = "X-Ouroboros-Try";

    /**
     * Header name for carrying Try identifier.
     */
    public static final String TRY_ID_HEADER = "X-Ouroboros-Try-Id";

    /**
     * Attribute name for storing tryId in STOMP session.
     */
    public static final String SESSION_TRY_ID_ATTR = "kr.co.ouroboros.tryId";

    /**
     * Header name used internally by interceptors for Scope management.
     */
    public static final String INTERNAL_SCOPE_HEADER = "kr.co.ouroboros.tryScope";

    /**
     * Header name used by outbound interceptor for Scope management.
     */
    public static final String INTERNAL_OUTBOUND_SCOPE_HEADER = "kr.co.ouroboros.tryOutboundScope";

    /**
     * Value indicating that Try header is enabled.
     */
    public static final String TRY_HEADER_ENABLED_VALUE = "on";
}

