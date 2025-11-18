package kr.co.ouroboros.core.websocket.handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.Components;
import kr.co.ouroboros.core.websocket.common.dto.Info;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.handler.pipeline.WebSocketSpecSyncPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Basic WebSocket protocol handler that works without Springwolf.
 * <p>
 * This handler provides minimal functionality for WebSocket API specification management:
 * <ul>
 *   <li>YAML file parsing and loading</li>
 *   <li>Specification validation and synchronization</li>
 *   <li>YAML file persistence</li>
 * </ul>
 * <p>
 * Code scanning is not available without Springwolf. The {@link #scanCurrentState()} method
 * returns an empty specification.
 * <p>
 * This handler is active by default unless a Springwolf-based handler is available.
 * When Springwolf is enabled, {@link OuroWebSocketHandler} takes priority via {@code @Primary}.
 *
 * @see OuroWebSocketHandler
 * @since 0.1.0
 */
@Component
@ConditionalOnMissingBean(name = "ouroWebSocketHandler")
@RequiredArgsConstructor
public class BasicWebSocketHandler implements OuroProtocolHandler {

    private final WebSocketSpecSyncPipeline pipeline;
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /**
     * Indicates that this handler targets the WebSocket protocol.
     *
     * @return the WebSocket protocol
     */
    @Override
    public Protocol getProtocol() {
        return Protocol.WEB_SOCKET;
    }

    /**
     * Provide the relative path to the WebSocket AsyncAPI specification file.
     *
     * @return the relative classpath location "ouroboros/websocket/ourowebsocket.yml"
     */
    @Override
    public String getSpecFilePath() {
        return "ouroboros/websocket/ourowebsocket.yml";
    }

    /**
     * Returns an empty WebSocket API specification since code scanning is not available
     * without Springwolf.
     * <p>
     * To enable code scanning, add Springwolf dependencies and set {@code springwolf.enabled=true}.
     *
     * @return an empty OuroWebSocketApiSpec
     */
    @Override
    public OuroApiSpec scanCurrentState() {
        // Create minimal empty spec structure
        OuroWebSocketApiSpec emptySpec = new OuroWebSocketApiSpec();
        emptySpec.setAsyncapi("3.0.0");

        // Create Info object with minimal required fields
        Info info = new Info();
        info.setTitle("WebSocket API");
        info.setVersion("1.0.0");
        emptySpec.setInfo(info);

        emptySpec.setDefaultContentType("application/json");
        emptySpec.setServers(new HashMap<>());
        emptySpec.setChannels(new HashMap<>());
        emptySpec.setOperations(new HashMap<>());

        // Create empty Components object
        Components components = new Components();
        components.setSchemas(new HashMap<>());
        components.setMessages(new HashMap<>());
        emptySpec.setComponents(components);

        return emptySpec;
    }

    /**
     * Parse YAML content describing a WebSocket API specification.
     *
     * @param yamlContent YAML document text containing the WebSocket API spec
     * @return an OuroWebSocketApiSpec representing the parsed specification
     */
    @Override
    public OuroApiSpec loadFromFile(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlContent);

        return mapper.convertValue(map, OuroWebSocketApiSpec.class);
    }

    /**
     * Validates and reconciles a file-based API specification against the currently scanned specification.
     * <p>
     * Since code scanning is not available without Springwolf, this method primarily validates
     * the file specification and returns it if valid.
     *
     * @param fileSpec    the specification loaded from the file to validate
     * @param scannedSpec the specification discovered from the running system (empty without Springwolf)
     * @return the resulting validated and reconciled {@code OuroApiSpec}
     */
    @Override
    public OuroApiSpec synchronize(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {
        return pipeline.validate(fileSpec, scannedSpec);
    }

    /**
     * Persists the given OuroApiSpec as YAML to the handler's configured spec file location.
     *
     * @param specToSave the API specification to serialize and save; must not be null
     */
    @Override
    public void saveYaml(OuroApiSpec specToSave) {
        // TODO: Implement YAML file saving logic
    }
}