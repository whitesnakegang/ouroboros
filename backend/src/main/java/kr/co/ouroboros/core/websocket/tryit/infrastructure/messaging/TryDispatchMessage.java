package kr.co.ouroboros.core.websocket.tryit.infrastructure.messaging;

import java.util.Map;

/**
 * Message payload for delivering Try request metadata to the publisher.
 * <p>
 * The tryId is included in STOMP message headers ({@link kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders#TRY_ID_HEADER}),
 * so the payload only contains original message information.
 * <p>
 * The destination is included in STOMP message headers, so it is removed from the payload.
 * <p>
 * The request timestamp should be recorded by the client when needed for accuracy.
 * The server-generated timestamp may differ from the client send time due to network latency.
 */
public record TryDispatchMessage(
        String payload,
        Map<String, String> headers
) {
}

