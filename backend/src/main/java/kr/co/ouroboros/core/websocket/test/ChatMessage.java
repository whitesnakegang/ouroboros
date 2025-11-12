package kr.co.ouroboros.core.websocket.test;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    public enum Type { ENTER, CHAT, LEAVE }

    private Type type;
    private String roomId;   // 간단히 방 개념도 넣어둠 (원하면 고정 사용 가능)
    private String sender;
    private String content;
    private User user;
    private List<String> list;
}