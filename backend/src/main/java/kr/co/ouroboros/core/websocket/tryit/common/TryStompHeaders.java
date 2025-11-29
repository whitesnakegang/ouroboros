package kr.co.ouroboros.core.websocket.tryit.common;

import kr.co.ouroboros.core.global.tryit.common.TryHeaders;

/**
 * Defines header and attribute names used in STOMP Try functionality.
 * <p>
 * This class provides STOMP-specific Try headers and delegates to {@link TryHeaders}
 * for protocol-agnostic header constants shared with REST.
 * <p>
 * <b>Protocol-agnostic headers (delegated to {@link TryHeaders}):</b>
 * <ul>
 *   <li>{@link #TRY_HEADER} - Identifies Try requests</li>
 *   <li>{@link #TRY_ID_HEADER} - Carries Try identifier</li>
 *   <li>{@link #TRY_HEADER_ENABLED_VALUE} - Indicates Try is enabled</li>
 * </ul>
 * <p>
 * <b>STOMP-specific headers:</b>
 * <ul>
 *   <li>{@link #INTERNAL_SCOPE_HEADER} - Internal scope management for STOMP interceptors</li>
 * </ul>
 *
 * @deprecated Use {@link TryHeaders} for protocol-agnostic Try header constants.
 *             This class will be removed in a future version. STOMP-specific constants
 *             will be moved to a dedicated internal package.
 * @see TryHeaders
 */
@Deprecated
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
     * <p>
     * Delegates to {@link TryHeaders#TRY_HEADER}.
     *
     * @deprecated Use {@link TryHeaders#TRY_HEADER} directly for protocol-agnostic Try header.
     * @see TryHeaders#TRY_HEADER
     */
    @Deprecated
    public static final String TRY_HEADER = TryHeaders.TRY_HEADER;

    /**
     * Header name for carrying Try identifier.
     * <p>
     * Delegates to {@link TryHeaders#TRY_ID_HEADER}.
     *
     * @deprecated Use {@link TryHeaders#TRY_ID_HEADER} directly for protocol-agnostic Try identifier header.
     * @see TryHeaders#TRY_ID_HEADER
     */
    @Deprecated
    public static final String TRY_ID_HEADER = TryHeaders.TRY_ID_HEADER;

    /**
     * Value indicating that Try header is enabled.
     * <p>
     * Delegates to {@link TryHeaders#TRY_HEADER_ENABLED_VALUE}.
     *
     * @deprecated Use {@link TryHeaders#TRY_HEADER_ENABLED_VALUE} directly for protocol-agnostic Try enabled value.
     * @see TryHeaders#TRY_HEADER_ENABLED_VALUE
     */
    @Deprecated
    public static final String TRY_HEADER_ENABLED_VALUE = TryHeaders.TRY_HEADER_ENABLED_VALUE;

    /**
     * Header name used internally by STOMP interceptors for Scope management.
     * <p>
     * This header is specific to the STOMP protocol implementation and is not
     * used in REST. It stores the OpenTelemetry Scope object for cleanup in
     * the afterSendCompletion phase of STOMP channel interceptors.
     */
    public static final String INTERNAL_SCOPE_HEADER = "kr.co.ouroboros.tryScope";
}
