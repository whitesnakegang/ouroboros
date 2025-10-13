package c102.com.demoapigen.model;

import lombok.Data;
import java.util.List;

@Data
public class Request {
    private String type;        // "body", "query", "none"
    private String contentType; // "json", "formData" (body인 경우)
    private List<Field> fields; // Request 필드 정의
}
