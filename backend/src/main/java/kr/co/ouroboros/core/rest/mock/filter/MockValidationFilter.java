package kr.co.ouroboros.core.rest.mock.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class MockValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;
        EndpointMeta meta = (EndpointMeta) httpReq.getAttribute("mockMeta");

        if (meta == null) {
            chain.doFilter(req, res);
            return;
        }

        // 1. 최우선: X-Ouroboros-Error 헤더 확인
        String forcedError = httpReq.getHeader("x-ouroboros-error");
        if (forcedError != null) {
            try {
                int errorCode = Integer.parseInt(forcedError);
                sendJsonError(httpRes, errorCode, "Forced error response via X-Ouroboros-Error header");
                return;
            } catch (NumberFormatException e) {
                // 잘못된 헤더 값은 무시하고 계속 진행
            }
        }

        // 2. 인증 헤더 검증 (401)
        if (meta.getAuthHeaders() != null) {
            for (String header : meta.getAuthHeaders()) {
                if (httpReq.getHeader(header) == null) {
                    sendJsonError(httpRes, 401, "Authentication required. Missing header: " + header);
                    return;
                }
            }
        }

        // 3. 일반 필수 헤더 검증 (400)
        if (meta.getRequiredHeaders() != null) {
            for (String header : meta.getRequiredHeaders()) {
                if (httpReq.getHeader(header) == null) {
                    sendJsonError(httpRes, 400, "Missing required header: " + header);
                    return;
                }
            }
        }

        // 4. 필수 파라미터 검증 (400)
        if (meta.getRequiredParams() != null) {
            for (String param : meta.getRequiredParams()) {
                if (httpReq.getParameter(param) == null) {
                    sendJsonError(httpRes, 400, "Missing required parameter: " + param);
                    return;
                }
            }
        }

        chain.doFilter(req, res);
    }

    private void sendJsonError(HttpServletResponse res, int statusCode, String message) throws IOException {
        res.setStatus(statusCode);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\": \"" + message + "\"}");
    }

}
