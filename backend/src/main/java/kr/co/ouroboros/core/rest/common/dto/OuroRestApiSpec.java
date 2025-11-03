package kr.co.ouroboros.core.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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

    /**
     * Provide the API protocol identifier.
     *
     * @return the protocol identifier "rest"
     */
    @Override
    public String getProtocol() {
        return "rest";
    }

    /**
     * Get the API version declared in this specification.
     *
     * @return the version string from {@code info}, or an empty string if {@code info} is null
     */
    @Override
    public String getVersion() {
        return info != null ? info.getVersion() : "";
    }
}
