package kr.co.ouroboros.core.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.springwolf.asyncapi.v3.model.AsyncAPI;
import io.github.springwolf.core.asyncapi.AsyncApiService;
import io.swagger.v3.core.util.Json31;
import java.util.Map;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import kr.co.ouroboros.core.websocket.common.dto.OuroWebSocketApiSpec;
import kr.co.ouroboros.core.websocket.handler.pipeline.WebSocketSpecSyncPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
@RequiredArgsConstructor
public class OuroWebSocketHandler implements OuroProtocolHandler {

    private final AsyncApiService asyncApiService;
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
     * Creates an OuroApiSpec that represents the current AsyncAPI state.
     *
     * @return an OuroApiSpec representing the current AsyncAPI state
     * @throws RuntimeException if the AsyncAPI cannot be serialized or converted to the websocket spec
     */
    @Override
    public OuroApiSpec scanCurrentState() {
        try {
        AsyncAPI asyncAPI = asyncApiService.getAsyncAPI();
            String json = Json31.mapper().writeValueAsString(asyncAPI);
            return mapper.readValue(json, OuroWebSocketApiSpec.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
     *
     * @param fileSpec    the specification loaded from the file to validate
     * @param scannedSpec the specification discovered from the running system to validate against
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
    
    }
}