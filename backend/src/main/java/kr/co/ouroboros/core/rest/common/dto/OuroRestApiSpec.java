package kr.co.ouroboros.core.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OuroRestApiSpec implements OuroApiSpec {

    private String openapi;
    private Info info;
    private Map<String, PathItem> paths;
    private Components components;
    private List<Server> servers;
    private List<Map<String, List<String>>> security;

    @Override
    public String getProtocol() {
        return "rest";
    }

    @Override
    public String getVersion() {
        return info != null ? info.getVersion() : "";
    }
}
