package kr.co.ouroboros.core.rest.tryit.filter;

import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.UUID;

/**
 * ResponseBodyAdvice를 사용하여 Try 요청에 대해 응답 헤더에 tryId를 설정합니다.
 * 이 방법은 사용자 필터와 완전히 독립적으로 동작합니다.
 * 
 * 역할:
 * - 응답 커밋 직전에 헤더에 tryId 설정 (Filter에서 누락된 경우 대비)
 * 
 * 장점:
 * - 사용자 필터 순서에 영향받지 않음
 * - 응답 커밋 직전에 실행되므로 헤더 설정이 안전함
 * - 모든 컨트롤러 메서드에 자동 적용
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class TryResponseAdvice implements ResponseBodyAdvice<Object> {

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
        
        // ResponseBodyAdvice는 응답 커밋 직전에 실행되므로, 헤더를 안전하게 설정할 수 있습니다.
        // Filter에서 이미 헤더를 설정했는지 확인하고, 없을 때만 설정합니다.
        try {
            var headers = response.getHeaders();
            if (!headers.containsKey("X-Ouroboros-Try-Id")) {
                // 헤더가 없으면 설정 (Filter에서 누락된 경우 대비)
                headers.set("X-Ouroboros-Try-Id", tryId.toString());
                log.debug("Set tryId in response header (Filter missed): {}", tryId);
            } else {
                // 헤더가 이미 있으면 Filter에서 설정한 것이므로 건너뜀
                log.trace("TryId header already set by Filter, skipping");
            }
        } catch (Exception e) {
            // 응답이 이미 커밋되었거나 헤더 설정이 불가능한 경우
            log.warn("Failed to set X-Ouroboros-Try-Id header (response may be committed): {}", e.getMessage());
        }

        // Body는 수정하지 않고 원본 그대로 반환
        return body;
    }
}

