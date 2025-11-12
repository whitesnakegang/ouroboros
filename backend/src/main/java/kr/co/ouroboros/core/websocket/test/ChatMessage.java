package kr.co.ouroboros.core.websocket.test;

public class ChatMessage {
    public enum Type { ENTER, CHAT, LEAVE }

    private Type type;
    private String roomId;   // 간단히 방 개념도 넣어둠 (원하면 고정 사용 가능)
    private String sender;
    private String content;

    public ChatMessage() {}

    public ChatMessage(Type type, String roomId, String sender, String content) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
