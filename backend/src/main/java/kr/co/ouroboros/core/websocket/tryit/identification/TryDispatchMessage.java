package kr.co.ouroboros.core.websocket.tryit.identification;

import java.util.Map;

/**
 * 발행자에게 Try 요청 메타데이터를 전달하기 위한 메시지 페이로드.
 * <p>
 * tryId는 STOMP 메시지 헤더({@link kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders#TRY_ID_HEADER})에 포함되므로
 * payload에는 원본 메시지 정보만 포함한다.
 * <p>
 * destination은 STOMP 메시지 헤더에 포함되므로 payload에서 제거한다.
 * <p>
 * 요청 시점은 클라이언트가 필요 시 자체적으로 기록하는 것이 더 정확하다.
 * 서버에서 생성한 시점은 네트워크 지연으로 인해 클라이언트 전송 시점과 다를 수 있다.
 */
public record TryDispatchMessage(
        String payload,
        Map<String, String> headers
) {
}

