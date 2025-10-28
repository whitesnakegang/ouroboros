package kr.co.ouroboros.core.rest.handler;

import io.swagger.v3.oas.models.OpenAPI;
import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OuroRestHandler implements OuroProtocolHandler {

    private final OpenAPI springDocOpenApi;

    /**
     * Identifies this handler's protocol as REST.
     *
     * @return the {@link Protocol#REST} enum value
     */
    @Override
    public Protocol getProtocol() {
        return Protocol.REST;
    }

    @Override
    public String getSpecFilePath() {
        return "";
    }

    @Override
    public OuroApiSpec scanCurrentState() {
        return null;
    }

    @Override
    public OuroApiSpec loadFromFile(String yamlContent) {
        return null;
    }

    @Override
    public OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {
        return null;
    }

    @Override
    public String serializeToYaml(OuroApiSpec specToSave) {
        return "";
    }
}