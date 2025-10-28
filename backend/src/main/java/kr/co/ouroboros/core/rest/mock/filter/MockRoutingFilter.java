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
        if (metaOpt.isEmpty() || !"mock".equalsIgnoreCase(metaOpt.get().getStatus())) {
            chain.doFilter(req, res);
            return;
        }
        req.setAttribute("mockMeta", metaOpt.get());
        chain.doFilter(req, res);
    }
}
