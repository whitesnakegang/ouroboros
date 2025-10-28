package kr.co.ouroboros.core.rest.mock.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;

import java.io.IOException;

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

        if (meta.getRequiredHeaders() != null) {
            for (String header : meta.getRequiredHeaders()) {
                if (http.getHeader(header) == null) {
                    sendError(httpRes, 400, "Missing required header: " + header);
                    return;
                }
            }
        }

        if (meta.getRequiredParams() != null) {
            for (String param : meta.getRequiredParams()) {
                if (http.getParameter(param) == null) {
                    sendError(httpRes, 400, "Missing required parameter: " + param);
                    return;
                }
            }
        }

        chain.doFilter(req, res);
    }

    private void sendError(HttpServletResponse res, int code, String msg) throws IOException {
        res.setStatus(code);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\": \"" + msg + "\"}");
    }
}
