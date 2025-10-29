package kr.co.ouroboros.core.rest.tryit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;
import java.util.UUID;

/**
 * ResponseBodyAdvice를 사용하여 응답 body에 tryId를 추가합니다.
 * 이 방법은 사용자 필터와 완전히 독립적으로 동작합니다.
 * 
 * 장점:
 * - 사용자 필터 순서에 영향받지 않음
 * - Response가 이미 커밋된 후에도 동작
 * - 모든 컨트롤러 메서드에 자동 적용
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class TryResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private static final String RESPONSE_FIELD_NAME = "_ouroborosTryId";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // TryContext에 tryId가 있거나 Try 헤더가 있을 때 동작
        return TryContext.hasTryId();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request, ServerHttpResponse response) {
        
        if (!TryContext.hasTryId()) {
            return body;
        }

        UUID tryId = TryContext.getTryId();
        log.debug("Adding tryId to response body: {}", tryId);

        try {
            // Map 타입인 경우 직접 추가
            if (body instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) body;
                
                // 이미 tryId가 있으면 중복 추가 방지
                if (!map.containsKey(RESPONSE_FIELD_NAME)) {
                    map.put(RESPONSE_FIELD_NAME, tryId.toString());
                }
                return map;
            }
            
            // String 타입인 경우 JSON 파싱 후 수정
            if (body instanceof String) {
                String jsonString = (String) body;
                
                // 이미 tryId가 있으면 중복 추가 방지
                if (jsonString.contains(RESPONSE_FIELD_NAME)) {
                    return body;
                }
                
                try {
                    var root = objectMapper.readTree(jsonString);
                    if (root.isObject()) {
                        var modified = (com.fasterxml.jackson.databind.node.ObjectNode) root;
                        modified.put(RESPONSE_FIELD_NAME, tryId.toString());
                        return objectMapper.writeValueAsString(modified);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse JSON string, creating wrapper: {}", e.getMessage());
                    return String.format("{\"_ouroborosTryId\":\"%s\",\"originalResponse\":%s}", 
                        tryId.toString(), jsonString);
                }
            }
            
            // 기타 타입인 경우 래퍼 객체 생성
            return Map.of(
                RESPONSE_FIELD_NAME, tryId.toString(),
                "originalResponse", body
            );
            
        } catch (Exception e) {
            log.error("Failed to add tryId to response body", e);
            return body; // 실패 시 원본 반환
        }
    }
}
