package kr.co.ouroboros.core.rest.mock.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import kr.co.ouroboros.core.rest.mock.registry.RestMockRegistry;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public class MockRoutingFilter implements Filter{
    private final RestMockRegistry registry;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        Optional<EndpointMeta> metaOpt = registry.find(http.getRequestURI(), http.getMethod());
        // Registry에 없으면 실제 구현으로 pass
        if (metaOpt.isEmpty()) {
            chain.doFilter(req, res);
            return;
        }

        // Registry에 있으면 mock 대상
        req.setAttribute("mockMeta", metaOpt.get());
        chain.doFilter(req, res);
    }
}
