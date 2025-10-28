package kr.co.ouroboros.core.rest.handler;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;

public class OuroRestHandler implements OuroProtocolHandler {

    @Override
    public Protocol getProtocol() {
        return Protocol.REST;
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
