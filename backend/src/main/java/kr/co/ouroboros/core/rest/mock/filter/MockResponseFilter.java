package kr.co.ouroboros.core.rest.mock.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import kr.co.ouroboros.core.global.mock.model.ResponseMeta;
import kr.co.ouroboros.core.global.mock.service.DummyDataGenerator;
import kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class MockResponseFilter implements Filter{
    private final DummyDataGenerator generator;
    private final SchemaMockBuilder schemaMockBuilder;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;
        EndpointMeta meta = (EndpointMeta) http.getAttribute("mockMeta");

        if (meta == null) {
            chain.doFilter(req, res);
            return;
        }

        // 요청자가 특정 상태 코드를 강제로 요청할 수도 있음
        String forcedError = http.getHeader("X-Ouroboros-Error");
        int statusCode = 200;
        if (forcedError != null && meta.getResponses().containsKey(Integer.parseInt(forcedError))) {
            statusCode = Integer.parseInt(forcedError);
        }

        // 응답 스키마 선택
        ResponseMeta responseMeta = meta.getResponses().get(statusCode);
        Object body;
        if (responseMeta.getSchema() != null) {
            body = schemaMockBuilder.build(responseMeta.getSchema());
        } else if (responseMeta.getProperties() != null) {
            body = responseMeta.getProperties();
        } else {
            body = Map.of("message", "No schema or properties found");
        }

        // Accept 헤더 및 content-type 결정
        String accept = http.getHeader("Accept");
        String contentType = responseMeta.getContentType(); // YAML의 contentType 반영
        if (contentType == null) {
            contentType = (accept != null && accept.toLowerCase().contains("xml"))
                    ? "application/xml"
                    : "application/json";
        }

        String bodyText;
        if (contentType.contains("xml")) {
            bodyText = xmlMapper.writeValueAsString(body);
            httpRes.setContentType("application/xml;charset=UTF-8");
        } else {
            bodyText = objectMapper.writeValueAsString(body);
            httpRes.setContentType("application/json;charset=UTF-8");
        }

        httpRes.setStatus(statusCode);
        httpRes.getWriter().write(bodyText);
    }
}
