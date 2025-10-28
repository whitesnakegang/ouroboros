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
        HttpServletRequest http = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;
        EndpointMeta meta = (EndpointMeta) http.getAttribute("mockMeta");

        if (meta == null) {
            chain.doFilter(req, res);
            return;
        }

        // 1. 최우선: X-Ouroboros-Error 헤더 확인
        String forcedError = http.getHeader("X-Ouroboros-Error");
        if (forcedError != null) {
            try {
                int errorCode = Integer.parseInt(forcedError);
                // 에러 정보만 설정하고 MockResponseFilter로 넘김
                req.setAttribute("validationError", errorCode);
                chain.doFilter(req, res);
                return;
            } catch (NumberFormatException e) {
                // 잘못된 헤더 값은 무시하고 계속 진행
            }
        }

        // 2. 인증 헤더 검증 (401)
        if (meta.getAuthHeaders() != null) {
            for (String header : meta.getAuthHeaders()) {
                if (http.getHeader(header) == null) {
                    req.setAttribute("validationError", 401);
                    req.setAttribute("validationMessage", "Authentication required. Missing header: " + header);
                    chain.doFilter(req, res);
                    return;
                }
            }
        }

        // 3. 일반 필수 헤더 검증 (400)
        if (meta.getRequiredHeaders() != null) {
            for (String header : meta.getRequiredHeaders()) {
                if (http.getHeader(header) == null) {
                    req.setAttribute("validationError", 400);
                    req.setAttribute("validationMessage", "Missing required header: " + header);
                    chain.doFilter(req, res);
                    return;
                }
            }
        }

        // 4. 필수 파라미터 검증 (400)
        if (meta.getRequiredParams() != null) {
            for (String param : meta.getRequiredParams()) {
                if (http.getParameter(param) == null) {
                    req.setAttribute("validationError", 400);
                    req.setAttribute("validationMessage", "Missing required parameter: " + param);
                    chain.doFilter(req, res);
                    return;
                }
            }
        }

        chain.doFilter(req, res);
    }

}
