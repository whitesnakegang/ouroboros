package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI specification data transfer object.
 * <p>
 * Represents an AsyncAPI 3.0.0 specification for WebSocket communication.
 * This class implements {@link OuroApiSpec} to provide protocol and version information.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OuroWebSocketApiSpec implements OuroApiSpec {

    private String asyncapi;
    private Info info;
    private String defaultContentType;
    private Map<String, Server> servers;
    private Map<String, Channel> channels;
    private Components components;
    private Map<String, Operation> operations;

    /**
     * Provide the API protocol identifier.
     *
     * @return the protocol identifier "websocket"
     */
    @Override
    public String getProtocol() {
        return "websocket";
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
