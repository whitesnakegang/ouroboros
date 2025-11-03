package kr.co.ouroboros.core.rest.tryit.identification;

import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
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
 * ResponseBodyAdvice for setting tryId in response headers for Try requests.
 * <p>
 * This component uses Spring's ResponseBodyAdvice mechanism to set tryId
 * in response headers just before response body is written. This approach
 * works completely independently of user filters.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Sets tryId in response header just before response commit (safety net for Filter)</li>
 *   <li>Works independently of filter execution order</li>
 *   <li>Automatically applies to all controller methods</li>
 * </ul>
 * <p>
 * <b>Advantages:</b>
 * <ul>
 *   <li>Not affected by user filter execution order</li>
 *   <li>Executes just before response commit, ensuring safe header setting</li>
 *   <li>Automatically applies to all controller methods</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class TryResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * Determines whether this advice supports the given return type and converter.
     * <p>
     * Returns true if TryContext has a tryId, indicating this is a Try request.
     *
     * @param returnType Return type of the controller method
     * @param converterType Selected HTTP message converter type
     * @return true if TryContext has tryId, false otherwise
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Act only when TryContext has tryId (indicating Try request)
        return TryContext.hasTryId();
    }

    /**
     * Sets tryId in response header before body is written.
     * <p>
     * This method executes just before the response body is written,
     * allowing safe header modification. Checks if header is already
     * set by Filter; if not, sets it as a safety net.
     *
     * @param body Response body (not modified)
     * @param returnType Return type of the controller method
     * @param selectedContentType Selected media type
     * @param selectedConverterType Selected HTTP message converter type
     * @param request HTTP request
     * @param response HTTP response (header is modified here)
     * @return Original response body (not modified)
     */
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

