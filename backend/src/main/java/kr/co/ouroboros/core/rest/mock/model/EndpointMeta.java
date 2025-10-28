package kr.co.ouroboros.core.rest.mock.model;
import kr.co.ouroboros.core.global.mock.model.ResponseMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointMeta {
    private String id;
    private String path;
    private String method;
    private String status;
    private List<String> requiredHeaders;
    private List<String> requiredParams;
    private Map<Integer, ResponseMeta> responses;
}
