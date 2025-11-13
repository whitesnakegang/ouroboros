package kr.co.ouroboros.core.websocket.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 서버 내부에서 발생하는 채팅 관련 이벤트 (예: 공지, 입장 알림 등)
 */
@Getter
@ToString
@AllArgsConstructor
public class SomeEvent {
    private String roomId;   // 이벤트가 발생한 방 ID
    private String message;  // 보낼 메시지 내용
}