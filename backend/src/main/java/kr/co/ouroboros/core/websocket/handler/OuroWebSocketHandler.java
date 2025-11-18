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
import kr.co.ouroboros.core.websocket.config.WebSocketPrefixProperties;
import kr.co.ouroboros.core.websocket.handler.helper.ChannelAddressNormalizer;
import kr.co.ouroboros.core.websocket.handler.pipeline.WebSocketSpecSyncPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * WebSocket protocol handler that uses Springwolf for code scanning.
 * <p>
 * This handler is only active when:
 * <ul>
 *   <li>Springwolf is available on the classpath</li>
 *   <li>Springwolf is enabled via {@code springwolf.enabled=true}</li>
 * </ul>
 * <p>
 * When active, this handler takes priority over {@link BasicWebSocketHandler} via {@code @Primary}.
 * If Springwolf is not configured, users should set {@code springwolf.enabled=false}
 * to use {@link BasicWebSocketHandler} without Springwolf dependencies.
 *
 * @see BasicWebSocketHandler
 * @since 0.1.0
 */
@Component("ouroWebSocketHandler")
@Primary
@ConditionalOnClass(name = "io.github.springwolf.core.asyncapi.AsyncApiService")
@ConditionalOnProperty(prefix = "springwolf", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class OuroWebSocketHandler implements OuroProtocolHandler {

    private final AsyncApiService asyncApiService;
    private final WebSocketSpecSyncPipeline pipeline;
    private final WebSocketPrefixProperties prefixProperties;
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
     * <p>
     * This method scans the current application code using Springwolf and normalizes
     * channel addresses by adding appropriate STOMP destination prefixes. Springwolf
     * captures only the {@code @MessageMapping} path (e.g., "/chat/send") without the
     * application destination prefix, so this method adds the configured prefix
     * (e.g., "/app") to produce the full STOMP destination (e.g., "/app/chat/send").
     *
     * @return an OuroApiSpec representing the current AsyncAPI state with normalized channel addresses
     * @throws RuntimeException if the AsyncAPI cannot be serialized or converted to the websocket spec
     */
    @Override
    public OuroApiSpec scanCurrentState() {
        try {
            AsyncAPI asyncAPI = asyncApiService.getAsyncAPI();
            String json = Json31.mapper().writeValueAsString(asyncAPI);
            OuroWebSocketApiSpec spec = mapper.readValue(json, OuroWebSocketApiSpec.class);

            // Normalize channel addresses by adding appropriate prefixes
            ChannelAddressNormalizer.normalizeChannelAddresses(spec, prefixProperties);

            return spec;
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