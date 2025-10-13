package c102.com.demoapigen.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Endpoint {
    private String path;
    private String method;
    private String description;
    private Boolean requiresAuth;  // 인증 필요 여부
    private String authType;       // "bearer", "basic", "apiKey", "custom"
    private String authHeader;     // custom인 경우 헤더명 (예: "X-API-Key")
    private Request request;       // Request 정의
    private List<StatusResponse> responses;  // 각 status code별 응답 정의
}
