package c102.com.demoapigen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "demoapigen")
public class DemoApiGenProperties {

    private List<DefaultStatusResponse> defaultResponses = createDefaultResponses();

    private static List<DefaultStatusResponse> createDefaultResponses() {
        List<DefaultStatusResponse> responses = new ArrayList<>();

        DefaultStatusResponse success = new DefaultStatusResponse();
        success.setStatusCode(200);
        success.setDescription("Success");
        responses.add(success);

        DefaultStatusResponse badRequest = new DefaultStatusResponse();
        badRequest.setStatusCode(400);
        badRequest.setDescription("Bad Request");
        responses.add(badRequest);

        DefaultStatusResponse unauthorized = new DefaultStatusResponse();
        unauthorized.setStatusCode(401);
        unauthorized.setDescription("Unauthorized");
        responses.add(unauthorized);

        DefaultStatusResponse notFound = new DefaultStatusResponse();
        notFound.setStatusCode(404);
        notFound.setDescription("Not Found");
        responses.add(notFound);

        DefaultStatusResponse serverError = new DefaultStatusResponse();
        serverError.setStatusCode(500);
        serverError.setDescription("Server Error");
        responses.add(serverError);

        return responses;
    }

    @Data
    public static class DefaultStatusResponse {
        private Integer statusCode;
        private String description;
    }
}
