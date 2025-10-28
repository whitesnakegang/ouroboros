package kr.co.ouroboros.core.global.mock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseMeta {
    private int statusCode;
    private Map<String, Object> headers;
    private Map<String, Object> body;
    private String contentType;                   // JSON of XML
}
