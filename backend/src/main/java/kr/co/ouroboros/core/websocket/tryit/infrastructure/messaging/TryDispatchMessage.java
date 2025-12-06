package kr.co.ouroboros.core.websocket.tryit.infrastructure.messaging;

import java.util.Map;

/**
 * Message payload for delivering Try request metadata to the publisher.
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

