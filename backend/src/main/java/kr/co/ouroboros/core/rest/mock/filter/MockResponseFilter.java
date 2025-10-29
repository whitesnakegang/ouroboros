package kr.co.ouroboros.core.rest.mock.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import kr.co.ouroboros.core.global.mock.model.ResponseMeta;
import kr.co.ouroboros.core.global.mock.service.SchemaMockBuilder;
import kr.co.ouroboros.core.rest.mock.model.EndpointMeta;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class MockResponseFilter implements Filter{
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

        int statusCode = 200;
        // 해당 코드의 ResponseMeta 가져오기
        ResponseMeta responseMeta = meta.getResponses().get(statusCode);
        if (responseMeta == null) {
            httpRes.setStatus(500);
            httpRes.getWriter().write("{\"error\": \"No response definition found\"}");
            return;
        }

        // 헤더 설정
        if (responseMeta.getHeaders() != null) {
            for (var entry : responseMeta.getHeaders().entrySet()) {
                httpRes.setHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        // Body 생성
        Object body=null;
        if (responseMeta.getBody() != null && !responseMeta.getBody().isEmpty()) {
            body = schemaMockBuilder.build(responseMeta.getBody());
        }

        // Content-Type 결정
        String contentType = responseMeta.getContentType();
        String accept = http.getHeader("Accept");

        if (contentType == null) {
            contentType = (accept != null && accept.toLowerCase().contains("xml"))
                    ? "application/xml"
                    : "application/json";
        }

        httpRes.setContentType(contentType + ";charset=UTF-8");
        httpRes.setStatus(responseMeta.getStatusCode() > 0 ? responseMeta.getStatusCode() : statusCode);

        // 직렬화 및 응답 전송
        String bodyText = "";
        if (body != null) {
            if (contentType.contains("xml")) {
                bodyText = xmlMapper.writeValueAsString(body);
            } else {
                bodyText = objectMapper.writeValueAsString(body);
            }
        }

        httpRes.getWriter().write(bodyText);
    }

}
