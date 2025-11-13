package kr.co.ouroboros.core.websocket.test;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
// TODO 삭제
public class ChatMessage {

    private String messageId;
    private String userId;
    private String content;
    private String timestamp;
    private List<MessageItem> attachments;
}