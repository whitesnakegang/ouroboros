package kr.co.ouroboros.core.websocket.tryit.identification;

import java.time.Instant;
import java.util.Map;

/**
 * 발행자에게 Try 요청 메타데이터를 전달하기 위한 메시지 페이로드.
 */
public record TryDispatchMessage(
        String tryId,
        String destination,
        String payload,
        Map<String, String> headers,
        Instant requestedAt
) {
}

