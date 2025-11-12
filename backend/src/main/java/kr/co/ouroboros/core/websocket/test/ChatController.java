package kr.co.ouroboros.core.websocket.test;

import kr.co.ouroboros.core.global.annotation.ApiState;
import kr.co.ouroboros.core.global.annotation.ApiState.State;
import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 클라이언트는 /app/chat.send 로 메시지를 보내고 구독은 /topic/rooms/{roomId} 로 받는 형태의 최소 예제
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 방 입장/퇴장/채팅 공통 처리 (브로드캐스트)
     */
    @MessageMapping("/chat.send/new_test")  // → /app/chat.send
    @SendTo("/topic/rooms/public") // 기본방 브로드캐스트 (roomId 없을 때)
    @ApiState(state = State.BUGFIX)
    public TestDto broadcastToDefault(@Payload TestDto message) {
        // roomId가 비어있으면 /topic/rooms/public 으로 뿌림 (@SendTo가 처리)
        // 아주 간단하게 “에코” 형태로 반환
        return message;
    }

    @MessageMapping("/chat.send/test")  // → /app/chat.send
    @SendTo("/topic/rooms/public") // 기본방 브로드캐스트 (roomId 없을 때)
    @ApiState(state = State.COMPLETED)
    public ChatMessage broadcast_To_DefaultTest(@Payload ChatMessage message) {
        // roomId가 비어있으면 /topic/rooms/public 으로 뿌림 (@SendTo가 처리)
        // 아주 간단하게 “에코” 형태로 반환
        return message;
    }

    /**
     * roomId가 있을 때는 명시적으로 해당 room topic에 publish
     */

//    @ApiState(state = State.IMPLEMENTING)
//    @MessageMapping("/chat.sendToRoom") // 구독자가 보낼 앱 목적지(consumer, 자동 스캔 대상)
//    @StompAsyncOperationBinding  // STOMP 프로토콜 바인딩 지정
//    public void sendToRoom(@Payload ChatMessage message) {
//        String room = (message.getRoomId() == null || message.getRoomId()
//                .isBlank())
//                ? "public" : message.getRoomId();
//        messagingTemplate.convertAndSend("/topic/rooms/" + room, message);
//    }

//    @EventListener(SomeEvent.class)
//    @SendTo("/topic/rooms/announcement")
//    public ChatMessage onEvent(SomeEvent event) {
//        return new ChatMessage();
//    }
//
//    @Scheduled(fixedRate = 10000) // 10초마다 실행
//    @ApiState(state = State.COMPLETED)
//    public void sendServerNotice() {
//        ChatMessage msg = new ChatMessage();
//        msg.setRoomId("announcement");
//        msg.setContent("[서버 공지] 10초마다 발송!");
//        messagingTemplate.convertAndSend("/topic/rooms/announcement", msg);
//    }
    @Data
    static class TestDto {
        String id;
        String pwd;
        Integer test;
    }
}
