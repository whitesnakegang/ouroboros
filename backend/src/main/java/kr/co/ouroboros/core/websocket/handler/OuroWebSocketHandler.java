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

    @Override
    public Protocol getProtocol() {
        return Protocol.WEB_SOCKET;
    }
    
    @Override
    public String getSpecFilePath() {
        return "ouroboros/websocket/ourowebsocket.yml";
    }
    
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
    
    @Override
    public OuroApiSpec loadFromFile(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlContent);

        return mapper.convertValue(map, OuroWebSocketApiSpec.class);
    }
    
    @Override
    public OuroApiSpec synchronize(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {
        return pipeline.validate(fileSpec, scannedSpec);
    }
    
    @Override
    public void saveYaml(OuroApiSpec specToSave) {
    
    }
}
